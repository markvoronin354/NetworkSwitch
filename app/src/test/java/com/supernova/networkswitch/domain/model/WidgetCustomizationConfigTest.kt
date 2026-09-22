package com.supernova.networkswitch.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetCustomizationConfigTest {

    @Test
    fun testGetEffectiveColorWithZeroOpacity() {
        val config = WidgetCustomizationConfig(
            useSystemColor = false,
            customColorHex = 0xFF333333.toInt(),
            opacity = 0.0f
        )
        val effectiveColor = config.getEffectiveColor(0xFF000000.toInt())
        val alpha = (effectiveColor shr 24) and 0xFF
        assertEquals(0, alpha)
    }

    @Test
    fun testGetEffectiveColorWithFullOpacity() {
        val config = WidgetCustomizationConfig(
            useSystemColor = false,
            customColorHex = 0xFF333333.toInt(),
            opacity = 1.0f
        )
        val effectiveColor = config.getEffectiveColor(0xFF000000.toInt())
        val alpha = (effectiveColor shr 24) and 0xFF
        assertEquals(255, alpha)
    }
}
