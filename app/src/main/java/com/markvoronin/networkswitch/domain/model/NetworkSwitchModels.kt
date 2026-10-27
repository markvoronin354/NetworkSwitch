package com.markvoronin.networkswitch.domain.model

enum class ControlMethod {
    ROOT,
    SHIZUKU
}

/**
 * Radio access technologies a [NetworkMode] allows the modem to use.
 */
enum class RadioTechnology {
    GSM,
    WCDMA,
    CDMA,
    EVDO,
    LTE,
    TDSCDMA,
    NR
}

private typealias T = RadioTechnology

/**
 * Network modes, identified by their RIL preferred-network-type constant ([value]).
 * [technologies] is the single source of truth for what each mode allows; the
 * telephony bitmask sent to the modem and the tile label are both derived from it.
 */
enum class NetworkMode(val displayName: String, val value: Int, vararg technologies: RadioTechnology) {
    // Basic modes
    GSM_ONLY("2G Only (GSM)", 1, T.GSM),
    WCDMA_ONLY("3G Only (WCDMA)", 2, T.WCDMA),
    LTE_ONLY("4G Only (LTE)", 11, T.LTE),
    NR_ONLY("5G Only (NR)", 23, T.NR),

    // Preferred modes
    WCDMA_PREF("2G/3G (3G Preferred)", 0, T.GSM, T.WCDMA),
    GSM_UMTS("2G/3G (Auto)", 3, T.GSM, T.WCDMA),

    // Combined modes
    LTE_GSM_WCDMA("2G/3G/4G (LTE/GSM/WCDMA)", 9, T.LTE, T.GSM, T.WCDMA),
    LTE_WCDMA("3G/4G (LTE/WCDMA)", 12, T.LTE, T.WCDMA),
    NR_LTE("4G/5G (NR/LTE)", 24, T.NR, T.LTE),
    NR_LTE_GSM_WCDMA("2G/3G/4G/5G (NR/LTE/GSM/WCDMA)", 26, T.NR, T.LTE, T.GSM, T.WCDMA),
    NR_LTE_WCDMA("3G/4G/5G (NR/LTE/WCDMA)", 28, T.NR, T.LTE, T.WCDMA),

    // CDMA modes for US carriers
    CDMA("CDMA (Auto)", 4, T.CDMA, T.EVDO),
    CDMA_NO_EVDO("CDMA Only", 5, T.CDMA),
    EVDO_NO_CDMA("EvDo Only", 6, T.EVDO),
    LTE_CDMA_EVDO("CDMA/4G (LTE/CDMA/EvDo)", 8, T.LTE, T.CDMA, T.EVDO),
    NR_LTE_CDMA_EVDO("CDMA/4G/5G (NR/LTE/CDMA/EvDo)", 25, T.NR, T.LTE, T.CDMA, T.EVDO),

    // Global modes
    GLOBAL("Global (All)", 7, T.GSM, T.WCDMA, T.CDMA, T.EVDO),
    LTE_CDMA_EVDO_GSM_WCDMA("Global 4G (LTE/CDMA/EvDo/GSM/WCDMA)", 10, T.LTE, T.CDMA, T.EVDO, T.GSM, T.WCDMA),
    NR_LTE_CDMA_EVDO_GSM_WCDMA("Global 5G (NR/LTE/CDMA/EvDo/GSM/WCDMA)", 27, T.NR, T.LTE, T.CDMA, T.EVDO, T.GSM, T.WCDMA),

    // TD-SCDMA modes for China
    TDSCDMA_ONLY("TD-SCDMA Only", 13, T.TDSCDMA),
    TDSCDMA_WCDMA("3G (TD-SCDMA/WCDMA)", 14, T.TDSCDMA, T.WCDMA),
    LTE_TDSCDMA("4G (LTE/TD-SCDMA)", 15, T.LTE, T.TDSCDMA),
    TDSCDMA_GSM("2G/TD-SCDMA", 16, T.TDSCDMA, T.GSM),
    LTE_TDSCDMA_GSM("2G/4G (LTE/TD-SCDMA/GSM)", 17, T.LTE, T.TDSCDMA, T.GSM),
    TDSCDMA_GSM_WCDMA("2G/3G (TD-SCDMA/GSM/WCDMA)", 18, T.TDSCDMA, T.GSM, T.WCDMA),
    LTE_TDSCDMA_WCDMA("3G/4G (LTE/TD-SCDMA/WCDMA)", 19, T.LTE, T.TDSCDMA, T.WCDMA),
    LTE_TDSCDMA_GSM_WCDMA("2G/3G/4G (LTE/TD-SCDMA/GSM/WCDMA)", 20, T.LTE, T.TDSCDMA, T.GSM, T.WCDMA),
    TDSCDMA_CDMA_EVDO_GSM_WCDMA("Global 3G (TD-SCDMA/CDMA/EvDo/GSM/WCDMA)", 21, T.TDSCDMA, T.CDMA, T.EVDO, T.GSM, T.WCDMA),
    LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA("Global 4G (LTE/TD-SCDMA/CDMA/EvDo/GSM/WCDMA)", 22, T.LTE, T.TDSCDMA, T.CDMA, T.EVDO, T.GSM, T.WCDMA),
    NR_LTE_TDSCDMA("4G/5G (NR/LTE/TD-SCDMA)", 29, T.NR, T.LTE, T.TDSCDMA),
    NR_LTE_TDSCDMA_GSM("2G/4G/5G (NR/LTE/TD-SCDMA/GSM)", 30, T.NR, T.LTE, T.TDSCDMA, T.GSM),
    NR_LTE_TDSCDMA_WCDMA("3G/4G/5G (NR/LTE/TD-SCDMA/WCDMA)", 31, T.NR, T.LTE, T.TDSCDMA, T.WCDMA),
    NR_LTE_TDSCDMA_GSM_WCDMA("2G/3G/4G/5G (NR/LTE/TD-SCDMA/GSM/WCDMA)", 32, T.NR, T.LTE, T.TDSCDMA, T.GSM, T.WCDMA),
    NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA("Global 5G + TD-SCDMA (All Networks)", 33, T.NR, T.LTE, T.TDSCDMA, T.CDMA, T.EVDO, T.GSM, T.WCDMA);

    val technologies: Set<RadioTechnology> = technologies.toSet()

    /**
     * Short display text for Quick Settings Tile and widget: the highest
     * generation this mode allows (e.g. "5G" for 4G/5G, "4G" for LTE only).
     */
    val tileLabel: String
        get() = when {
            RadioTechnology.NR in technologies -> "5G"
            RadioTechnology.LTE in technologies -> "4G"
            RadioTechnology.WCDMA in technologies ||
                RadioTechnology.EVDO in technologies ||
                RadioTechnology.TDSCDMA in technologies -> "3G"
            else -> "2G"
        }

    companion object {
        /**
         * Get NetworkMode by RIL constant value
         */
        fun fromValue(value: Int): NetworkMode? = entries.find { it.value == value }

        /**
         * Get the mode that allows exactly [technologies], or null if no mode does.
         * Several modes can share the same technologies (e.g. [WCDMA_PREF] and
         * [GSM_UMTS]); the first declared one is returned.
         */
        fun fromTechnologies(technologies: Set<RadioTechnology>): NetworkMode? =
            entries.find { it.technologies == technologies }
    }
}

/**
 * Configuration for the two modes that can be toggled between
 */
data class ToggleModeConfig(
    val modeA: NetworkMode,
    val modeB: NetworkMode,
    val nextModeIsB: Boolean = true
) {
    fun getNextMode(): NetworkMode {
        return if (nextModeIsB) modeB else modeA
    }
    
    fun getCurrentMode(): NetworkMode {
        return if (nextModeIsB) modeA else modeB
    }
    
    fun toggle(): ToggleModeConfig {
        return copy(nextModeIsB = !nextModeIsB)
    }
}

sealed class CompatibilityState {
    object Pending : CompatibilityState()
    object Compatible : CompatibilityState()
    data class Incompatible(val reason: String) : CompatibilityState()
    data class PermissionDenied(val method: ControlMethod) : CompatibilityState()
}

/**
 * Configuration for widget appearance customization
 */
data class WidgetCustomizationConfig(
    val useSystemColor: Boolean = true,
    val customColorHex: Int = 0xFF333333.toInt(), // Default grey
    val opacity: Float = 0.50f // Default 50% opacity (50% transparent)
) {
    /**
     * Calculate final ARGB color integer combining RGB color and opacity
     */
    fun getEffectiveColor(systemColorHex: Int): Int {
        val baseColor = if (useSystemColor) systemColorHex else customColorHex
        val alphaInt = (opacity.coerceIn(0f, 1f) * 255).toInt()
        return (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
    }
}
