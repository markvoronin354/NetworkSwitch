package com.markvoronin.networkswitch.data.source

import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.NetworkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface NetworkControlDataSource {
    suspend fun checkCompatibility(subId: Int): CompatibilityState

    /** Returns the active mode, or null if it can't be read or isn't a known [NetworkMode]. */
    suspend fun getCurrentNetworkMode(subId: Int): NetworkMode?

    /** Applies [mode]; throws if the change could not be applied. */
    suspend fun setNetworkMode(subId: Int, mode: NetworkMode)

    /** Briefly drops and restores Wi-Fi and mobile data and restarts the modem; throws if that failed. */
    suspend fun reconnectNetwork()

    fun isConnected(): Boolean
    fun resetConnection()
    fun requestPermission() {}
    fun observePermissionStateChanges(): Flow<Unit> = emptyFlow()
}
