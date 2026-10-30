package com.markvoronin.networkswitch.util

import android.telephony.SubscriptionManager
import com.topjohnwu.superuser.Shell

object Utils {
    /**
     * Whether this app has root. Blocks until the superuser prompt is answered,
     * so it must not be called on the main thread.
     */
    fun isRootGranted(): Boolean {
        Shell.getShell()
        return Shell.isAppGrantedRoot() == true
    }

    /** Formats a bit rate as e.g. "850 Kbps" or "12.4 Mbps". */
    fun formatSpeed(bitsPerSecond: Long): String = when {
        bitsPerSecond >= 1_000_000_000 -> "%.1f Gbps".format(bitsPerSecond / 1_000_000_000.0)
        bitsPerSecond >= 1_000_000 -> "%.1f Mbps".format(bitsPerSecond / 1_000_000.0)
        else -> "%d Kbps".format(bitsPerSecond / 1_000)
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
