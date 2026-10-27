package com.markvoronin.networkswitch.domain.usecase

import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.repository.NetworkControlRepository
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ToggleNetworkModeUseCase with alternating logic
 */
class ToggleNetworkModeUseCaseTest {
    
    private lateinit var networkControlRepository: NetworkControlRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: ToggleNetworkModeUseCase
    
    private val testSubId = 1
    
    @Before
    fun setUp() {
        networkControlRepository = mockk()
        preferencesRepository = mockk()
        useCase = ToggleNetworkModeUseCase(networkControlRepository, preferencesRepository)
        // By default the current mode can't be read, so the stored flag decides
        coEvery { networkControlRepository.getCurrentNetworkMode(any()) } returns null
    }
    
    @Test
    fun `should switch to mode B when nextModeIsB is true`() = runTest {
        // Given
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.success(Unit)
        
        val updatedConfig = config.toggle() // After successful switch, toggle the state
        coEvery { preferencesRepository.setToggleModeConfig(updatedConfig) } returns Unit
        
        // When
        val result = useCase(testSubId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(NetworkMode.NR_ONLY, result.getOrNull())
        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) }
        coVerify { preferencesRepository.setToggleModeConfig(updatedConfig) }
    }
    
    @Test
    fun `should switch to mode A when nextModeIsB is false`() = runTest {
        // Given
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = false)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)
        
        val updatedConfig = config.toggle() // After successful switch, toggle the state
        coEvery { preferencesRepository.setToggleModeConfig(updatedConfig) } returns Unit
        
        // When
        val result = useCase(testSubId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(NetworkMode.LTE_ONLY, result.getOrNull())
        coVerify { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) }
        coVerify { preferencesRepository.setToggleModeConfig(updatedConfig) }
    }
    
    @Test
    fun `should handle alternating sequence correctly`() = runTest {
        // Given
        val initialConfig = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        coEvery { preferencesRepository.getToggleModeConfig() } returns initialConfig
        
        // First call should switch to NR_ONLY
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.success(Unit)
        val afterFirstToggle = initialConfig.toggle()
        coEvery { preferencesRepository.setToggleModeConfig(afterFirstToggle) } returns Unit
        
        // When
        val firstResult = useCase(testSubId)
        
        // Then
        assertTrue(firstResult.isSuccess)
        assertEquals(NetworkMode.NR_ONLY, firstResult.getOrNull())
        
        // The next call should switch to LTE_ONLY
        coEvery { preferencesRepository.getToggleModeConfig() } returns afterFirstToggle
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)
        val afterSecondToggle = afterFirstToggle.toggle()
        coEvery { preferencesRepository.setToggleModeConfig(afterSecondToggle) } returns Unit
        
        val secondResult = useCase(testSubId)
        
        assertTrue(secondResult.isSuccess)
        assertEquals(NetworkMode.LTE_ONLY, secondResult.getOrNull())
    }
    
    @Test
    fun `should return failure when setNetworkMode fails and not update config`() = runTest {
        // Given
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        
        val exception = RuntimeException("Network mode not supported")
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.failure(exception)
        
        // When
        val result = useCase(testSubId)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        // Config should not be updated when network mode change fails
        coVerify(exactly = 0) { preferencesRepository.setToggleModeConfig(any()) }
    }
    
    @Test
    fun `should handle repository exception`() = runTest {
        // Given
        coEvery { preferencesRepository.getToggleModeConfig() } throws RuntimeException("Repository error")
        
        // When
        val result = useCase(testSubId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `should follow the real mode when the stored flag is out of sync`() = runTest {
        // Flag says "next is B", but the device is already on B
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.NR_ONLY
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)
        coEvery { preferencesRepository.setToggleModeConfig(any()) } returns Unit

        val result = useCase(testSubId)

        assertEquals(NetworkMode.LTE_ONLY, result.getOrNull())
        // Back on A, so the next switch goes to B
        coVerify { preferencesRepository.setToggleModeConfig(config.copy(nextModeIsB = true)) }
    }

    @Test
    fun `should switch to mode B when the device is on mode A`() = runTest {
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = false)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.LTE_ONLY
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.success(Unit)
        coEvery { preferencesRepository.setToggleModeConfig(any()) } returns Unit

        val result = useCase(testSubId)

        assertEquals(NetworkMode.NR_ONLY, result.getOrNull())
        coVerify { preferencesRepository.setToggleModeConfig(config.copy(nextModeIsB = false)) }
    }

    @Test
    fun `should fall back to the stored flag when the device is on neither mode`() = runTest {
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY, nextModeIsB = true)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.GSM_ONLY
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.success(Unit)
        coEvery { preferencesRepository.setToggleModeConfig(any()) } returns Unit

        assertEquals(NetworkMode.NR_ONLY, useCase(testSubId).getOrNull())
    }

    @Test
    fun `should treat a mode with identical technologies as the configured mode`() = runTest {
        // The radio can't tell "3G preferred" from "Auto"; the device reports WCDMA_PREF
        val config = ToggleModeConfig(NetworkMode.GSM_UMTS, NetworkMode.LTE_ONLY, nextModeIsB = false)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.WCDMA_PREF
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)
        coEvery { preferencesRepository.setToggleModeConfig(any()) } returns Unit

        // Flag says "next is A", but the device is on A, so it switches to B
        assertEquals(NetworkMode.LTE_ONLY, useCase(testSubId).getOrNull())
    }

    @Test
    fun `should fall back to the stored flag when both modes allow the same technologies`() = runTest {
        val config = ToggleModeConfig(NetworkMode.WCDMA_PREF, NetworkMode.GSM_UMTS, nextModeIsB = true)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { networkControlRepository.getCurrentNetworkMode(testSubId) } returns NetworkMode.WCDMA_PREF
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.GSM_UMTS) } returns Result.success(Unit)
        coEvery { preferencesRepository.setToggleModeConfig(any()) } returns Unit

        assertEquals(NetworkMode.GSM_UMTS, useCase(testSubId).getOrNull())
    }
}
