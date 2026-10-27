package com.markvoronin.networkswitch.service

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.markvoronin.networkswitch.domain.repository.SurfaceRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asks the system to redraw the Quick Settings tile and home-screen widgets.
 */
@Singleton
class AndroidSurfaceRefresher @Inject constructor(
    @ApplicationContext private val context: Context
) : SurfaceRefresher {

    override fun refreshTile() {
        try {
            TileService.requestListeningState(context, ComponentName(context, NetworkTileService::class.java))
        } catch (_: Exception) {
            // Tile may not be added to Quick Settings
        }
    }

    override fun refreshWidgets() {
        NetworkWidgetProvider.updateAllWidgets(context)
    }
}
