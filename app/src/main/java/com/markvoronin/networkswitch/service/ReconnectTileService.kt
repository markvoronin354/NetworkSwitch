package com.markvoronin.networkswitch.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.domain.repository.NetworkControlRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * One-tap tile that runs the same reconnect as the button on the main screen.
 */
@AndroidEntryPoint
class ReconnectTileService : TileService() {

    @Inject
    lateinit var networkControlRepository: NetworkControlRepository

    private companion object {
        const val TAG = "NetworkSwitch"

        // Shared across service instances so a reconnect is never started twice
        val isReconnecting = AtomicBoolean(false)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { performReconnect() }
        } else {
            performReconnect()
        }
    }

    private fun performReconnect() {
        if (!isReconnecting.compareAndSet(false, true)) return

        Toast.makeText(this, R.string.toast_reconnecting, Toast.LENGTH_SHORT).show()
        updateTile()

        // Process-wide scope so the reconnect completes even if the service is destroyed meanwhile
        CoroutineScope(ProcessLifecycleOwner.get().lifecycleScope.coroutineContext + Dispatchers.IO).launch {
            try {
                networkControlRepository.reconnectNetwork()
                    .onFailure { e ->
                        Log.e(TAG, "Reconnect tile: failed", e)
                        reportFailure()
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect tile: error", e)
                reportFailure()
            } finally {
                isReconnecting.set(false)
                withContext(Dispatchers.Main) { updateTile() }
            }
        }
    }

    private suspend fun reportFailure() {
        withContext(Dispatchers.Main) {
            Toast.makeText(applicationContext, R.string.error_reconnect_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val busy = isReconnecting.get()
        tile.state = if (busy) Tile.STATE_UNAVAILABLE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_reconnect_label)
        tile.subtitle = getString(if (busy) R.string.tile_reconnecting else R.string.tile_reconnect_subtitle)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_reconnect)
        tile.updateTile()
    }
}
