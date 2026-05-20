package com.cloudng.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudng.app.core.CoreBridge
import com.cloudng.app.data.model.*
import com.cloudng.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val dnsConfig: DnsConfig = DnsConfig(),
    val routingConfig: RoutingConfig = RoutingConfig(),
    val coreVersion: String = "",
    val exportPath: String? = null,
    val message: String? = null
)

sealed class SettingsEvent {
    data class UpdateSettings(val settings: AppSettings) : SettingsEvent()
    data class UpdateDns(val config: DnsConfig) : SettingsEvent()
    data class UpdateRouting(val config: RoutingConfig) : SettingsEvent()
    object SetOnboardingComplete : SettingsEvent()
    object ExportDiagnostics : SettingsEvent()
    object DismissMessage : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val coreBridge: CoreBridge,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(coreVersion = coreBridge.coreVersion()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.appSettings,
                settingsRepository.dnsConfig,
                settingsRepository.routingConfig
            ) { settings, dns, routing ->
                _uiState.update {
                    it.copy(settings = settings, dnsConfig = dns, routingConfig = routing)
                }
            }.collect()
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateSettings -> viewModelScope.launch {
                settingsRepository.updateSettings(event.settings)
            }
            is SettingsEvent.UpdateDns -> viewModelScope.launch {
                settingsRepository.updateDnsConfig(event.config)
            }
            is SettingsEvent.UpdateRouting -> viewModelScope.launch {
                settingsRepository.updateRoutingConfig(event.config)
            }
            SettingsEvent.SetOnboardingComplete -> viewModelScope.launch {
                settingsRepository.setOnboardingComplete()
            }
            SettingsEvent.ExportDiagnostics -> viewModelScope.launch {
                val file = exportDiagnosticsBundle()
                _uiState.update { it.copy(exportPath = file.absolutePath, message = "Diagnostics exported") }
            }
            SettingsEvent.DismissMessage -> _uiState.update { it.copy(message = null, exportPath = null) }
        }
    }

    private fun exportDiagnosticsBundle(): File {
        val dir = context.cacheDir
        val file = File(dir, "cloudng_diagnostics_${System.currentTimeMillis()}.txt")
        file.writeText(buildString {
            appendLine("=== CloudNG Diagnostics ===")
            appendLine("Core Version: ${coreBridge.coreVersion()}")
            appendLine("App Version: 1.0.0")
            appendLine("Core State: ${coreBridge.status().state}")
            appendLine("--- Settings (sanitized) ---")
            val s = _uiState.value.settings
            appendLine("AutoStart: ${s.autoStartOnBoot}")
            appendLine("KillSwitch: ${s.killSwitch}")
            appendLine("SafeMode: ${s.safeMode}")
            appendLine("SocksPort: ${s.socksPort}")
            appendLine("HttpPort: ${s.httpPort}")
            appendLine("MTU: ${s.mtu}")
            appendLine("LogLevel: ${s.logLevel}")
        })
        return file
    }
}
