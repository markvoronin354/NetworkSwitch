package com.markvoronin.networkswitch.data.repository

import com.markvoronin.networkswitch.data.source.NetworkControlDataSource
import com.markvoronin.networkswitch.data.source.RootNetworkControlDataSource
import com.markvoronin.networkswitch.data.source.ShizukuNetworkControlDataSource
import com.markvoronin.networkswitch.di.IoDispatcher
import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.repository.NetworkControlRepository
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import com.markvoronin.networkswitch.util.Utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NetworkControlRepository that delegates to the data source for the
 * selected control method. Root and Shizuku calls block on IPC, so they run on [ioDispatcher].
 */
@Singleton
class NetworkControlRepositoryImpl @Inject constructor(
    private val rootDataSource: RootNetworkControlDataSource,
    private val shizukuDataSource: ShizukuNetworkControlDataSource,
    private val preferencesRepository: PreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NetworkControlRepository {

    override suspend fun checkCompatibility(method: ControlMethod): CompatibilityState =
        withContext(ioDispatcher) {
            getDataSource(method).checkCompatibility(Utils.getValidSubId())
        }

    override suspend fun getCurrentNetworkMode(subId: Int): NetworkMode? = withContext(ioDispatcher) {
        try {
            val method = preferencesRepository.getControlMethod()
            val detected = getDataSource(method).getCurrentNetworkMode(subId) ?: return@withContext null
            preferConfiguredMode(detected)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setNetworkMode(subId: Int, mode: NetworkMode): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val method = preferencesRepository.getControlMethod()
                getDataSource(method).setNetworkMode(subId, mode)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Reset connections for both data sources - useful when switching control methods
     */
    override suspend fun resetConnections() = withContext(ioDispatcher) {
        rootDataSource.resetConnection()
        shizukuDataSource.resetConnection()
    }

    override suspend fun requestPermission(method: ControlMethod) = withContext(ioDispatcher) {
        getDataSource(method).requestPermission()
    }

    override fun observePermissionStateChanges(): Flow<Unit> = shizukuDataSource.shizukuPermissionStateChanged

    /**
     * Some modes are identical at the radio level (e.g. "2G/3G (3G Preferred)" and "2G/3G (Auto)"),
     * so the radio can't tell them apart. If the user's toggle pair contains an equivalent mode,
     * report that one so the UI matches what the user selected.
     */
    private suspend fun preferConfiguredMode(detected: NetworkMode): NetworkMode {
        val config = preferencesRepository.getToggleModeConfig()
        return listOf(config.getCurrentMode(), config.getNextMode())
            .firstOrNull { it.technologies == detected.technologies }
            ?: detected
    }

    private fun getDataSource(method: ControlMethod): NetworkControlDataSource {
        return when (method) {
            ControlMethod.ROOT -> rootDataSource
            ControlMethod.SHIZUKU -> shizukuDataSource
        }
    }
}
