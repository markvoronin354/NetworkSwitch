package com.supernova.networkswitch.service

import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NetworkWidgetProviderTest {

    private lateinit var getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase
    private lateinit var toggleNetworkModeUseCase: ToggleNetworkModeUseCase
    private lateinit var getToggleModeConfigUseCase: GetToggleModeConfigUseCase
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var provider: NetworkWidgetProvider

    @Before
    fun setUp() {
        getCurrentNetworkModeUseCase = mockk()
        toggleNetworkModeUseCase = mockk()
        getToggleModeConfigUseCase = mockk()
        preferencesRepository = mockk()

        provider = NetworkWidgetProvider().apply {
            this.getCurrentNetworkModeUseCase = this@NetworkWidgetProviderTest.getCurrentNetworkModeUseCase
            this.toggleNetworkModeUseCase = this@NetworkWidgetProviderTest.toggleNetworkModeUseCase
            this.getToggleModeConfigUseCase = this@NetworkWidgetProviderTest.getToggleModeConfigUseCase
            this.preferencesRepository = this@NetworkWidgetProviderTest.preferencesRepository
        }
    }

    @Test
    fun `test toggle network mode use case integration with widget provider`() = runTest {
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        coEvery { getToggleModeConfigUseCase() } returns config
        coEvery { toggleNetworkModeUseCase(any()) } returns Result.success(NetworkMode.NR_ONLY)

        val result = toggleNetworkModeUseCase(1)
        assertEquals(NetworkMode.NR_ONLY, result.getOrNull())
    }

    @Test
    fun `test Samsung 1x tall widget calculates pill corner radius and vertical inset`() {
        val heightPx = 200f
        val widgetHeightDp = 110
        val (cornerRadius, verticalInset) = provider.calculateCornerRadiusAndInset(
            heightPx = heightPx,
            widgetHeightDp = widgetHeightDp,
            isSamsung = true
        )
        // verticalInset = 200 * 0.185 = 37f
        // pillHeight = 200 - 74 = 126f
        // cornerRadius = 126 / 2 = 63f
        assertEquals(37f, verticalInset, 0.01f)
        assertEquals(63f, cornerRadius, 0.01f)
    }

    @Test
    fun `test Samsung 2x tall widget uses standard rounded rectangle corner radius without inset`() {
        val heightPx = 300f
        val widgetHeightDp = 150
        val (cornerRadius, verticalInset) = provider.calculateCornerRadiusAndInset(
            heightPx = heightPx,
            widgetHeightDp = widgetHeightDp,
            isSamsung = true
        )
        assertEquals(0f, verticalInset, 0.01f)
        assertEquals(66f, cornerRadius, 0.01f)
    }

    @Test
    fun `test non-Samsung widget uses standard rounded rectangle corner radius without inset`() {
        val heightPx = 120f
        val widgetHeightDp = 60
        val (cornerRadius, verticalInset) = provider.calculateCornerRadiusAndInset(
            heightPx = heightPx,
            widgetHeightDp = widgetHeightDp,
            isSamsung = false
        )
        assertEquals(0f, verticalInset, 0.01f)
        assertEquals(26.4f, cornerRadius, 0.01f)
    }
}
