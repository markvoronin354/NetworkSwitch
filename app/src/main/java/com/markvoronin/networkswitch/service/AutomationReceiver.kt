package com.markvoronin.networkswitch.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import com.markvoronin.networkswitch.domain.repository.SurfaceRefresher
import com.markvoronin.networkswitch.domain.usecase.SetNetworkModeUseCase
import com.markvoronin.networkswitch.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lets automation apps such as MacroDroid switch to a specific network mode with a broadcast:
 * action [ACTION_SET_MODE], string extra [EXTRA_MODE] holding a [NetworkMode] name (e.g. "NR_LTE").
 *
 * Exported so other apps can reach it, so every request is ignored unless the user turned on
 * "Allow automation apps" in Settings.
 */
@AndroidEntryPoint
class AutomationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var setNetworkModeUseCase: SetNetworkModeUseCase

    @Inject
    lateinit var surfaceRefresher: SurfaceRefresher

    override fun onReceive(context: Context, intent: Intent) {
        val modeName = when (intent.action) {
            ACTION_SET_MODE -> intent.getStringExtra(EXTRA_MODE)
            ACTION_FIRE_SETTING -> intent.getBundleExtra(EXTRA_BUNDLE)?.getString(EXTRA_MODE)
            else -> return
        }
        val mode = NetworkMode.entries.find { it.name == modeName }
        if (mode == null) {
            Log.w(TAG, "Automation: ignoring unknown mode '$modeName'")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!preferencesRepository.isAutomationEnabled()) {
                    Log.w(TAG, "Automation: ignoring request, automation access is off")
                    return@launch
                }
                setNetworkModeUseCase(Utils.getValidSubId(), mode)
                    .onSuccess {
                        surfaceRefresher.refreshTile()
                        surfaceRefresher.refreshWidgets()
                    }
                    .onFailure { e -> Log.e(TAG, "Automation: failed to set $mode", e) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Automation: error setting $mode", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val TAG = "NetworkSwitch"
        const val ACTION_SET_MODE = "com.markvoronin.networkswitch.ACTION_SET_MODE"
        const val EXTRA_MODE = "mode"

        // Tasker/Locale plugin contract, which is what MacroDroid's "Tasker/Locale Plugin" action uses
        const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
        const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    }
}
