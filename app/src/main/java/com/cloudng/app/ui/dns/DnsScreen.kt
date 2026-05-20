package com.cloudng.app.ui.dns

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudng.app.data.model.DnsConfig
import com.cloudng.app.data.model.DnsMode
import com.cloudng.app.ui.components.CloudNGTopBar
import com.cloudng.app.ui.components.SectionHeader
import com.cloudng.app.ui.settings.SettingsEvent
import com.cloudng.app.ui.settings.SettingsViewModel

@Composable
fun DnsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var config by remember(uiState.dnsConfig) { mutableStateOf(uiState.dnsConfig) }
    var isDirty by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CloudNGTopBar(
                title = "DNS Settings",
                onBack = onBack,
                actions = {
                    if (isDirty) {
                        TextButton(onClick = {
                            viewModel.onEvent(SettingsEvent.UpdateDns(config))
                            isDirty = false
                        }) { Text("Save") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("DNS Mode")
            DnsMode.entries.forEach { mode ->
                val label = when (mode) {
                    DnsMode.SYSTEM -> "System DNS"
                    DnsMode.DOH -> "DNS over HTTPS (DoH)"
                    DnsMode.DOT -> "DNS over TLS (DoT)"
                    DnsMode.FAKE_DNS -> "Fake DNS (requires core support)"
                }
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = {
                        RadioButton(
                            selected = config.mode == mode,
                            onClick = { config = config.copy(mode = mode); isDirty = true }
                        )
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (config.mode != DnsMode.SYSTEM) {
                SectionHeader("Remote DNS Server")
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.remoteDns,
                        onValueChange = { config = config.copy(remoteDns = it); isDirty = true },
                        label = { Text("Remote DNS (DoH/DoT URL or IP)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://1.1.1.1/dns-query") }
                    )
                    OutlinedTextField(
                        value = config.domesticDns,
                        onValueChange = { config = config.copy(domesticDns = it); isDirty = true },
                        label = { Text("Domestic/Fallback DNS") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("223.5.5.5") }
                    )
                }
            }

            SectionHeader("Options")
            Column(Modifier.padding(horizontal = 8.dp)) {
                ListItem(
                    headlineContent = { Text("Enable Fake DNS") },
                    supportingContent = { Text("Intercept DNS queries (core must support)") },
                    trailingContent = {
                        Switch(
                            checked = config.enableFakeDns,
                            onCheckedChange = {
                                config = config.copy(enableFakeDns = it)
                                isDirty = true
                            }
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text("DNS Cache") },
                    supportingContent = { Text("Cache DNS responses for performance") },
                    trailingContent = {
                        Switch(
                            checked = config.enableDnsCache,
                            onCheckedChange = {
                                config = config.copy(enableDnsCache = it)
                                isDirty = true
                            }
                        )
                    }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
