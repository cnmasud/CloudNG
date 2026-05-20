package com.cloudng.app

import app.cash.turbine.test
import com.cloudng.app.core.CoreBridge
import com.cloudng.app.core.CoreState
import com.cloudng.app.core.PingResult
import com.cloudng.app.data.model.*
import com.cloudng.app.data.repository.ProfileRepository
import com.cloudng.app.data.repository.SettingsRepository
import com.cloudng.app.ui.home.HomeEvent
import com.cloudng.app.ui.home.HomeViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var coreBridge: CoreBridge
    private lateinit var profileRepository: ProfileRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: HomeViewModel

    private val stateFlow = MutableStateFlow(CoreState.IDLE)
    private val trafficFlow = MutableStateFlow(TrafficStats.EMPTY)
    private val profilesFlow = MutableStateFlow<List<Profile>>(emptyList())
    private val settingsFlow = MutableStateFlow(AppSettings())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coreBridge = mockk(relaxed = true)
        profileRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { coreBridge.observeState() } returns stateFlow
        every { coreBridge.observeTraffic() } returns trafficFlow
        every { profileRepository.observeAll() } returns profilesFlow
        every { settingsRepository.appSettings } returns settingsFlow

        viewModel = HomeViewModel(coreBridge, profileRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is IDLE with no profile`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CoreState.IDLE, state.coreState)
            assertNull(state.selectedProfile)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SelectProfile event updates selected profile id`() = runTest {
        val profile = Profile(id = "p1", name = "Test", address = "1.2.3.4", port = 443)
        coEvery { settingsRepository.setSelectedProfile("p1") } just Runs

        viewModel.onEvent(HomeEvent.SelectProfile(profile))
        advanceUntilIdle()

        coVerify { settingsRepository.setSelectedProfile("p1") }
    }

    @Test
    fun `StopVpn event calls coreBridge stop`() = runTest {
        coEvery { coreBridge.stop() } returns Result.success(Unit)

        viewModel.onEvent(HomeEvent.StopVpn)
        advanceUntilIdle()

        coVerify { coreBridge.stop() }
    }

    @Test
    fun `TestLatency event updates latency in state`() = runTest {
        val profile = Profile(id = "p1", name = "Test", address = "1.2.3.4", port = 443)
        profilesFlow.value = listOf(profile)
        settingsFlow.value = AppSettings(lastSelectedProfileId = "p1")
        advanceUntilIdle()

        coEvery { coreBridge.ping(profile) } returns PingResult("p1", 120L, true)
        coEvery { profileRepository.updateLatency("p1", 120L) } just Runs

        viewModel.onEvent(HomeEvent.TestLatency)
        advanceUntilIdle()

        assertEquals(120L, viewModel.uiState.value.latencyMs)
    }
}
