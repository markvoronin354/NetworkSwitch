package com.markvoronin.networkswitch.service

import android.content.Intent
import com.markvoronin.networkswitch.IRootController
import com.topjohnwu.superuser.ipc.RootService

/**
 * Runs in the root process started by libsu and forwards calls to [TelephonyController].
 */
class RootNetworkControllerService : RootService() {

    init {
        TelephonyController.allowHiddenApiAccess()
    }

    override fun onBind(intent: Intent) = object : IRootController.Stub() {

        override fun compatibilityCheck(subId: Int): Boolean = TelephonyController.isAvailable()

        override fun getCurrentNetworkMode(subId: Int): Int = TelephonyController.getNetworkMode(subId)

        override fun setNetworkMode(subId: Int, networkMode: Int): Boolean =
            TelephonyController.setNetworkMode(subId, networkMode)

        override fun reconnectNetwork(): Boolean = NetworkReconnector.reconnect()
    }
}
