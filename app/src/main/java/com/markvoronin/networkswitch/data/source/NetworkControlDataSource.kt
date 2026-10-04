package com.markvoronin.networkswitch.data.source

import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.NetworkMode

interface NetworkControlDataSource {
    suspend fun checkCompatibility(subId: Int): CompatibilityState
    suspend fun getCurrentNetworkMode(subId: Int): NetworkMode?
    suspend fun setNetworkMode(subId: Int, mode: NetworkMode)
    fun isConnected(): Boolean
    fun resetConnection()
    fun requestPermission() {}
    fun observePermissionStateChanges(): kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.emptyFlow()
}
