package com.markvoronin.networkswitch.domain.repository

import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
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
}

/**
 * Refreshes the Quick Settings tile and home-screen widgets after state they display has changed
 */
interface SurfaceRefresher {
    fun refreshTile()
    fun refreshWidgets()
}
