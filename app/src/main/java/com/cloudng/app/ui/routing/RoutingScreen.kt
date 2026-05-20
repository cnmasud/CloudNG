package com.cloudng.app.ui.routing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudng.app.data.model.*
import com.cloudng.app.ui.components.CloudNGTopBar
import com.cloudng.app.ui.components.SectionHeader

@Composable
fun RoutingScreen(
    onNavigateToPerApp: () -> Unit,
    onBack: () -> Unit,
    viewModel: RoutingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddRuleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { CloudNGTopBar("Routing Rules", onBack = onBack) },
        floatingActionButton = {
            if (uiState.routingConfig.mode == RoutingMode.CUSTOM) {
                FloatingActionButton(onClick = { showAddRuleDialog = true }) {
                    Icon(Icons.Default.Add, "Add Rule")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 80.dp
            )
        ) {
            item {
                SectionHeader("Mode")
                RoutingModeSelector(
                    selected = uiState.routingConfig.mode,
                    onSelect = { viewModel.onEvent(RoutingEvent.SetMode(it)) }
                )
            }

            item {
                SectionHeader("Per-App Proxy")
                ListItem(
                    headlineContent = { Text("Per-App Proxy") },
                    supportingContent = {
                        Text(uiState.routingConfig.perAppMode.name.replace('_', ' '))
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, null)
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable { onNavigateToPerApp() }
                )
            }

            if (uiState.routingConfig.mode == RoutingMode.CUSTOM) {
                item { SectionHeader("Custom Rules (${uiState.customRules.size})") }
                if (uiState.customRules.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No custom rules. Tap + to add.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    items(uiState.customRules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onToggle = { viewModel.onEvent(RoutingEvent.ToggleRule(rule)) },
                            onDelete = { viewModel.onEvent(RoutingEvent.DeleteRule(rule)) }
                        )
                    }
                }
            }
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onAdd = { rule ->
                viewModel.onEvent(RoutingEvent.AddRule(rule))
                showAddRuleDialog = false
            }
        )
    }
}

@Composable
private fun RoutingModeSelector(selected: RoutingMode, onSelect: (RoutingMode) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RoutingMode.entries.forEach { mode ->
            val label = when (mode) {
                RoutingMode.GLOBAL_PROXY -> "Global Proxy"
                RoutingMode.BYPASS_LAN -> "Bypass LAN"
                RoutingMode.BYPASS_MAINLAND -> "Bypass Mainland"
                RoutingMode.CUSTOM -> "Custom Rules"
            }
            val desc = when (mode) {
                RoutingMode.GLOBAL_PROXY -> "Route all traffic through proxy"
                RoutingMode.BYPASS_LAN -> "Direct for LAN, proxy for rest"
                RoutingMode.BYPASS_MAINLAND -> "Direct for mainland IPs/domains"
                RoutingMode.CUSTOM -> "Define your own rules"
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == mode)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                onClick = { onSelect(mode) }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == mode, onClick = { onSelect(mode) })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(desc, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleCard(rule: RoutingRule, onToggle: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(rule.value) },
        supportingContent = {
            Text("${rule.type.name} → ${rule.action.name}",
                style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            Row {
                Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                }
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun AddRuleDialog(onDismiss: () -> Unit, onAdd: (RoutingRule) -> Unit) {
    var value by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RuleType.DOMAIN) }
    var action by remember { mutableStateOf(RuleAction.PROXY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Routing Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = value, onValueChange = { value = it },
                    label = { Text("Value (e.g. google.com, 8.8.8.0/24)") },
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleType.entries.take(4).forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t },
                            label = { Text(t.name, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleAction.entries.forEach { a ->
                        FilterChip(selected = action == a, onClick = { action = a },
                            label = { Text(a.name) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (value.isNotBlank()) onAdd(RoutingRule(value = value, type = type, action = action)) },
                enabled = value.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
