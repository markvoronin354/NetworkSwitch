package com.markvoronin.networkswitch.domain.repository

import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.NetworkStats
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.model.WidgetCustomizationConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for network control operations
 */
interface NetworkControlRepository {
    /**
     * Check if network control is compatible with current device/method
     */
    suspend fun checkCompatibility(method: ControlMethod): CompatibilityState
    
    /**
     * Get the current network mode, or null if it can't be determined.
     * When several modes are indistinguishable at the radio level, the
     * configured toggle modes are preferred.
     */
    suspend fun getCurrentNetworkMode(subId: Int): NetworkMode?
    
    /**
     * Set network mode. Fails if the change could not be applied.
     */
    suspend fun setNetworkMode(subId: Int, mode: NetworkMode): Result<Unit>
    
    /**
     * Reset connections - useful when switching control methods
     */
    suspend fun resetConnections()

    /**
     * Request permission for the given control method (e.g. Shizuku)
     */
    suspend fun requestPermission(method: ControlMethod)

    /**
     * Observe permission or binder state changes
     */
    fun observePermissionStateChanges(): Flow<Unit>
}

/**
 * Repository interface for app preferences
 */
interface PreferencesRepository {
    /**
     * Get preferred control method
     */
    suspend fun getControlMethod(): ControlMethod
    
    /**
     * Set preferred control method
     */
    suspend fun setControlMethod(method: ControlMethod)
    
    /**
     * Observe control method changes
     */
    fun observeControlMethod(): Flow<ControlMethod>
    
    /**
     * Get toggle mode configuration
     */
    suspend fun getToggleModeConfig(): ToggleModeConfig
    
    /**
     * Set toggle mode configuration
     */
    suspend fun setToggleModeConfig(config: ToggleModeConfig)
    
    /**
     * Observe toggle mode configuration changes
     */
    fun observeToggleModeConfig(): Flow<ToggleModeConfig>
    
    /**
     * Get widget customization configuration
     */
    suspend fun getWidgetCustomizationConfig(): WidgetCustomizationConfig

    /**
     * Set widget customization configuration
     */
    suspend fun setWidgetCustomizationConfig(config: WidgetCustomizationConfig)

    /**
     * Observe widget customization configuration changes
     */
    fun observeWidgetCustomizationConfig(): Flow<WidgetCustomizationConfig>

    /**
     * Observe the last speed test result (bits per second) for each network mode that has one
     */
    fun observeSpeedTestResults(): Flow<Map<NetworkMode, Long>>

    /**
     * Save the latest speed test result (bits per second) for a network mode
     */
    suspend fun setSpeedTestResult(mode: NetworkMode, bitsPerSecond: Long)
}

/**
 * Repository interface for live network statistics
 */
interface NetworkStatsRepository {
    /**
     * Observe network stats, updated about once per second. The underlying
     * polling and signal listener only run while the flow is being collected.
     */
    fun observeNetworkStats(): Flow<NetworkStats>
}

/**
 * Repository interface for the cellular speed test
 */
interface SpeedTestRepository {
    /**
     * Download a test file over the cellular network and return the average speed in bits per
     * second. Fails if there is no cellular connection or the download breaks off.
     */
    suspend fun runDownloadTest(): Result<Long>
}

/**
 * Refreshes the Quick Settings tile and home-screen widgets after state they display has changed
 */
interface SurfaceRefresher {
    fun refreshTile()
    fun refreshWidgets()
}
