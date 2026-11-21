package com.markvoronin.networkswitch.domain.usecase

import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.repository.NetworkControlRepository
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SetNetworkModeUseCaseTest {

    private lateinit var networkControlRepository: NetworkControlRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: SetNetworkModeUseCase

    private val testSubId = 1
    private val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_LTE, nextModeIsB = true)

    @Before
    fun setUp() {
        networkControlRepository = mockk()
        preferencesRepository = mockk()
        useCase = SetNetworkModeUseCase(networkControlRepository, preferencesRepository)
        coEvery { preferencesRepository.getToggleModeConfig() } returns config
        coEvery { preferencesRepository.setToggleModeConfig(any()) } returns Unit
    }

    @Test
    fun `setting mode B makes mode A the next toggle target`() = runTest {
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_LTE) } returns Result.success(Unit)

        val result = useCase(testSubId, NetworkMode.NR_LTE)

        assertTrue(result.isSuccess)
        coVerify { preferencesRepository.setToggleModeConfig(config.copy(nextModeIsB = false)) }
    }

    @Test
    fun `setting a mode already matching the stored state leaves the toggle config alone`() = runTest {
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.LTE_ONLY) } returns Result.success(Unit)

        useCase(testSubId, NetworkMode.LTE_ONLY)

        coVerify(exactly = 0) { preferencesRepository.setToggleModeConfig(any()) }
    }

    @Test
    fun `setting a mode outside the toggle pair leaves the toggle config alone`() = runTest {
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_ONLY) } returns Result.success(Unit)

        useCase(testSubId, NetworkMode.NR_ONLY)

        coVerify(exactly = 0) { preferencesRepository.setToggleModeConfig(any()) }
    }

    @Test
    fun `a rejected mode does not touch the toggle config`() = runTest {
        coEvery { networkControlRepository.setNetworkMode(testSubId, NetworkMode.NR_LTE) } returns
            Result.failure(IllegalStateException("rejected"))

        val result = useCase(testSubId, NetworkMode.NR_LTE)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { preferencesRepository.setToggleModeConfig(any()) }
    }
}
