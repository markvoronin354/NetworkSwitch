package com.markvoronin.networkswitch.presentation.viewmodel

import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.repository.NetworkControlRepository
import com.markvoronin.networkswitch.domain.repository.NetworkStatsRepository
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import com.markvoronin.networkswitch.domain.repository.SpeedTestRepository
import com.markvoronin.networkswitch.domain.usecase.CheckCompatibilityUseCase
import com.markvoronin.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.markvoronin.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.markvoronin.networkswitch.domain.usecase.UpdateControlMethodUseCase
import com.markvoronin.networkswitch.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var checkCompatibility: CheckCompatibilityUseCase
    private lateinit var getCurrentNetworkMode: GetCurrentNetworkModeUseCase
    private lateinit var toggleNetworkMode: ToggleNetworkModeUseCase
    private lateinit var updateControlMethod: UpdateControlMethodUseCase
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var networkControlRepository: NetworkControlRepository
    private lateinit var networkStatsRepository: NetworkStatsRepository
    private lateinit var speedTestRepository: SpeedTestRepository

    private val controlMethod = MutableStateFlow(ControlMethod.SHIZUKU)
    private val toggleConfig = MutableStateFlow(ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_LTE))
    private val speedTestResults = MutableStateFlow<Map<NetworkMode, Long>>(emptyMap())

    @Before
    fun setUp() {
        checkCompatibility = mockk()
        getCurrentNetworkMode = mockk()
        toggleNetworkMode = mockk()
        updateControlMethod = mockk(relaxed = true)
        preferencesRepository = mockk()
        networkControlRepository = mockk(relaxed = true)
        networkStatsRepository = mockk(relaxed = true)
        speedTestRepository = mockk()

        coEvery { checkCompatibility() } returns CompatibilityState.Compatible
        coEvery { getCurrentNetworkMode(any()) } returns Result.success(NetworkMode.LTE_ONLY)
        coEvery { preferencesRepository.observeControlMethod() } returns controlMethod
        coEvery { preferencesRepository.observeToggleModeConfig() } returns toggleConfig
        coEvery { preferencesRepository.observeSpeedTestResults() } returns speedTestResults
        coEvery { networkControlRepository.observePermissionStateChanges() } returns emptyFlow()
    }

    private fun createViewModel() = MainViewModel(
        checkCompatibility,
        getCurrentNetworkMode,
        toggleNetworkMode,
        updateControlMethod,
        preferencesRepository,
        networkControlRepository,
        networkStatsRepository,
        speedTestRepository
    )

    @Test
    fun `refreshAllData loads the current mode when compatible`() = runTest {
        val viewModel = createViewModel()

        viewModel.refreshAllData()

        assertEquals(CompatibilityState.Compatible, viewModel.compatibilityState)
        assertEquals(NetworkMode.LTE_ONLY, viewModel.currentNetworkMode)
    }

    @Test
    fun `runSpeedTest saves the result under the current mode`() = runTest {
        coEvery { speedTestRepository.runDownloadTest() } returns Result.success(50_000_000L)
        coEvery { preferencesRepository.setSpeedTestResult(any(), any()) } returns Unit
        val viewModel = createViewModel()
        viewModel.refreshAllData()

        viewModel.runSpeedTest()

        coVerify { preferencesRepository.setSpeedTestResult(NetworkMode.LTE_ONLY, 50_000_000L) }
        assertFalse(viewModel.isSpeedTestRunning)
        assertFalse(viewModel.speedTestFailed)
    }

    @Test
    fun `runSpeedTest flags a failure and saves nothing`() = runTest {
        coEvery { speedTestRepository.runDownloadTest() } returns Result.failure(java.io.IOException())
        val viewModel = createViewModel()
        viewModel.refreshAllData()

        viewModel.runSpeedTest()

        assertTrue(viewModel.speedTestFailed)
        assertFalse(viewModel.isSpeedTestRunning)
        coVerify(exactly = 0) { preferencesRepository.setSpeedTestResult(any(), any()) }
    }

    @Test
    fun `refreshAllData clears the current mode when not compatible`() = runTest {
        val viewModel = createViewModel()
        viewModel.refreshAllData()
        coEvery { checkCompatibility() } returns CompatibilityState.PermissionDenied(ControlMethod.SHIZUKU)

        viewModel.refreshAllData()

        assertTrue(viewModel.compatibilityState is CompatibilityState.PermissionDenied)
        assertNull(viewModel.currentNetworkMode)
    }

    @Test
    fun `toggle success updates the current mode and clears loading`() = runTest {
        coEvery { toggleNetworkMode(any()) } returns Result.success(NetworkMode.NR_LTE)
        val viewModel = createViewModel()

        viewModel.toggleNetworkMode()

        assertEquals(NetworkMode.NR_LTE, viewModel.currentNetworkMode)
        assertFalse(viewModel.isLoading)
        assertFalse(viewModel.switchFailed)
    }

    @Test
    fun `toggle failure is reported and the real mode is re-read`() = runTest {
        coEvery { toggleNetworkMode(any()) } returns Result.failure(IllegalStateException("rejected"))
        coEvery { getCurrentNetworkMode(any()) } returns Result.success(NetworkMode.LTE_ONLY)
        val viewModel = createViewModel()

        viewModel.toggleNetworkMode()

        assertTrue(viewModel.switchFailed)
        assertFalse(viewModel.isLoading)
        assertEquals(NetworkMode.LTE_ONLY, viewModel.currentNetworkMode)
        coVerify { getCurrentNetworkMode(any()) }
    }

    @Test
    fun `dismissSwitchFailure clears the failure flag`() = runTest {
        coEvery { toggleNetworkMode(any()) } returns Result.failure(IllegalStateException("rejected"))
        val viewModel = createViewModel()
        viewModel.toggleNetworkMode()

        viewModel.dismissSwitchFailure()

        assertFalse(viewModel.switchFailed)
    }

    @Test
    fun `toggle config changes from preferences are reflected`() = runTest {
        val viewModel = createViewModel()
        val newConfig = ToggleModeConfig(NetworkMode.GSM_ONLY, NetworkMode.NR_ONLY)

        toggleConfig.value = newConfig

        assertEquals(newConfig, viewModel.toggleModeConfig)
    }

    @Test
    fun `control method changes from preferences trigger a new compatibility check`() = runTest {
        val viewModel = createViewModel()
        coEvery { checkCompatibility() } returns CompatibilityState.PermissionDenied(ControlMethod.ROOT)

        controlMethod.value = ControlMethod.ROOT

        assertEquals(ControlMethod.ROOT, viewModel.selectedMethod)
        assertTrue(viewModel.compatibilityState is CompatibilityState.PermissionDenied)
    }
}
