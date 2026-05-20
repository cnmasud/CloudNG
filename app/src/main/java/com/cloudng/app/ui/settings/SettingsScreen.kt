package com.cloudng.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudng.app.data.model.AppSettings
import com.cloudng.app.data.model.AppTheme
import com.cloudng.app.data.model.LogLevel
import com.cloudng.app.ui.components.CloudNGTopBar
import com.cloudng.app.ui.components.InfoRow
import com.cloudng.app.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToDns: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var settings by remember(uiState.settings) { mutableStateOf(uiState.settings) }

    fun save() = viewModel.onEvent(SettingsEvent.UpdateSettings(settings))

    Scaffold(
        topBar = { CloudNGTopBar("Settings", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("General")
            SwitchSetting(
                title = "Auto-start on Boot",
                subtitle = "Reconnect automatically after reboot",
                checked = settings.autoStartOnBoot,
                onCheckedChange = { settings = settings.copy(autoStartOnBoot = it); save() }
            )
            SwitchSetting(
                title = "Kill Switch",
                subtitle = "Block traffic when VPN is not active",
                checked = settings.killSwitch,
                onCheckedChange = { settings = settings.copy(killSwitch = it); save() }
            )
            SwitchSetting(
                title = "Allow LAN Access",
                subtitle = "Bypass VPN for local network devices",
                checked = settings.allowLan,
                onCheckedChange = { settings = settings.copy(allowLan = it); save() }
            )

            SectionHeader("Ports")
            PortSetting(
                label = "SOCKS Port",
                value = settings.socksPort.toString(),
                onChange = { it.toIntOrNull()?.let { p -> settings = settings.copy(socksPort = p); save() } }
            )
            PortSetting(
                label = "HTTP Port",
                value = settings.httpPort.toString(),
                onChange = { it.toIntOrNull()?.let { p -> settings = settings.copy(httpPort = p); save() } }
            )
            PortSetting(
                label = "MTU",
                value = settings.mtu.toString(),
                onChange = { it.toIntOrNull()?.let { m -> settings = settings.copy(mtu = m); save() } }
            )

            SectionHeader("Appearance")
            DropdownSetting(
                label = "Theme",
                options = AppTheme.entries.map { it.name },
                selected = settings.theme.name,
                onSelect = {
                    settings = settings.copy(theme = AppTheme.valueOf(it)); save()
                }
            )

            SectionHeader("Diagnostics")
            DropdownSetting(
                label = "Log Level",
                options = LogLevel.entries.map { it.name },
                selected = settings.logLevel.name,
                onSelect = {
                    settings = settings.copy(logLevel = LogLevel.valueOf(it)); save()
                }
            )
            OutlinedButton(
                onClick = { onNavigateToLogs() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Article, null)
                Spacer(Modifier.width(8.dp))
                Text("View Logs")
            }
            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsEvent.ExportDiagnostics) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Export Diagnostics Bundle")
            }

            SectionHeader("Advanced")
            SwitchSetting(
                title = "Safe Mode",
                subtitle = "Use minimal config for troubleshooting",
                checked = settings.safeMode,
                onCheckedChange = { settings = settings.copy(safeMode = it); save() }
            )
            SwitchSetting(
                title = "Telemetry",
                subtitle = "Anonymous crash reports (opt-in)",
                checked = settings.telemetryEnabled,
                onCheckedChange = { settings = settings.copy(telemetryEnabled = it); save() }
            )

            SectionHeader("DNS")
            OutlinedButton(
                onClick = onNavigateToDns,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Dns, null)
                Spacer(Modifier.width(8.dp))
                Text("DNS Settings")
            }

            SectionHeader("About")
            InfoRow("App Version", "1.0.0")
            InfoRow("Core Version", uiState.coreVersion)
            Spacer(Modifier.height(32.dp))
        }
    }

    uiState.message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.onEvent(SettingsEvent.DismissMessage)
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun PortSetting(label: String, value: String, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}
