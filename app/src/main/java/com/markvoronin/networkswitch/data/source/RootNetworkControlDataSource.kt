package com.markvoronin.networkswitch.data.source

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.markvoronin.networkswitch.IRootController
import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.service.RootNetworkControllerService
import com.markvoronin.networkswitch.util.Utils
import com.topjohnwu.superuser.ipc.RootService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class RootNetworkControlDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkControlDataSource {

    @Volatile
    private var networkController: IRootController? = null

    @Volatile
    private var activeConnection: ServiceConnection? = null

    // Serializes binding so concurrent callers share one connection instead of each binding their own.
    private val bindMutex = Mutex()

    override suspend fun checkCompatibility(subId: Int): CompatibilityState {
        return if (!Utils.isRootGranted()) {
            CompatibilityState.PermissionDenied(ControlMethod.ROOT)
        } else {
            CompatibilityState.Compatible
        }
    }

    override suspend fun getCurrentNetworkMode(subId: Int): NetworkMode? {
        return try {
            val modeValue = getNetworkController()?.getCurrentNetworkMode(subId) ?: return null
            NetworkMode.fromValue(modeValue)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setNetworkMode(subId: Int, mode: NetworkMode) {
        val controller = getNetworkController()
            ?: throw IllegalStateException("Root service is not available")
        if (!controller.setNetworkMode(subId, mode.value)) {
            throw IllegalStateException("The device rejected network mode ${mode.displayName}")
        }
    }

    override suspend fun reconnectNetwork() {
        val controller = getNetworkController()
            ?: throw IllegalStateException("Root service is not available")
        if (!controller.reconnectNetwork()) {
            throw IllegalStateException("The device could not reconnect the network")
        }
    }

    override fun isConnected(): Boolean = networkController?.asBinder()?.isBinderAlive == true

    override fun resetConnection() {
        val connection = activeConnection
        activeConnection = null
        networkController = null
        if (connection != null) {
            try {
                RootService.unbind(connection)
            } catch (_: Exception) {
                // Already unbound
            }
        }
    }

    private suspend fun getNetworkController(): IRootController? {
        connectedController()?.let { return it }

        return bindMutex.withLock {
            // Another caller may have connected while we waited for the lock.
            connectedController()?.let { return@withLock it }
            resetConnection()

            withTimeoutOrNull(BIND_TIMEOUT_MS) {
                suspendCancellableCoroutine<IRootController?> { continuation ->
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            val controller = IRootController.Stub.asInterface(service)
                            networkController = controller
                            if (continuation.isActive) continuation.resume(controller)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            networkController = null
                        }
                    }
                    activeConnection = connection
                    continuation.invokeOnCancellation { resetConnection() }

                    try {
                        RootService.bind(Intent(context, RootNetworkControllerService::class.java), connection)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }.also { if (it == null) resetConnection() }
        }
    }

    private fun connectedController(): IRootController? =
        networkController?.takeIf { it.asBinder().isBinderAlive }

    private companion object {
        const val BIND_TIMEOUT_MS = 15_000L
    }
}
