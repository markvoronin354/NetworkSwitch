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
