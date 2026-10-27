package com.markvoronin.networkswitch.service

import android.content.Context
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.RadioTechnology
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method

/**
 * Talks to the hidden `ITelephony` interface. Runs inside the privileged
 * Shizuku / root service processes, shared by both controller services.
 *
 * The telephony API changed shape across Android releases, so every call tries
 * the known variants in order of preference until one reports success.
 */
internal object TelephonyController {

    private const val TAG = "NetworkSwitch"
    private const val PHONE_PACKAGE = "com.android.phone"
    private const val TELEPHONY_MANAGER = "android.telephony.TelephonyManager"

    /** Value returned by [getNetworkMode] when the current mode can't be determined. */
    const val MODE_UNKNOWN = -1

    fun allowHiddenApiAccess() {
        try {
            HiddenApiBypass.setHiddenApiExemptions("Landroid/telephony/", "Lcom/android/internal/telephony/")
        } catch (e: Throwable) {
            Log.e(TAG, "HiddenApiBypass failed", e)
        }
    }

    fun isAvailable(): Boolean = getITelephony() != null

    /**
     * Returns the [NetworkMode.value] currently allowed for [subId], or [MODE_UNKNOWN].
     */
    fun getNetworkMode(subId: Int): Int {
        try {
            val telephony = getITelephony() ?: return MODE_UNKNOWN
            val methods = telephony.javaClass.methods

            // Android 11+: allowed network types bitmask
            for (m in methods.named("getAllowedNetworkTypesForReason")) {
                try {
                    val result = when {
                        m.parameterCount == 2 -> m.invoke(telephony, subId, reasonUser)
                        m.parameterCount == 3 && m.parameterTypes[2] == String::class.java ->
                            m.invoke(telephony, subId, reasonUser, PHONE_PACKAGE)
                        else -> continue
                    }
                    val bitmask = (result as? Number)?.toLong() ?: continue
                    val mode = modeForBitmask(bitmask)
                    Log.i(TAG, "getNetworkMode(subId=$subId) bitmask=$bitmask -> $mode")
                    return mode?.value ?: MODE_UNKNOWN
                } catch (_: Exception) {
                    // Try next variant
                }
            }

            // Android 10: RIL preferred network type
            for (m in methods.named("getPreferredNetworkType")) {
                try {
                    val result = when {
                        m.parameterCount == 1 -> m.invoke(telephony, subId)
                        m.parameterCount == 2 && m.parameterTypes[1] == String::class.java ->
                            m.invoke(telephony, subId, PHONE_PACKAGE)
                        else -> continue
                    }
                    val value = (result as? Number)?.toInt() ?: continue
                    Log.i(TAG, "getNetworkMode(subId=$subId) preferred type -> $value")
                    return if (NetworkMode.fromValue(value) != null) value else MODE_UNKNOWN
                } catch (_: Exception) {
                    // Try next variant
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error in getNetworkMode", e)
        }
        return MODE_UNKNOWN
    }

    /**
     * Applies the mode with RIL constant [modeValue] to [subId].
     * Returns true only if the framework reported success.
     */
    fun setNetworkMode(subId: Int, modeValue: Int): Boolean {
        val mode = NetworkMode.fromValue(modeValue)
        if (mode == null) {
            Log.w(TAG, "Unknown network mode $modeValue")
            return false
        }
        try {
            val telephony = getITelephony() ?: return false
            val bitmask = bitmaskFor(mode)
            val methods = telephony.javaClass.methods

            for (m in methods.named("setAllowedNetworkTypesForReason")) {
                val result = when {
                    m.parameterCount == 3 -> tryInvoke(m, telephony, subId, reasonUser, bitmask)
                    m.parameterCount == 4 && m.parameterTypes[3] == String::class.java ->
                        tryInvoke(m, telephony, subId, reasonUser, bitmask, PHONE_PACKAGE)
                    else -> continue
                }
                if (result) {
                    Log.i(TAG, "setAllowedNetworkTypesForReason(subId=$subId, mode=$mode) ok")
                    return true
                }
            }

            for (m in methods.named("setAllowedNetworkTypes")) {
                val result = when {
                    m.parameterCount == 2 -> tryInvoke(m, telephony, subId, bitmask)
                    m.parameterCount == 3 && m.parameterTypes[1] == Long::class.javaPrimitiveType ->
                        tryInvoke(m, telephony, subId, bitmask, PHONE_PACKAGE)
                    else -> continue
                }
                if (result) {
                    Log.i(TAG, "setAllowedNetworkTypes(subId=$subId, mode=$mode) ok")
                    return true
                }
            }

            for (m in methods.named("setPreferredNetworkType").filter { it.parameterCount == 2 }) {
                if (tryInvoke(m, telephony, subId, mode.value)) {
                    Log.i(TAG, "setPreferredNetworkType(subId=$subId, mode=$mode) ok")
                    return true
                }
            }

            Log.w(TAG, "Failed to set network mode $mode for subId $subId")
        } catch (e: Throwable) {
            Log.e(TAG, "Error in setNetworkMode", e)
        }
        return false
    }

    /** Invokes [method]; a thrown exception or an explicit `false` result is a failure. */
    private fun tryInvoke(method: Method, target: Any, vararg args: Any): Boolean {
        return try {
            method.invoke(target, *args) != false
        } catch (_: Exception) {
            false
        }
    }

    private fun Array<Method>.named(name: String): List<Method> = filter { it.name == name }

    private fun getITelephony(): Any? {
        return try {
            val binder = ServiceManager.getService(Context.TELEPHONY_SERVICE)
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ITelephony", e)
            null
        }
    }

    // --- Bitmask <-> mode translation -------------------------------------------------

    private fun bitmaskFor(mode: NetworkMode): Long =
        mode.technologies.fold(0L) { acc, tech -> acc or bitsFor(tech) }

    /**
     * Maps an allowed-network-types bitmask to the mode allowing exactly the same
     * technologies, or null. A technology counts as allowed if any of its bits is set.
     */
    private fun modeForBitmask(bitmask: Long): NetworkMode? {
        val technologies = RadioTechnology.entries.filterTo(mutableSetOf()) { (bitmask and bitsFor(it)) != 0L }
        return NetworkMode.fromTechnologies(technologies)
    }

    private val reasonUser by lazy {
        staticInt("ALLOWED_NETWORK_TYPES_REASON_USER", 0)
    }

    private val technologyBits: Map<RadioTechnology, Long> by lazy {
        mapOf(
            RadioTechnology.GSM to (bit("GSM", 15) or bit("GPRS", 0) or bit("EDGE", 1)),
            RadioTechnology.WCDMA to (bit("UMTS", 2) or bit("HSDPA", 7) or bit("HSUPA", 8) or
                bit("HSPA", 9) or bit("HSPAP", 14)),
            RadioTechnology.CDMA to (bit("CDMA", 3) or bit("1xRTT", 6)),
            RadioTechnology.EVDO to (bit("EVDO_0", 4) or bit("EVDO_A", 5) or bit("EVDO_B", 11)),
            RadioTechnology.LTE to (bit("LTE", 12) or bit("LTE_CA", 18)),
            RadioTechnology.TDSCDMA to bit("TD_SCDMA", 16),
            RadioTechnology.NR to bit("NR", 19)
        )
    }

    private fun bitsFor(technology: RadioTechnology): Long = technologyBits.getValue(technology)

    /** Reads `TelephonyManager.NETWORK_TYPE_BITMASK_<name>`, falling back to `1 << fallbackShift`. */
    private fun bit(name: String, fallbackShift: Int): Long = try {
        val field = Class.forName(TELEPHONY_MANAGER).getDeclaredField("NETWORK_TYPE_BITMASK_$name")
        field.isAccessible = true
        field.getLong(null)
    } catch (_: Exception) {
        1L shl fallbackShift
    }

    private fun staticInt(fieldName: String, fallback: Int): Int = try {
        val field = Class.forName(TELEPHONY_MANAGER).getDeclaredField(fieldName)
        field.isAccessible = true
        field.getInt(null)
    } catch (_: Exception) {
        fallback
    }
}
