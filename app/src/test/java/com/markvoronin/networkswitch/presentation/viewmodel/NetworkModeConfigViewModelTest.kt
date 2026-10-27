package com.markvoronin.networkswitch.presentation.viewmodel

import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.markvoronin.networkswitch.domain.usecase.UpdateToggleModeConfigUseCase
import com.markvoronin.networkswitch.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class NetworkModeConfigViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var getConfig: GetToggleModeConfigUseCase
    private lateinit var updateConfig: UpdateToggleModeConfigUseCase

    private val storedConfig = ToggleModeConfig(NetworkMode.GSM_ONLY, NetworkMode.NR_ONLY)

    @Before
    fun setUp() {
        getConfig = mockk()
        updateConfig = mockk(relaxed = true)
        coEvery { getConfig() } returns storedConfig
    }

    private fun createViewModel() = NetworkModeConfigViewModel(getConfig, updateConfig)

    @Test
    fun `loads the stored configuration`() = runTest {
        assertEquals(storedConfig, createViewModel().currentConfig.value)
    }

    @Test
    fun `keeps defaults when loading fails`() = runTest {
        coEvery { getConfig() } throws RuntimeException("io")

        val config = createViewModel().currentConfig.value

        assertEquals(NetworkMode.LTE_ONLY, config.modeA)
        assertEquals(NetworkMode.NR_LTE, config.modeB)
    }

    @Test
    fun `saves a valid configuration`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateModeA(NetworkMode.LTE_ONLY)

        viewModel.saveConfiguration()

        coVerify { updateConfig(ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)) }
        assertTrue(viewModel.configSaved.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `does not save when both modes are the same`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateModeA(NetworkMode.NR_ONLY)

        viewModel.saveConfiguration()

        coVerify(exactly = 0) { updateConfig(any()) }
        assertFalse(viewModel.configSaved.value)
    }

    @Test
    fun `reports a failed save`() = runTest {
        coEvery { updateConfig(any()) } throws RuntimeException("io")
        val viewModel = createViewModel()

        viewModel.saveConfiguration()

        assertTrue(viewModel.saveFailed.value)
        assertFalse(viewModel.configSaved.value)
        assertFalse(viewModel.isLoading.value)

        viewModel.resetSaveFailedState()
        assertFalse(viewModel.saveFailed.value)
    }
}
