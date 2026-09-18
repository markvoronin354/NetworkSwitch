package com.supernova.networkswitch.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.WidgetCustomizationConfig
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NetworkWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase

    @Inject
    lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase

    @Inject
    lateinit var getToggleModeConfigUseCase: GetToggleModeConfigUseCase

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidget(context, appWidgetManager, appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_NETWORK) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    showSwitchingState(context)

                    val subId = Utils.getValidSubId()
                    toggleNetworkModeUseCase(subId)

                    updateAllWidgets(context)
                } catch (e: Exception) {
                    Log.e("NetworkWidgetProvider", "Error in widget toggle", e)
                    updateAllWidgets(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showSwitchingState(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, NetworkWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_network_tile)
            views.setTextViewText(R.id.widget_tile_subtitle, "Switching...")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_network_tile)
        val subId = Utils.getValidSubId()

        // Get actual widget dimensions from options
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 140
        val minHeightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 60

        val widgetWidthDp = if (minWidthDp > 0) minWidthDp else 140
        val widgetHeightDp = if (minHeightDp > 0) minHeightDp else 60

        val density = context.resources.displayMetrics.density

        // Proportional scale tiers based on widget height and width
        val (badgeSizeDp, titleSp, subtitleSp) = when {
            widgetHeightDp >= 180 || widgetWidthDp >= 320 -> Triple(64f, 26f, 16f)
            widgetHeightDp >= 130 || widgetWidthDp >= 240 -> Triple(54f, 22f, 14f)
            widgetHeightDp >= 90  || widgetWidthDp >= 180 -> Triple(46f, 19f, 13f)
            else                                         -> Triple(38f, 16f, 12f)
        }

        val badgeSizePx = (badgeSizeDp * density).toInt().coerceAtLeast(32)

        // Apply scaled text sizes
        views.setTextViewTextSize(R.id.widget_tile_title, TypedValue.COMPLEX_UNIT_SP, titleSp)
        views.setTextViewTextSize(R.id.widget_tile_subtitle, TypedValue.COMPLEX_UNIT_SP, subtitleSp)

        val customization = try {
            preferencesRepository.getWidgetCustomizationConfig()
        } catch (_: Exception) {
            WidgetCustomizationConfig()
        }

        val systemAccentColor = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getColor(android.R.color.system_accent1_600)
            } else {
                context.getColor(R.color.purple_700)
            }
        } catch (_: Exception) {
            context.getColor(R.color.purple_700)
        }

        val effectiveColor = customization.getEffectiveColor(systemAccentColor)

        // Render rounded background bitmap scaled to actual widget proportions
        val widthPx = (widgetWidthDp * density).toInt().coerceAtLeast(200)
        val heightPx = (widgetHeightDp * density).toInt().coerceAtLeast(100)
        val cornerRadiusPx = (heightPx * 0.22f).coerceIn(20f, 120f)

        val bgBitmap = createRoundedBackgroundBitmap(
            widthPx = widthPx,
            heightPx = heightPx,
            color = effectiveColor,
            cornerRadiusPx = cornerRadiusPx
        )
        views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)

        // Adjust text contrast based on background color luminance
        val isDark = isColorDark(effectiveColor)
        val titleColor = if (isDark) Color.WHITE else Color.BLACK
        val subtitleColor = if (isDark) Color.parseColor("#E0E0E0") else Color.parseColor("#424242")

        views.setTextColor(R.id.widget_tile_title, titleColor)
        views.setTextColor(R.id.widget_tile_subtitle, subtitleColor)

        val toggleConfig = try {
            getToggleModeConfigUseCase()
        } catch (_: Exception) {
            null
        }

        val currentMode = try {
            getCurrentNetworkModeUseCase(subId).getOrNull()
        } catch (_: Exception) {
            null
        }

        val modeLabel: String
        val nextModeLabel: String

        if (toggleConfig != null) {
            val mode = currentMode ?: toggleConfig.getCurrentMode()
            modeLabel = mode.tileLabel
            nextModeLabel = toggleConfig.getNextMode().tileLabel

            views.setTextViewText(R.id.widget_tile_title, modeLabel)
            views.setTextViewText(R.id.widget_tile_subtitle, "Next: $nextModeLabel")
        } else {
            modeLabel = "N/A"
            views.setTextViewText(R.id.widget_tile_title, "Network Switch")
            views.setTextViewText(R.id.widget_tile_subtitle, "Tap to load")
        }

        // Render scaled badge icon bitmap with Network Switch logo
        val badgeColor = if (isDark) Color.parseColor("#4A4A4A") else Color.parseColor("#616161")
        val badgeBitmap = createBadgeIconBitmap(
            context = context,
            badgeSizePx = badgeSizePx,
            badgeColor = badgeColor
        )
        views.setImageViewBitmap(R.id.widget_tile_icon, badgeBitmap)

        val toggleIntent = Intent(context, NetworkWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_NETWORK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createBadgeIconBitmap(
        context: Context,
        badgeSizePx: Int,
        badgeColor: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(badgeSizePx, badgeSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeColor
            style = Paint.Style.FILL
        }
        val radius = badgeSizePx / 2f
        canvas.drawCircle(radius, radius, radius, circlePaint)

        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher_foreground)
            ?: ContextCompat.getDrawable(context, R.mipmap.ic_launcher)

        drawable?.let {
            val inset = (badgeSizePx * 0.02f).toInt()
            it.setBounds(inset, inset, badgeSizePx - inset, badgeSizePx - inset)
            it.draw(canvas)
        }

        return bitmap
    }

    private fun createRoundedBackgroundBitmap(
        widthPx: Int,
        heightPx: Int,
        color: Int,
        cornerRadiusPx: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
        return bitmap
    }

    private fun isColorDark(color: Int): Boolean {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        return luminance < 0.6
    }

    companion object {
        const val ACTION_TOGGLE_NETWORK = "com.supernova.networkswitch.ACTION_WIDGET_TOGGLE"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, NetworkWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(componentName) ?: return
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, NetworkWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (_: Exception) {
                // Ignore errors updating widget
            }
        }
    }
}
