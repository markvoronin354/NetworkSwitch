package com.markvoronin.networkswitch.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build

object WidgetThemeHelper {
    fun getSystemWidgetColor(context: Context): Int {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDark = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        return if (isDark) {
            getSystemDarkColor(context)
        } else {
            getSystemLightColor(context)
        }
    }

    fun getSystemDarkColor(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return FALLBACK_DARK
        return try {
            context.getColor(android.R.color.system_neutral1_800)
        } catch (_: Exception) {
            FALLBACK_DARK
        }
    }

    fun getSystemLightColor(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return FALLBACK_LIGHT
        return try {
            context.getColor(android.R.color.system_neutral1_100)
        } catch (_: Exception) {
            FALLBACK_LIGHT
        }
    }

    private const val FALLBACK_DARK = 0xFF212121.toInt()
    private const val FALLBACK_LIGHT = 0xFFE0E0E0.toInt()
}
