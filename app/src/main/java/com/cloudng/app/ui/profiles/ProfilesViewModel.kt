package com.cloudng.app.ui.profiles

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudng.app.core.CoreBridge
import com.cloudng.app.core.ParseResult
import com.cloudng.app.core.ProfileParser
import com.cloudng.app.data.model.Profile
import com.cloudng.app.data.repository.ProfileRepository
import com.cloudng.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val selectedProfileId: String? = null,
    val importError: String? = null,
    val importSuccess: String? = null,
    val isLoading: Boolean = false,
    val pingingProfileIds: Set<String> = emptySet()
)

sealed class ProfilesEvent {
    data class DeleteProfile(val profile: Profile) : ProfilesEvent()
    data class SelectProfile(val profile: Profile) : ProfilesEvent()
    data class ImportFromText(val text: String) : ProfilesEvent()
    object ImportFromClipboard : ProfilesEvent()
    data class SaveProfile(val profile: Profile) : ProfilesEvent()
    object DismissMessages : ProfilesEvent()
    data class PingProfile(val profile: Profile) : ProfilesEvent()
    object PingAll : ProfilesEvent()
}

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val parser: ProfileParser,
    private val coreBridge: CoreBridge,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                profileRepository.observeAll(),
                settingsRepository.appSettings
            ) { profiles, settings ->
                _uiState.update {
                    it.copy(profiles = profiles, selectedProfileId = settings.lastSelectedProfileId)
                }
            }.collect()
        }
    }

    fun onEvent(event: ProfilesEvent) {
        when (event) {
            is ProfilesEvent.DeleteProfile -> viewModelScope.launch {
                profileRepository.delete(event.profile)
            }
            is ProfilesEvent.SelectProfile -> viewModelScope.launch {
                settingsRepository.setSelectedProfile(event.profile.id)
            }
            is ProfilesEvent.ImportFromText -> importText(event.text)
            ProfilesEvent.ImportFromClipboard -> {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                if (text.isBlank()) {
                    _uiState.update { it.copy(importError = "Clipboard is empty") }
                } else {
                    importText(text)
                }
            }
            is ProfilesEvent.SaveProfile -> viewModelScope.launch {
                profileRepository.save(event.profile)
                _uiState.update { it.copy(importSuccess = "Profile saved") }
            }
            ProfilesEvent.DismissMessages -> _uiState.update {
                it.copy(importError = null, importSuccess = null)
            }
            is ProfilesEvent.PingProfile -> pingProfile(event.profile)
            ProfilesEvent.PingAll -> pingAll()
        }
    }

    private fun pingProfile(profile: Profile) {
        viewModelScope.launch {
            _uiState.update { it.copy(pingingProfileIds = it.pingingProfileIds + profile.id) }
            val result = coreBridge.ping(profile)
            profileRepository.updateLatency(profile.id, result.latencyMs)
            _uiState.update { it.copy(pingingProfileIds = it.pingingProfileIds - profile.id) }
        }
    }

    private fun pingAll() {
        val profiles = _uiState.value.profiles
        if (profiles.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(pingingProfileIds = profiles.map { p -> p.id }.toSet()) }
            profiles.forEach { profile ->
                launch {
                    val result = coreBridge.ping(profile)
                    profileRepository.updateLatency(profile.id, result.latencyMs)
                    _uiState.update { it.copy(pingingProfileIds = it.pingingProfileIds - profile.id) }
                }
            }
        }
    }

    private fun importText(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val lines = text.lines().filter { it.isNotBlank() }
            var imported = 0
            var failed = 0
            lines.forEach { line ->
                when (val result = parser.parse(line.trim())) {
                    is ParseResult.Success -> {
                        profileRepository.saveAll(result.profiles)
                        imported += result.profiles.size
                    }
                    is ParseResult.Failure -> failed++
                }
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    importSuccess = if (imported > 0) "Imported $imported profile(s)" else null,
                    importError = if (imported == 0) "Import failed: unrecognized format" else null
                )
            }
        }
    }
}
