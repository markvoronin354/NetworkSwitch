package com.markvoronin.networkswitch.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.NetworkStats
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.repository.NetworkControlRepository
import com.markvoronin.networkswitch.domain.repository.NetworkStatsRepository
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import com.markvoronin.networkswitch.domain.repository.SpeedTestRepository
import com.markvoronin.networkswitch.domain.usecase.CheckCompatibilityUseCase
import com.markvoronin.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.markvoronin.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.markvoronin.networkswitch.domain.usecase.UpdateControlMethodUseCase
import com.markvoronin.networkswitch.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkCompatibilityUseCase: CheckCompatibilityUseCase,
    private val getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase,
    private val toggleNetworkModeUseCase: ToggleNetworkModeUseCase,
    private val updateControlMethodUseCase: UpdateControlMethodUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val networkControlRepository: NetworkControlRepository,
    private val networkStatsRepository: NetworkStatsRepository,
    private val speedTestRepository: SpeedTestRepository
) : ViewModel() {

    // Current control method selection
    var selectedMethod by mutableStateOf(ControlMethod.SHIZUKU)
        private set

    // Current network mode, or null while unknown
    var currentNetworkMode by mutableStateOf<NetworkMode?>(null)
        private set

    // Toggle mode configuration
    var toggleModeConfig by mutableStateOf(ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_LTE))
        private set

    var isLoading by mutableStateOf(false)
        private set

    var compatibilityState by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set

    // Set when the last switch attempt failed, until the UI acknowledges it
    var switchFailed by mutableStateOf(false)
        private set

    // Latest network stats; only updated while observeNetworkStats() is running
    var networkStats by mutableStateOf(NetworkStats())
        private set

    var isSpeedTestRunning by mutableStateOf(false)
        private set

    // Set when the last speed test failed, until the UI acknowledges it
    var speedTestFailed by mutableStateOf(false)
        private set

    // Last speed test result (bits per second) for each network mode
    var speedTestResults by mutableStateOf<Map<NetworkMode, Long>>(emptyMap())
        private set

    private var compatibilityJob: Job? = null
    private var speedTestJob: Job? = null

    init {
        observeControlMethodPreference()
        observeToggleModeConfig()
        observeSpeedTestResults()
        observePermissionStateChanges()
        // The initial compatibility check and state refresh happen in refreshAllData() from onResume
    }

    /**
     * Observe permission state changes (e.g. Shizuku permission granted)
     */
    private fun observePermissionStateChanges() {
        viewModelScope.launch {
            networkControlRepository.observePermissionStateChanges().collectLatest {
                // Let Shizuku settle before re-checking
                delay(PERMISSION_SETTLE_DELAY_MS)
                checkCompatibility()
            }
        }
    }

    private fun observeControlMethodPreference() {
        viewModelScope.launch {
            preferencesRepository.observeControlMethod().collectLatest { preferredMethod ->
                if (selectedMethod != preferredMethod) {
                    selectedMethod = preferredMethod
                    currentNetworkMode = null
                    checkCompatibility(showPending = true)
                }
            }
        }
    }

    private fun observeToggleModeConfig() {
        viewModelScope.launch {
            preferencesRepository.observeToggleModeConfig().collectLatest { newConfig ->
                toggleModeConfig = newConfig
            }
        }
    }

    private fun observeSpeedTestResults() {
        viewModelScope.launch {
            preferencesRepository.observeSpeedTestResults().collectLatest { results ->
                speedTestResults = results
            }
        }
    }

    /**
     * Re-check compatibility and, if the device is usable, refresh the displayed mode.
     * Only the first check shows the "checking" spinner; later ones update in place.
     */
    private fun checkCompatibility(showPending: Boolean = false) {
        compatibilityJob?.cancel()
        compatibilityJob = viewModelScope.launch {
            if (showPending) compatibilityState = CompatibilityState.Pending
            val state = checkCompatibilityUseCase()
            compatibilityState = state
            if (state is CompatibilityState.Compatible) {
                refreshNetworkState()
            } else {
                currentNetworkMode = null
            }
        }
    }

    /**
     * Switch control method
     */
    fun switchToMethod(method: ControlMethod) {
        viewModelScope.launch {
            updateControlMethodUseCase(method)
            networkControlRepository.requestPermission(method)
        }
    }

    /**
     * Retry compatibility check
     */
    fun retryCompatibilityCheck() {
        viewModelScope.launch {
            networkControlRepository.requestPermission(selectedMethod)
        }
        checkCompatibility(showPending = true)
    }

    /**
     * Refresh all data when app resumes
     */
    fun refreshAllData() {
        checkCompatibility(showPending = compatibilityState is CompatibilityState.Pending)
    }

    /**
     * Toggle network mode using configurable modes
     */
    fun toggleNetworkMode() {
        if (isLoading) return

        // A result measured across a mode switch would be filed under the wrong mode
        speedTestJob?.cancel()
        isLoading = true
        viewModelScope.launch {
            try {
                toggleNetworkModeUseCase(Utils.getValidSubId())
                    .onSuccess { newMode ->
                        currentNetworkMode = newMode
                    }
                    .onFailure {
                        switchFailed = true
                        refreshNetworkState()
                    }
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Run a speed test and file the result under the current network mode
     */
    fun runSpeedTest() {
        val mode = currentNetworkMode ?: return
        if (isSpeedTestRunning) return

        isSpeedTestRunning = true
        speedTestJob = viewModelScope.launch {
            try {
                speedTestRepository.runDownloadTest()
                    .onSuccess { preferencesRepository.setSpeedTestResult(mode, it) }
                    .onFailure { speedTestFailed = true }
            } finally {
                isSpeedTestRunning = false
            }
        }
    }

    fun dismissSpeedTestFailure() {
        speedTestFailed = false
    }

    /**
     * Collect live network stats until cancelled. Call from a started-lifecycle scope so
     * polling and the signal listener stop when the screen is not visible.
     */
    suspend fun observeNetworkStats() {
        networkStatsRepository.observeNetworkStats().collect { networkStats = it }
    }

    fun dismissSwitchFailure() {
        switchFailed = false
    }

    private suspend fun refreshNetworkState() {
        currentNetworkMode = getCurrentNetworkModeUseCase(Utils.getValidSubId()).getOrNull()
    }

    private companion object {
        const val PERMISSION_SETTLE_DELAY_MS = 100L
    }
}
