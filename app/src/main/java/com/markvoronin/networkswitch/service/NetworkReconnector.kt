package com.markvoronin.networkswitch.service

import android.util.Log

/**
 * Drops and restores Wi-Fi and mobile data and restarts the modem, like the "Fix connectivity"
 * button in some Settings apps. Runs in the privileged (Shizuku / root) process, where `svc` is
 * allowed to change the radios. Only radios that are currently on are touched, and they are
 * always switched back on, even if something fails halfway.
 */
object NetworkReconnector {

    private const val TAG = "NetworkReconnector"
    private const val OFF_DURATION_MS = 2_000L

    /** Returns true only if every Wi-Fi and mobile data change succeeded. */
    fun reconnect(): Boolean {
        val wifiWasOn = readSetting("wifi_on") == "1"
        val dataWasOn = readSetting("mobile_data") == "1"
        if (!wifiWasOn && !dataWasOn) return true

        var ok = true
        try {
            if (wifiWasOn) ok = svc("wifi", "disable") && ok
            if (dataWasOn) ok = svc("data", "disable") && ok
            // Best effort: not every device lets the shell user restart the modem
            TelephonyController.restartModem()
            Thread.sleep(OFF_DURATION_MS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            ok = false
        } finally {
            if (wifiWasOn) ok = svc("wifi", "enable") && ok
            if (dataWasOn) ok = svc("data", "enable") && ok
        }
        return ok
    }

    private fun readSetting(key: String): String? = run("/system/bin/settings", "get", "global", key)

    // Not listed in `svc help` on recent Android versions, but still handled.
    private fun svc(radio: String, action: String): Boolean = run("/system/bin/svc", radio, action) != null

    /** Returns the command's output, or null if it failed to run or exited non-zero. */
    private fun run(vararg command: String): String? {
        return try {
            val process = ProcessBuilder(*command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0) output else null
        } catch (e: Exception) {
            Log.e(TAG, "Command failed: ${command.joinToString(" ")}", e)
            null
        }
    }
}
