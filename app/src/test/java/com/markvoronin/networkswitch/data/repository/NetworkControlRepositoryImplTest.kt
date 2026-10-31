package com.markvoronin.networkswitch.data.repository

import com.markvoronin.networkswitch.data.source.RootNetworkControlDataSource
import com.markvoronin.networkswitch.data.source.ShizukuNetworkControlDataSource
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class NetworkControlRepositoryImplTest {

    private lateinit var rootDataSource: RootNetworkControlDataSource
    private lateinit var shizukuDataSource: ShizukuNetworkControlDataSource
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var repository: NetworkControlRepositoryImpl

    private val subId = 1

    @Before
    fun setUp() {
        rootDataSource = mockk(relaxed = true)
        shizukuDataSource = mockk(relaxed = true)
        preferencesRepository = mockk()
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.SHIZUKU
        coEvery { preferencesRepository.getToggleModeConfig() } returns
            ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_LTE)
        repository = NetworkControlRepositoryImpl(
            rootDataSource,
            shizukuDataSource,
            preferencesRepository,
            UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `setNetworkMode uses the selected control method`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.ROOT

        val result = repository.setNetworkMode(subId, NetworkMode.NR_LTE)

        assertTrue(result.isSuccess)
        coVerify { rootDataSource.setNetworkMode(subId, NetworkMode.NR_LTE) }
        coVerify(exactly = 0) { shizukuDataSource.setNetworkMode(any(), any()) }
    }

    @Test
    fun `setNetworkMode returns failure when the data source throws`() = runTest {
        val error = IllegalStateException("The device rejected network mode")
        coEvery { shizukuDataSource.setNetworkMode(subId, NetworkMode.NR_LTE) } throws error

        val result = repository.setNetworkMode(subId, NetworkMode.NR_LTE)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `reconnectNetwork uses the selected control method`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.ROOT

        val result = repository.reconnectNetwork()

        assertTrue(result.isSuccess)
        coVerify { rootDataSource.reconnectNetwork() }
        coVerify(exactly = 0) { shizukuDataSource.reconnectNetwork() }
    }

    @Test
    fun `reconnectNetwork returns failure when the data source throws`() = runTest {
        val error = IllegalStateException("The device could not reconnect the network")
        coEvery { shizukuDataSource.reconnectNetwork() } throws error

        val result = repository.reconnectNetwork()

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `getCurrentNetworkMode returns the detected mode`() = runTest {
        coEvery { shizukuDataSource.getCurrentNetworkMode(subId) } returns NetworkMode.NR_LTE

        assertEquals(NetworkMode.NR_LTE, repository.getCurrentNetworkMode(subId))
    }

    @Test
    fun `getCurrentNetworkMode returns null when detection fails`() = runTest {
        coEvery { shizukuDataSource.getCurrentNetworkMode(subId) } returns null

        assertNull(repository.getCurrentNetworkMode(subId))
    }

    @Test
    fun `getCurrentNetworkMode returns null when the data source throws`() = runTest {
        coEvery { shizukuDataSource.getCurrentNetworkMode(subId) } throws RuntimeException("boom")

        assertNull(repository.getCurrentNetworkMode(subId))
    }

    @Test
    fun `getCurrentNetworkMode prefers the configured mode that is identical at the radio level`() = runTest {
        // The radio can't distinguish "3G preferred" from "Auto"; the user chose GSM_UMTS
        coEvery { preferencesRepository.getToggleModeConfig() } returns
            ToggleModeConfig(NetworkMode.GSM_UMTS, NetworkMode.LTE_ONLY)
        coEvery { shizukuDataSource.getCurrentNetworkMode(subId) } returns NetworkMode.WCDMA_PREF

        assertEquals(NetworkMode.GSM_UMTS, repository.getCurrentNetworkMode(subId))
    }

    @Test
    fun `resetConnections resets both data sources`() = runTest {
        repository.resetConnections()

        verify { rootDataSource.resetConnection() }
        verify { shizukuDataSource.resetConnection() }
    }
}
