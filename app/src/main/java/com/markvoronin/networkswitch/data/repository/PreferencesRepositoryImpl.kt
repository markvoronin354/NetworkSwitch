package com.markvoronin.networkswitch.data.repository

import com.markvoronin.networkswitch.data.source.PreferencesDataSource
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.model.WidgetCustomizationConfig
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import com.markvoronin.networkswitch.domain.repository.SurfaceRefresher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PreferencesRepository. Saving a setting that the tile or widgets
 * display also asks [surfaceRefresher] to redraw them.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val surfaceRefresher: SurfaceRefresher
) : PreferencesRepository {

    override suspend fun getControlMethod(): ControlMethod {
        return preferencesDataSource.getControlMethod()
    }

    override suspend fun setControlMethod(method: ControlMethod) {
        preferencesDataSource.setControlMethod(method)
    }

    override fun observeControlMethod(): Flow<ControlMethod> {
        return preferencesDataSource.observeControlMethod()
    }

    override suspend fun getToggleModeConfig(): ToggleModeConfig {
        return preferencesDataSource.getToggleModeConfig()
    }

    override suspend fun setToggleModeConfig(config: ToggleModeConfig) {
        preferencesDataSource.setToggleModeConfig(config)
        surfaceRefresher.refreshTile()
        surfaceRefresher.refreshWidgets()
    }

    override fun observeToggleModeConfig(): Flow<ToggleModeConfig> {
        return preferencesDataSource.observeToggleModeConfig()
    }

    override suspend fun getWidgetCustomizationConfig(): WidgetCustomizationConfig {
        return preferencesDataSource.getWidgetCustomizationConfig()
    }

    override suspend fun setWidgetCustomizationConfig(config: WidgetCustomizationConfig) {
        preferencesDataSource.setWidgetCustomizationConfig(config)
        surfaceRefresher.refreshWidgets()
    }

    override fun observeWidgetCustomizationConfig(): Flow<WidgetCustomizationConfig> {
        return preferencesDataSource.observeWidgetCustomizationConfig()
    }
}
