package com.markvoronin.networkswitch.data.source

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.markvoronin.networkswitch.BuildConfig
import com.markvoronin.networkswitch.IShizukuController
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.service.ShizukuControllerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ShizukuNetworkControlDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkControlDataSource {

    @Volatile
    private var userService: IShizukuController? = null

    @Volatile
    private var activeConnection: ServiceConnection? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    // Serializes binding so concurrent callers share one connection instead of each binding their own.
    private val bindMutex = Mutex()

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(context, ShizukuControllerService::class.java))
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
            .tag("NetworkSwitch")
    }

    private val _shizukuPermissionStateChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val shizukuPermissionStateChanged: SharedFlow<Unit> = _shizukuPermissionStateChanged.asSharedFlow()

    override fun observePermissionStateChanges(): Flow<Unit> = shizukuPermissionStateChanged

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_ID) {
            Log.d(TAG, "Shizuku permission result: $grantResult")
            resetConnection()
            _shizukuPermissionStateChanged.tryEmit(Unit)
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received")
        resetConnection()
        _shizukuPermissionStateChanged.tryEmit(Unit)
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(TAG, "Shizuku binder dead")
        resetConnection()
        _shizukuPermissionStateChanged.tryEmit(Unit)
    }

    init {
        try {
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering Shizuku listeners", e)
        }
    }

    override fun requestPermission() {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting Shizuku permission")
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_ID)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        }
    }

    override suspend fun checkCompatibility(subId: Int): CompatibilityState {
        return try {
            if (!Shizuku.pingBinder()) {
                return CompatibilityState.Incompatible(context.getString(R.string.shizuku_not_running))
            }

            val permission = try {
                Shizuku.checkSelfPermission()
            } catch (e: Exception) {
                PackageManager.PERMISSION_DENIED
            }

            if (permission != PackageManager.PERMISSION_GRANTED) {
                requestPermission()
                return CompatibilityState.PermissionDenied(ControlMethod.SHIZUKU)
            }

            CompatibilityState.Compatible
        } catch (e: Exception) {
            CompatibilityState.Incompatible(context.getString(R.string.shizuku_unavailable, e.message))
        }
    }

    override suspend fun getCurrentNetworkMode(subId: Int): NetworkMode? {
        if (!ensureServiceBinding()) return null
        return try {
            val modeValue = userService?.getCurrentNetworkMode(subId) ?: return null
            NetworkMode.fromValue(modeValue)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setNetworkMode(subId: Int, mode: NetworkMode) {
        if (!ensureServiceBinding()) {
            throw SecurityException("Shizuku permission not granted or service binding failed")
        }
        val service = userService ?: throw IllegalStateException("Shizuku service is not available")
        if (!service.setNetworkMode(subId, mode.value)) {
            throw IllegalStateException("The device rejected network mode ${mode.displayName}")
        }
    }

    override fun isConnected(): Boolean = _isConnected.value

    override fun resetConnection() {
        val connection = activeConnection
        activeConnection = null
        userService = null
        _isConnected.value = false
        if (connection != null) {
            try {
                Shizuku.unbindUserService(userServiceArgs, connection, false)
            } catch (_: Exception) {
                // Shizuku not running; nothing to unbind
            }
        }
    }

    private fun isServiceAlive(): Boolean =
        userService != null && _isConnected.value && userService?.asBinder()?.pingBinder() == true

    /**
     * Ensures a live user-service connection exists, binding one if needed.
     * Returns false if Shizuku isn't running, permission isn't granted, or binding times out.
     */
    private suspend fun ensureServiceBinding(): Boolean {
        val hasPermission = try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
        if (!hasPermission) {
            resetConnection()
            return false
        }

        if (isServiceAlive()) return true

        return bindMutex.withLock {
            // Another caller may have connected while we waited for the lock.
            if (isServiceAlive()) return@withLock true
            resetConnection()

            val bound = withTimeoutOrNull(BIND_TIMEOUT_MS) {
                suspendCancellableCoroutine<Boolean> { continuation ->
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
                            if (binder != null && binder.pingBinder()) {
                                userService = IShizukuController.Stub.asInterface(binder)
                                _isConnected.value = true
                                if (continuation.isActive) continuation.resume(true)
                            } else if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }

                        override fun onServiceDisconnected(componentName: ComponentName?) {
                            userService = null
                            _isConnected.value = false
                        }
                    }
                    activeConnection = connection
                    continuation.invokeOnCancellation { resetConnection() }

                    try {
                        Shizuku.bindUserService(userServiceArgs, connection)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to bind Shizuku user service", e)
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            } ?: false

            if (!bound) resetConnection()
            bound
        }
    }

    private companion object {
        const val TAG = "NetworkSwitch"
        const val SHIZUKU_PERMISSION_REQUEST_ID = 8
        const val BIND_TIMEOUT_MS = 15_000L
    }
}
