package com.markvoronin.networkswitch.data.repository

import com.markvoronin.networkswitch.data.source.PreferencesDataSource
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.domain.model.ToggleModeConfig
import com.markvoronin.networkswitch.domain.repository.SurfaceRefresher
import com.markvoronin.networkswitch.domain.model.WidgetCustomizationConfig
import com.markvoronin.networkswitch.util.CoroutineTestRule
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PreferencesRepositoryImplTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var mockDataSource: PreferencesDataSource
    private lateinit var surfaceRefresher: SurfaceRefresher
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setUp() {
        mockDataSource = mockk(relaxed = true)
        surfaceRefresher = mockk(relaxed = true)
        repository = PreferencesRepositoryImpl(mockDataSource, surfaceRefresher)
    }

    @Test
    fun `getControlMethod calls data source`() = runTest {
        repository.getControlMethod()
        coVerify { mockDataSource.getControlMethod() }
    }

    @Test
    fun `setControlMethod calls data source`() = runTest {
        val method = ControlMethod.ROOT
        repository.setControlMethod(method)
        coVerify { mockDataSource.setControlMethod(method) }
    }

    @Test
    fun `observeControlMethod calls data source`() {
        repository.observeControlMethod()
        verify { mockDataSource.observeControlMethod() }
    }

    @Test
    fun `getWidgetCustomizationConfig calls data source`() = runTest {
        repository.getWidgetCustomizationConfig()
        coVerify { mockDataSource.getWidgetCustomizationConfig() }
    }

    @Test
    fun `setWidgetCustomizationConfig calls data source`() = runTest {
        val config = WidgetCustomizationConfig()
        repository.setWidgetCustomizationConfig(config)
        coVerify { mockDataSource.setWidgetCustomizationConfig(config) }
    }

    @Test
    fun `setWidgetCustomizationConfig refreshes widgets but not the tile`() = runTest {
        repository.setWidgetCustomizationConfig(WidgetCustomizationConfig())

        verify { surfaceRefresher.refreshWidgets() }
        verify(exactly = 0) { surfaceRefresher.refreshTile() }
    }

    @Test
    fun `setToggleModeConfig saves then refreshes tile and widgets`() = runTest {
        val config = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_LTE)

        repository.setToggleModeConfig(config)

        coVerify { mockDataSource.setToggleModeConfig(config) }
        verify { surfaceRefresher.refreshTile() }
        verify { surfaceRefresher.refreshWidgets() }
    }

    @Test
    fun `setControlMethod does not refresh surfaces`() = runTest {
        repository.setControlMethod(ControlMethod.ROOT)

        verify(exactly = 0) { surfaceRefresher.refreshTile() }
        verify(exactly = 0) { surfaceRefresher.refreshWidgets() }
    }

    @Test
    fun `observeWidgetCustomizationConfig calls data source`() {
        repository.observeWidgetCustomizationConfig()
        verify { mockDataSource.observeWidgetCustomizationConfig() }
    }
}
