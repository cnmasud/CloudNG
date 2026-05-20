package com.cloudng.app.ui.routing

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudng.app.data.model.PerAppProxyMode
import com.cloudng.app.ui.components.CloudNGTopBar
import com.google.accompanist.drawablepainter.DrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppProxyScreen(
    onBack: () -> Unit,
    viewModel: RoutingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { info ->
                    AppInfo(
                        packageName = info.packageName,
                        label = pm.getApplicationLabel(info).toString(),
                        icon = runCatching { pm.getApplicationIcon(info.packageName) }.getOrNull()
                    )
                }
                .sortedBy { it.label }
        }
        loading = false
    }

    val mode = uiState.routingConfig.perAppMode
    val selectedPackages = when (mode) {
        PerAppProxyMode.BYPASS_SELECTED -> uiState.routingConfig.bypassedApps.toSet()
        PerAppProxyMode.PROXY_ONLY_SELECTED -> uiState.routingConfig.proxiedApps.toSet()
        PerAppProxyMode.DISABLED -> emptySet()
    }

    val filtered = apps.filter {
        query.isBlank() || it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = { CloudNGTopBar("Per-App Proxy", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mode", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerAppProxyMode.entries.forEach { m ->
                        val label = when (m) {
                            PerAppProxyMode.DISABLED -> "Disabled"
                            PerAppProxyMode.BYPASS_SELECTED -> "Bypass selected"
                            PerAppProxyMode.PROXY_ONLY_SELECTED -> "Proxy only selected"
                        }
                        FilterChip(
                            selected = mode == m,
                            onClick = { viewModel.onEvent(RoutingEvent.SetPerAppMode(m)) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            if (mode != PerAppProxyMode.DISABLED) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn {
                        items(filtered, key = { it.packageName }) { app ->
                            val checked = app.packageName in selectedPackages
                            ListItem(
                                headlineContent = { Text(app.label) },
                                supportingContent = {
                                    Text(app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                },
                                leadingContent = {
                                    app.icon?.let {
                                        Image(
                                            painter = DrawablePainter(it),
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                },
                                trailingContent = {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { nowChecked ->
                                            val updated = if (nowChecked)
                                                selectedPackages + app.packageName
                                            else
                                                selectedPackages - app.packageName
                                            when (mode) {
                                                PerAppProxyMode.BYPASS_SELECTED ->
                                                    viewModel.onEvent(RoutingEvent.UpdateBypassedApps(updated.toList()))
                                                PerAppProxyMode.PROXY_ONLY_SELECTED ->
                                                    viewModel.onEvent(RoutingEvent.UpdateProxiedApps(updated.toList()))
                                                PerAppProxyMode.DISABLED -> {}
                                            }
                                        }
                                    )
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Enable a per-app mode above to select apps.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}
