package com.supernova.networkswitch.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
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
        val (widgetWidthDp, widgetHeightDp) = getWidgetDimensionsDp(options, context)

        val density = context.resources.displayMetrics.density

        val isSamsung = Build.MANUFACTURER.contains("samsung", ignoreCase = true) ||
                Build.BRAND.contains("samsung", ignoreCase = true) ||
                Build.FINGERPRINT.contains("samsung", ignoreCase = true)

        val isSamsung1xTall = isSamsung && widgetHeightDp < 145

        // Proportional scale tiers based on widget height and width
        val (badgeSizeDp, titleSp, subtitleSp) = when {
            isSamsung1xTall -> Triple(32f, 16f, 11f)
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
        val widthPx = (widgetWidthDp * density).toInt().coerceAtLeast(32)
        val heightPx = (widgetHeightDp * density).toInt().coerceAtLeast(32)

        val (cornerRadiusPx, verticalInsetPx) = calculateCornerRadiusAndInset(
            heightPx = heightPx.toFloat(),
            widgetHeightDp = widgetHeightDp,
            isSamsung = isSamsung
        )

        val bgBitmap = createRoundedBackgroundBitmap(
            widthPx = widthPx,
            heightPx = heightPx,
            color = effectiveColor,
            cornerRadiusPx = cornerRadiusPx,
            verticalInsetPx = verticalInsetPx
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
        cornerRadiusPx: Float,
        verticalInsetPx: Float = 0f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val rect = RectF(0f, verticalInsetPx, widthPx.toFloat(), heightPx.toFloat() - verticalInsetPx)
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
        return bitmap
    }

    private fun getWidgetDimensionsDp(options: Bundle?, context: Context): Pair<Int, Int> {
        if (options == null) return Pair(140, 60)

        val config = context.resources.configuration
        val isPortrait = config.orientation != Configuration.ORIENTATION_LANDSCAPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            } else {
                @Suppress("DEPRECATION")
                options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            }
            if (!sizes.isNullOrEmpty()) {
                val size = if (isPortrait) {
                    sizes.minByOrNull { it.width } ?: sizes[0]
                } else {
                    sizes.maxByOrNull { it.width } ?: sizes[0]
                }
                return Pair(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            }
        }

        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 140)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 60)

        val widthDp = if (isPortrait) minWidth else maxWidth
        val heightDp = if (isPortrait) maxHeight else minHeight

        return Pair(
            if (widthDp > 0) widthDp else 140,
            if (heightDp > 0) heightDp else 60
        )
    }

    internal fun calculateCornerRadiusAndInset(
        heightPx: Float,
        widgetHeightDp: Int,
        isSamsung: Boolean
    ): Pair<Float, Float> {
        val isSamsung1xTall = isSamsung && widgetHeightDp < 145
        return if (isSamsung1xTall) {
            val verticalInsetPx = heightPx * 0.185f
            val pillHeightPx = heightPx - (2 * verticalInsetPx)
            Pair(pillHeightPx / 2f, verticalInsetPx)
        } else {
            val cornerRadiusPx = (heightPx * 0.22f).coerceIn(20f, 120f)
            Pair(cornerRadiusPx, 0f)
        }
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
