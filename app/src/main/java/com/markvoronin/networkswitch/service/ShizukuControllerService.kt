package com.markvoronin.networkswitch.service

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.markvoronin.networkswitch.IShizukuController

/**
 * Runs in the Shizuku user-service process and forwards calls to [TelephonyController].
 */
class ShizukuControllerService() : IShizukuController.Stub() {

    init {
        TelephonyController.allowHiddenApiAccess()
    }

    // Shizuku instantiates user services through this constructor when available.
    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : this()

    override fun compatibilityCheck(subId: Int): Boolean = TelephonyController.isAvailable()

    override fun getCurrentNetworkMode(subId: Int): Int = TelephonyController.getNetworkMode(subId)

    override fun setNetworkMode(subId: Int, networkMode: Int): Boolean =
        TelephonyController.setNetworkMode(subId, networkMode)

    override fun destroy() {
        Log.d("NetworkSwitch", "ShizukuControllerService: destroy")
    }
}
