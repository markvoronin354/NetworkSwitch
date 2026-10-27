package com.markvoronin.networkswitch.service

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.markvoronin.networkswitch.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles taps on the home-screen widget. Kept separate from [NetworkWidgetProvider]
 * so it can be non-exported: only the widget's own PendingIntent can reach it,
 * unlike the provider, which the manifest has to export for the launcher.
 */
@AndroidEntryPoint
class WidgetToggleReceiver : BroadcastReceiver() {

    @Inject
    lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_NETWORK) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                showSwitchingState(appContext)
                toggleNetworkModeUseCase(Utils.getValidSubId())
                    .onFailure { e ->
                        Log.e(TAG, "Error in widget toggle", e)
                        showFailure(appContext)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error in widget toggle", e)
                showFailure(appContext)
            } finally {
                NetworkWidgetProvider.updateAllWidgets(appContext)
                pendingResult.finish()
            }
        }
    }

    /** Updates only the subtitle; a full update would replace the whole widget with a bare layout. */
    private fun showSwitchingState(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NetworkWidgetProvider::class.java))

        val views = RemoteViews(context.packageName, R.layout.widget_network_tile)
        views.setTextViewText(R.id.widget_tile_subtitle, context.getString(R.string.widget_switching))
        appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
    }

    private fun showFailure(context: Context) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, R.string.error_switch_failed, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val TAG = "NetworkSwitch"
        const val ACTION_TOGGLE_NETWORK = "com.markvoronin.networkswitch.ACTION_WIDGET_TOGGLE"
    }
}
