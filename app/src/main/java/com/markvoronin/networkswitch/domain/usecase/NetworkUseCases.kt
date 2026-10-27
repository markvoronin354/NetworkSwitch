package com.markvoronin.networkswitch.domain.usecase

import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.model.WidgetCustomizationConfig
import com.markvoronin.networkswitch.domain.repository.NetworkControlRepository
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class CheckCompatibilityUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): CompatibilityState {
        val controlMethod = preferencesRepository.getControlMethod()
        return networkControlRepository.checkCompatibility(controlMethod)
    }
}

class ToggleNetworkModeUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(subId: Int): Result<NetworkMode> {
        return try {
            val toggleConfig = preferencesRepository.getToggleModeConfig()
            val targetMode = chooseTarget(toggleConfig, networkControlRepository.getCurrentNetworkMode(subId))

            // Only update the stored state once the device has actually applied the mode
            val result = networkControlRepository.setNetworkMode(subId, targetMode)

            if (result.isSuccess) {
                preferencesRepository.setToggleModeConfig(
                    toggleConfig.copy(nextModeIsB = targetMode == toggleConfig.modeA)
                )
            }

            result.map { targetMode }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Prefers the real current mode over the stored flag, so the toggle still works after the
     * mode was changed outside the app. Falls back to the stored flag when the current mode is
     * unknown, matches neither mode, or can't be told apart (both modes allow the same technologies).
     */
    private fun chooseTarget(config: ToggleModeConfig, current: NetworkMode?): NetworkMode {
        val isModeA = current?.technologies == config.modeA.technologies
        val isModeB = current?.technologies == config.modeB.technologies
        return when {
            isModeA && !isModeB -> config.modeB
            isModeB && !isModeA -> config.modeA
            else -> config.getNextMode()
        }
    }
}

/**
 * Use case for getting current network mode
 */
class GetCurrentNetworkModeUseCase @Inject constructor(
    private val networkControlRepository: NetworkControlRepository
) {
    suspend operator fun invoke(subId: Int): Result<NetworkMode?> {
        return try {
            Result.success(networkControlRepository.getCurrentNetworkMode(subId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for updating control method preference
 */
class UpdateControlMethodUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(method: ControlMethod) {
        preferencesRepository.setControlMethod(method)
    }
}

/**
 * Use case for getting toggle mode configuration
 */
class GetToggleModeConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): ToggleModeConfig {
        return preferencesRepository.getToggleModeConfig()
    }
}

/**
 * Use case for updating toggle mode configuration
 */
class UpdateToggleModeConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(config: ToggleModeConfig) {
        preferencesRepository.setToggleModeConfig(config)
    }
}

/**
 * Use case for getting widget customization configuration
 */
class GetWidgetCustomizationConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): WidgetCustomizationConfig {
        return preferencesRepository.getWidgetCustomizationConfig()
    }
}

/**
 * Use case for updating widget customization configuration
 */
class UpdateWidgetCustomizationConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(config: WidgetCustomizationConfig) {
        preferencesRepository.setWidgetCustomizationConfig(config)
    }
}
