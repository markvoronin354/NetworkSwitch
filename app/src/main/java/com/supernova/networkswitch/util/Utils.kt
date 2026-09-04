package com.supernova.networkswitch.util

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

