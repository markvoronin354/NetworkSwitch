package com.markvoronin.networkswitch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.WidgetCustomizationConfig
import com.markvoronin.networkswitch.domain.repository.NetworkControlRepository
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings screen ViewModel using clean architecture
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val networkControlRepository: NetworkControlRepository
) : ViewModel() {
    
    val controlMethod: StateFlow<ControlMethod> = preferencesRepository.observeControlMethod()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ControlMethod.SHIZUKU
        )

    val widgetCustomization: StateFlow<WidgetCustomizationConfig> =
        preferencesRepository.observeWidgetCustomizationConfig()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = WidgetCustomizationConfig()
            )
    
    // Compatibility status for each method
    var rootCompatibility by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set
    
    var shizukuCompatibility by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set
    
    init {
        checkAllCompatibility()
        observePermissionStateChanges()
    }

    private fun observePermissionStateChanges() {
        viewModelScope.launch {
            networkControlRepository.observePermissionStateChanges().collect {
                // Let Shizuku settle before re-checking
                delay(100)
                checkAllCompatibility()
            }
        }
    }
    
    fun updateControlMethod(method: ControlMethod) {
        viewModelScope.launch {
            preferencesRepository.setControlMethod(method)
            networkControlRepository.resetConnections()
            checkAllCompatibility()
            if (method == ControlMethod.SHIZUKU) {
                networkControlRepository.requestPermission(ControlMethod.SHIZUKU)
            }
        }
    }
    
    fun retryCompatibilityCheck() {
        viewModelScope.launch {
            networkControlRepository.requestPermission(ControlMethod.SHIZUKU)
        }
        checkAllCompatibility()
    }

    fun updateWidgetCustomization(config: WidgetCustomizationConfig) {
        viewModelScope.launch {
            preferencesRepository.setWidgetCustomizationConfig(config)
        }
    }
    
    private var compatibilityJob: Job? = null

    private fun checkAllCompatibility() {
        compatibilityJob?.cancel()
        compatibilityJob = viewModelScope.launch {
            rootCompatibility = CompatibilityState.Pending
            shizukuCompatibility = CompatibilityState.Pending

            // Check both methods in parallel; each result shows as soon as it arrives
            coroutineScope {
                launch { rootCompatibility = networkControlRepository.checkCompatibility(ControlMethod.ROOT) }
                launch { shizukuCompatibility = networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) }
            }
        }
    }
}
