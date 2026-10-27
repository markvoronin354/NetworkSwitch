package com.markvoronin.networkswitch.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.model.WidgetCustomizationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val CONTROL_METHOD_KEY = stringPreferencesKey("control_method")
        private val TOGGLE_MODE_A_KEY = intPreferencesKey("toggle_mode_a")
        private val TOGGLE_MODE_B_KEY = intPreferencesKey("toggle_mode_b")
        private val TOGGLE_NEXT_IS_B_KEY = booleanPreferencesKey("toggle_next_is_b")
        private val WIDGET_USE_SYSTEM_COLOR_KEY = booleanPreferencesKey("widget_use_system_color")
        private val WIDGET_CUSTOM_COLOR_KEY = intPreferencesKey("widget_custom_color")
        private val WIDGET_OPACITY_KEY = floatPreferencesKey("widget_opacity")

        private val DEFAULT_CONTROL_METHOD = ControlMethod.SHIZUKU

        private val DEFAULT_MODE_A = NetworkMode.LTE_ONLY
        private val DEFAULT_MODE_B = NetworkMode.NR_LTE
        private const val DEFAULT_NEXT_IS_B = true

        private const val DEFAULT_USE_SYSTEM_COLOR = true
        private const val DEFAULT_CUSTOM_COLOR = 0xFF333333.toInt()
        private const val DEFAULT_OPACITY = 0.50f
    }

    private fun Preferences.toControlMethod(): ControlMethod {
        val name = this[CONTROL_METHOD_KEY] ?: return DEFAULT_CONTROL_METHOD
        return ControlMethod.entries.find { it.name == name } ?: DEFAULT_CONTROL_METHOD
    }

    private fun Preferences.toToggleModeConfig(): ToggleModeConfig {
        val modeA = NetworkMode.fromValue(this[TOGGLE_MODE_A_KEY] ?: DEFAULT_MODE_A.value) ?: DEFAULT_MODE_A
        val modeB = NetworkMode.fromValue(this[TOGGLE_MODE_B_KEY] ?: DEFAULT_MODE_B.value) ?: DEFAULT_MODE_B
        return ToggleModeConfig(modeA, modeB, this[TOGGLE_NEXT_IS_B_KEY] ?: DEFAULT_NEXT_IS_B)
    }

    private fun Preferences.toWidgetCustomizationConfig() = WidgetCustomizationConfig(
        useSystemColor = this[WIDGET_USE_SYSTEM_COLOR_KEY] ?: DEFAULT_USE_SYSTEM_COLOR,
        customColorHex = this[WIDGET_CUSTOM_COLOR_KEY] ?: DEFAULT_CUSTOM_COLOR,
        opacity = this[WIDGET_OPACITY_KEY] ?: DEFAULT_OPACITY
    )

    suspend fun getControlMethod(): ControlMethod = observeControlMethod().first()

    suspend fun setControlMethod(method: ControlMethod) {
        dataStore.edit { preferences ->
            preferences[CONTROL_METHOD_KEY] = method.name
        }
    }

    fun observeControlMethod(): Flow<ControlMethod> =
        dataStore.data.map { it.toControlMethod() }

    suspend fun getToggleModeConfig(): ToggleModeConfig = observeToggleModeConfig().first()

    suspend fun setToggleModeConfig(config: ToggleModeConfig) {
        dataStore.edit { preferences ->
            preferences[TOGGLE_MODE_A_KEY] = config.modeA.value
            preferences[TOGGLE_MODE_B_KEY] = config.modeB.value
            preferences[TOGGLE_NEXT_IS_B_KEY] = config.nextModeIsB
        }
    }

    fun observeToggleModeConfig(): Flow<ToggleModeConfig> =
        dataStore.data.map { it.toToggleModeConfig() }

    suspend fun getWidgetCustomizationConfig(): WidgetCustomizationConfig =
        observeWidgetCustomizationConfig().first()

    suspend fun setWidgetCustomizationConfig(config: WidgetCustomizationConfig) {
        dataStore.edit { preferences ->
            preferences[WIDGET_USE_SYSTEM_COLOR_KEY] = config.useSystemColor
            preferences[WIDGET_CUSTOM_COLOR_KEY] = config.customColorHex
            preferences[WIDGET_OPACITY_KEY] = config.opacity
        }
    }

    fun observeWidgetCustomizationConfig(): Flow<WidgetCustomizationConfig> =
        dataStore.data.map { it.toWidgetCustomizationConfig() }
}
