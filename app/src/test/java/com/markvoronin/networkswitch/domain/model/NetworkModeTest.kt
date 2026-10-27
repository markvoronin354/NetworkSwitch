package com.markvoronin.networkswitch.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NetworkMode enum
 */
class NetworkModeTest {
    
    @Test
    fun testNetworkModeFromValue() {
        // Test basic modes
        assertEquals(NetworkMode.GSM_ONLY, NetworkMode.fromValue(1))
        assertEquals(NetworkMode.WCDMA_ONLY, NetworkMode.fromValue(2))
        assertEquals(NetworkMode.LTE_ONLY, NetworkMode.fromValue(11))
        assertEquals(NetworkMode.NR_ONLY, NetworkMode.fromValue(23))
        
        // Test combined modes
        assertEquals(NetworkMode.NR_LTE, NetworkMode.fromValue(24))
        assertEquals(NetworkMode.NR_LTE_GSM_WCDMA, NetworkMode.fromValue(26))
        
        // Test invalid value
        assertNull(NetworkMode.fromValue(-1))
        assertNull(NetworkMode.fromValue(999))
    }
    
    @Test
    fun testNetworkModeDisplayNames() {
        assertEquals("2G Only (GSM)", NetworkMode.GSM_ONLY.displayName)
        assertEquals("3G Only (WCDMA)", NetworkMode.WCDMA_ONLY.displayName)
        assertEquals("4G Only (LTE)", NetworkMode.LTE_ONLY.displayName)
        assertEquals("5G Only (NR)", NetworkMode.NR_ONLY.displayName)
        assertEquals("4G/5G (NR/LTE)", NetworkMode.NR_LTE.displayName)
    }

    @Test
    fun testNetworkModeTileLabel() {
        // Single network modes
        assertEquals("2G", NetworkMode.GSM_ONLY.tileLabel)
        assertEquals("3G", NetworkMode.WCDMA_ONLY.tileLabel)
        assertEquals("4G", NetworkMode.LTE_ONLY.tileLabel)
        assertEquals("5G", NetworkMode.NR_ONLY.tileLabel)

        // Multi-network modes (highest mode)
        assertEquals("5G", NetworkMode.NR_LTE.tileLabel)
        assertEquals("5G", NetworkMode.NR_LTE_GSM_WCDMA.tileLabel)
        assertEquals("4G", NetworkMode.LTE_GSM_WCDMA.tileLabel)
        assertEquals("4G", NetworkMode.LTE_WCDMA.tileLabel)
        assertEquals("3G", NetworkMode.WCDMA_PREF.tileLabel)
    }

    @Test
    fun `there are 34 modes with unique RIL values`() {
        assertEquals(34, NetworkMode.entries.size)
        assertEquals(34, NetworkMode.entries.map { it.value }.toSet().size)
        assertEquals((0..33).toSet(), NetworkMode.entries.map { it.value }.toSet())
    }

    @Test
    fun `every mode allows at least one technology`() {
        NetworkMode.entries.forEach { assertTrue("$it", it.technologies.isNotEmpty()) }
    }

    @Test
    fun `tile label shows the highest generation allowed`() {
        // CDMA-only is 2G; EvDo is 3G
        assertEquals("2G", NetworkMode.CDMA_NO_EVDO.tileLabel)
        assertEquals("3G", NetworkMode.EVDO_NO_CDMA.tileLabel)
        assertEquals("3G", NetworkMode.CDMA.tileLabel)
        // "Global" has no LTE, so it must not claim 4G
        assertEquals("3G", NetworkMode.GLOBAL.tileLabel)
        assertEquals("4G", NetworkMode.LTE_CDMA_EVDO_GSM_WCDMA.tileLabel)
        assertEquals("3G", NetworkMode.TDSCDMA_ONLY.tileLabel)
        assertEquals("5G", NetworkMode.NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA.tileLabel)
    }

    @Test
    fun `fromTechnologies finds the mode allowing exactly those technologies`() {
        assertEquals(NetworkMode.LTE_ONLY, NetworkMode.fromTechnologies(setOf(RadioTechnology.LTE)))
        assertEquals(
            NetworkMode.NR_LTE,
            NetworkMode.fromTechnologies(setOf(RadioTechnology.NR, RadioTechnology.LTE))
        )
    }

    @Test
    fun `fromTechnologies returns null for unknown combinations`() {
        assertNull(NetworkMode.fromTechnologies(emptySet()))
        // NR + GSM without LTE isn't a defined mode
        assertNull(NetworkMode.fromTechnologies(setOf(RadioTechnology.NR, RadioTechnology.GSM)))
    }

    @Test
    fun `fromTechnologies returns the first of modes that share technologies`() {
        val shared = setOf(RadioTechnology.GSM, RadioTechnology.WCDMA)
        assertEquals(NetworkMode.WCDMA_PREF, NetworkMode.fromTechnologies(shared))
        assertEquals(shared, NetworkMode.GSM_UMTS.technologies)
    }
}
