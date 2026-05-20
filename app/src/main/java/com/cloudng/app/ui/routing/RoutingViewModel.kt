package com.cloudng.app.ui.routing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudng.app.data.db.RoutingRuleDao
import com.cloudng.app.data.model.*
import com.cloudng.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutingUiState(
    val routingConfig: RoutingConfig = RoutingConfig(),
    val customRules: List<RoutingRule> = emptyList(),
    val message: String? = null
)

sealed class RoutingEvent {
    data class SetMode(val mode: RoutingMode) : RoutingEvent()
    data class AddRule(val rule: RoutingRule) : RoutingEvent()
    data class DeleteRule(val rule: RoutingRule) : RoutingEvent()
    data class ToggleRule(val rule: RoutingRule) : RoutingEvent()
    data class SetPerAppMode(val mode: PerAppProxyMode) : RoutingEvent()
    data class UpdateBypassedApps(val packages: List<String>) : RoutingEvent()
    data class UpdateProxiedApps(val packages: List<String>) : RoutingEvent()
    object DismissMessage : RoutingEvent()
}

@HiltViewModel
class RoutingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val routingRuleDao: RoutingRuleDao
) : ViewModel() {

    val uiState: StateFlow<RoutingUiState> = combine(
        settingsRepository.routingConfig,
        routingRuleDao.observeAll()
    ) { config, rules ->
        RoutingUiState(routingConfig = config, customRules = rules)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoutingUiState())

    fun onEvent(event: RoutingEvent) {
        when (event) {
            is RoutingEvent.SetMode -> viewModelScope.launch {
                val current = settingsRepository.routingConfig.first()
                settingsRepository.updateRoutingConfig(current.copy(mode = event.mode))
            }
            is RoutingEvent.AddRule -> viewModelScope.launch {
                routingRuleDao.upsert(event.rule)
            }
            is RoutingEvent.DeleteRule -> viewModelScope.launch {
                routingRuleDao.delete(event.rule)
            }
            is RoutingEvent.ToggleRule -> viewModelScope.launch {
                routingRuleDao.upsert(event.rule.copy(enabled = !event.rule.enabled))
            }
            is RoutingEvent.SetPerAppMode -> viewModelScope.launch {
                val current = settingsRepository.routingConfig.first()
                settingsRepository.updateRoutingConfig(current.copy(perAppMode = event.mode))
            }
            is RoutingEvent.UpdateBypassedApps -> viewModelScope.launch {
                val current = settingsRepository.routingConfig.first()
                settingsRepository.updateRoutingConfig(current.copy(bypassedApps = event.packages))
            }
            is RoutingEvent.UpdateProxiedApps -> viewModelScope.launch {
                val current = settingsRepository.routingConfig.first()
                settingsRepository.updateRoutingConfig(current.copy(proxiedApps = event.packages))
            }
            RoutingEvent.DismissMessage -> {}
        }
    }
}
