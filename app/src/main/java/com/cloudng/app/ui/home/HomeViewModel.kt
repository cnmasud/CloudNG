package com.cloudng.app.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudng.app.core.CoreBridge
import com.cloudng.app.core.CoreState
import com.cloudng.app.data.model.Profile
import com.cloudng.app.data.model.TrafficStats
import com.cloudng.app.data.repository.ProfileRepository
import com.cloudng.app.data.repository.SettingsRepository
import com.cloudng.app.service.CloudVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val coreState: CoreState = CoreState.IDLE,
    val selectedProfile: Profile? = null,
    val traffic: TrafficStats = TrafficStats.EMPTY,
    val latencyMs: Long = -1L,
    val errorMessage: String? = null,
    val profiles: List<Profile> = emptyList()
)

sealed class HomeEvent {
    data class StartVpn(val profileId: String) : HomeEvent()
    object StopVpn : HomeEvent()
    data class SelectProfile(val profile: Profile) : HomeEvent()
    object TestLatency : HomeEvent()
    object DismissError : HomeEvent()
    object NoProfileSelected : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val coreBridge: CoreBridge,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                coreBridge.observeState(),
                profileRepository.observeAll(),
                settingsRepository.appSettings
            ) { state, profiles, settings ->
                val selected = settings.lastSelectedProfileId?.let { id ->
                    profiles.find { it.id == id }
                } ?: profiles.firstOrNull()
                Triple(state, profiles, selected)
            }.collect { (state, profiles, selected) ->
                _uiState.update { it.copy(coreState = state, profiles = profiles, selectedProfile = selected) }
            }
        }
        viewModelScope.launch {
            coreBridge.observeTraffic().collect { traffic ->
                _uiState.update { it.copy(traffic = traffic) }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SelectProfile -> {
                viewModelScope.launch {
                    settingsRepository.setSelectedProfile(event.profile.id)
                }
            }
            is HomeEvent.StartVpn -> {
                Intent(context, CloudVpnService::class.java).also {
                    it.action = CloudVpnService.ACTION_START
                    it.putExtra(CloudVpnService.EXTRA_PROFILE_ID, event.profileId)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(it)
                    } else {
                        context.startService(it)
                    }
                }
            }
            HomeEvent.StopVpn -> {
                Intent(context, CloudVpnService::class.java).also {
                    it.action = CloudVpnService.ACTION_STOP
                    context.startService(it)
                }
            }
            HomeEvent.NoProfileSelected -> {
                _uiState.update { it.copy(errorMessage = "No profile selected. Go to Profiles and select one.") }
            }
            HomeEvent.TestLatency -> {
                viewModelScope.launch {
                    val profile = _uiState.value.selectedProfile ?: return@launch
                    val result = coreBridge.ping(profile)
                    profileRepository.updateLatency(profile.id, result.latencyMs)
                    _uiState.update { it.copy(latencyMs = result.latencyMs) }
                }
            }
            HomeEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }
}
