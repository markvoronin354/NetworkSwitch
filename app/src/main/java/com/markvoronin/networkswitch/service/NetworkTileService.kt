package com.markvoronin.networkswitch.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import com.markvoronin.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.markvoronin.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.markvoronin.networkswitch.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class NetworkTileService : TileService() {
    
    @Inject
    lateinit var getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase
    
    @Inject
    lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listeningJob: Job? = null
    
    private var currentNetworkMode: NetworkMode? = null
    private var toggleConfig: ToggleModeConfig? = null

    private companion object {
        const val TAG = "NetworkSwitch"
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            try {
                // Observe toggle configuration changes
                preferencesRepository.observeToggleModeConfig().collect { newConfig ->
                    toggleConfig = newConfig
                    refreshNetworkState()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tile: Failed to observe toggle config", e)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
        listeningJob = null
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "Tile: onClick triggered")
        Toast.makeText(this, R.string.toast_switching, Toast.LENGTH_SHORT).show()

        // Provide immediate visual feedback
        qsTile?.let {
            it.state = Tile.STATE_UNAVAILABLE
            it.subtitle = getString(R.string.tile_processing)
            it.updateTile()
        }

        if (isLocked) {
            Log.d(TAG, "Tile: Device is locked, unlocking and running...")
            unlockAndRun {
                performToggle()
            }
        } else {
            performToggle()
        }
    }

    private fun performToggle() {
        val subId = Utils.getValidSubId()
        Log.d(TAG, "Tile: performToggle for subId: $subId")

        // Process-wide scope so the switch completes even if the service is destroyed meanwhile
        CoroutineScope(ProcessLifecycleOwner.get().lifecycleScope.coroutineContext + Dispatchers.IO).launch {
            try {
                toggleNetworkModeUseCase(subId)
                    .onSuccess { newMode ->
                        Log.d(TAG, "Tile: Toggle success, new mode: ${newMode.displayName}")
                        currentNetworkMode = newMode
                        withContext(Dispatchers.Main) {
                            updateTileState()
                        }
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Tile: Toggle failed", e)
                        reportToggleFailure()
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tile: Error in performToggle", e)
                reportToggleFailure()
            }
        }
    }

    private suspend fun reportToggleFailure() {
        withContext(Dispatchers.Main) {
            Toast.makeText(applicationContext, R.string.error_switch_failed, Toast.LENGTH_LONG).show()
        }
        refreshNetworkState()
    }

    private suspend fun refreshNetworkState() {
        val subId = Utils.getValidSubId()
        currentNetworkMode = getCurrentNetworkModeUseCase(subId).getOrNull()
        withContext(Dispatchers.Main) {
            updateTileState()
        }
    }

    private fun createTextIcon(text: String): Icon {
        val size = 128
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = if (text.length <= 2) 72f else 48f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val x = size / 2f
        val y = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(text, x, y, paint)

        return Icon.createWithBitmap(bitmap)
    }

    private fun updateTileState() {
        try {
            val tile = qsTile ?: return
            val config = toggleConfig

            if (config != null) {
                tile.state = Tile.STATE_ACTIVE

                // Show current mode short label and next mode as subtitle
                val currentMode = currentNetworkMode ?: config.getCurrentMode()
                val modeLabel = currentMode.tileLabel

                tile.label = modeLabel
                tile.subtitle = getString(R.string.next_mode, config.getNextMode().tileLabel)
                tile.icon = createTextIcon(modeLabel)
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.network_switch)
                tile.subtitle = getString(R.string.tile_tap_to_load)
                tile.icon = createTextIcon(getString(R.string.tile_unavailable_icon))
            }
            tile.updateTile()
        } catch (e: Exception) {
            Log.e(TAG, "Tile: Failed to update tile state", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
