package com.supernova.networkswitch.data.source

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.model.WidgetCustomizationConfig
import com.supernova.networkswitch.service.NetworkWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val CONTROL_METHOD_KEY = stringPreferencesKey("control_method")
        private val TOGGLE_MODE_A_KEY = intPreferencesKey("toggle_mode_a")
        private val TOGGLE_MODE_B_KEY = intPreferencesKey("toggle_mode_b")
        private val TOGGLE_NEXT_IS_B_KEY = booleanPreferencesKey("toggle_next_is_b")
        private val WIDGET_USE_SYSTEM_COLOR_KEY = booleanPreferencesKey("widget_use_system_color")
        private val WIDGET_CUSTOM_COLOR_KEY = intPreferencesKey("widget_custom_color")
        private val WIDGET_OPACITY_KEY = floatPreferencesKey("widget_opacity")
        
        private const val DEFAULT_CONTROL_METHOD = "SHIZUKU"
        
        private val DEFAULT_MODE_A = NetworkMode.LTE_ONLY
        private val DEFAULT_MODE_B = NetworkMode.NR_LTE
        private const val DEFAULT_NEXT_IS_B = true
        
        private const val DEFAULT_USE_SYSTEM_COLOR = false
        private const val DEFAULT_CUSTOM_COLOR = 0xFF333333.toInt()
        private const val DEFAULT_OPACITY = 0.70f
    }
    
    private fun parseControlMethod(methodString: String?): ControlMethod {
        return try {
            ControlMethod.valueOf(methodString ?: DEFAULT_CONTROL_METHOD)
        } catch (e: IllegalArgumentException) {
            ControlMethod.SHIZUKU
        }
    }
    
    suspend fun getControlMethod(): ControlMethod {
        return dataStore.data.map { preferences ->
            parseControlMethod(preferences[CONTROL_METHOD_KEY])
        }.first()
    }
    
    suspend fun setControlMethod(method: ControlMethod) {
        dataStore.edit { preferences ->
            preferences[CONTROL_METHOD_KEY] = method.name
        }
    }
    
    fun observeControlMethod(): Flow<ControlMethod> {
        return dataStore.data.map { preferences ->
            parseControlMethod(preferences[CONTROL_METHOD_KEY])
        }
    }
    
    suspend fun getToggleModeConfig(): ToggleModeConfig {
        return dataStore.data.map { preferences ->
            val modeAValue = preferences[TOGGLE_MODE_A_KEY] ?: DEFAULT_MODE_A.value
            val modeBValue = preferences[TOGGLE_MODE_B_KEY] ?: DEFAULT_MODE_B.value
            val nextIsB = preferences[TOGGLE_NEXT_IS_B_KEY] ?: DEFAULT_NEXT_IS_B
            
            val modeA = NetworkMode.fromValue(modeAValue) ?: DEFAULT_MODE_A
            val modeB = NetworkMode.fromValue(modeBValue) ?: DEFAULT_MODE_B
            
            ToggleModeConfig(modeA, modeB, nextIsB)
        }.first()
    }
    
    suspend fun setToggleModeConfig(config: ToggleModeConfig) {
        dataStore.edit { preferences ->
            preferences[TOGGLE_MODE_A_KEY] = config.modeA.value
            preferences[TOGGLE_MODE_B_KEY] = config.modeB.value
            preferences[TOGGLE_NEXT_IS_B_KEY] = config.nextModeIsB
        }
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, "com.supernova.networkswitch.service.NetworkTileService")
            )
        } catch (_: Exception) {
            // Tile request optional
        }
        try {
            NetworkWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {
            // Widget update optional
        }
    }
    
    fun observeToggleModeConfig(): Flow<ToggleModeConfig> {
        return dataStore.data.map { preferences ->
            val modeAValue = preferences[TOGGLE_MODE_A_KEY] ?: DEFAULT_MODE_A.value
            val modeBValue = preferences[TOGGLE_MODE_B_KEY] ?: DEFAULT_MODE_B.value
            val nextIsB = preferences[TOGGLE_NEXT_IS_B_KEY] ?: DEFAULT_NEXT_IS_B
            
            val modeA = NetworkMode.fromValue(modeAValue) ?: DEFAULT_MODE_A
            val modeB = NetworkMode.fromValue(modeBValue) ?: DEFAULT_MODE_B
            
            ToggleModeConfig(modeA, modeB, nextIsB)
        }
    }

    suspend fun getWidgetCustomizationConfig(): WidgetCustomizationConfig {
        return dataStore.data.map { preferences ->
            val useSystemColor = preferences[WIDGET_USE_SYSTEM_COLOR_KEY] ?: DEFAULT_USE_SYSTEM_COLOR
            val customColorHex = preferences[WIDGET_CUSTOM_COLOR_KEY] ?: DEFAULT_CUSTOM_COLOR
            val opacity = preferences[WIDGET_OPACITY_KEY] ?: DEFAULT_OPACITY

            WidgetCustomizationConfig(useSystemColor, customColorHex, opacity)
        }.first()
    }

    suspend fun setWidgetCustomizationConfig(config: WidgetCustomizationConfig) {
        dataStore.edit { preferences ->
            preferences[WIDGET_USE_SYSTEM_COLOR_KEY] = config.useSystemColor
            preferences[WIDGET_CUSTOM_COLOR_KEY] = config.customColorHex
            preferences[WIDGET_OPACITY_KEY] = config.opacity
        }
        try {
            NetworkWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {
            // Widget update optional
        }
    }

    fun observeWidgetCustomizationConfig(): Flow<WidgetCustomizationConfig> {
        return dataStore.data.map { preferences ->
            val useSystemColor = preferences[WIDGET_USE_SYSTEM_COLOR_KEY] ?: DEFAULT_USE_SYSTEM_COLOR
            val customColorHex = preferences[WIDGET_CUSTOM_COLOR_KEY] ?: DEFAULT_CUSTOM_COLOR
            val opacity = preferences[WIDGET_OPACITY_KEY] ?: DEFAULT_OPACITY

            WidgetCustomizationConfig(useSystemColor, customColorHex, opacity)
        }
    }
}
