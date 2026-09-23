package com.supernova.networkswitch.util

import android.R
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.telephony.SubscriptionManager
import com.topjohnwu.superuser.Shell

object Utils {
    fun isRootGranted(): Boolean {
        Shell.getShell()
        return Shell.isAppGrantedRoot() == true
    }

    fun getValidSubId(): Int {
        val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return dataSubId
        }
        val defaultSubId = SubscriptionManager.getDefaultSubscriptionId()
        if (defaultSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return defaultSubId
        }
        return 1
    }
}

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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                context.getColor(R.color.system_neutral1_800)
            } catch (_: Exception) {
                0xFF212121.toInt()
            }
        } else {
            0xFF212121.toInt()
        }
    }

    fun getSystemLightColor(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                context.getColor(R.color.system_neutral1_100)
            } catch (_: Exception) {
                0xFFE0E0E0.toInt()
            }
        } else {
            0xFFE0E0E0.toInt()
        }
    }
}
