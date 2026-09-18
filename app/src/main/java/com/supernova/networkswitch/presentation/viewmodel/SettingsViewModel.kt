package com.supernova.networkswitch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.WidgetCustomizationConfig
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
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
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
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
                android.util.Log.d("NetworkSwitch", "SettingsViewModel: Permission state changed, re-checking all compatibility...")
                kotlinx.coroutines.delay(100)
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
        networkControlRepository.requestPermission(ControlMethod.SHIZUKU)
        checkAllCompatibility()
    }

    fun updateWidgetCustomization(config: WidgetCustomizationConfig) {
        viewModelScope.launch {
            preferencesRepository.setWidgetCustomizationConfig(config)
        }
    }
    
    private fun checkAllCompatibility() {
        viewModelScope.launch {
            rootCompatibility = CompatibilityState.Pending
            shizukuCompatibility = CompatibilityState.Pending
            
            // Check both methods in parallel
            val rootResult = async { networkControlRepository.checkCompatibility(ControlMethod.ROOT) }
            val shizukuResult = async { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) }
            
            rootCompatibility = rootResult.await()
            shizukuCompatibility = shizukuResult.await()
        }
    }
}
