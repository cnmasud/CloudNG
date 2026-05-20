package com.cloudng.app.ui.home

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudng.app.core.CoreState
import com.cloudng.app.data.model.Profile
import com.cloudng.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfiles: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            uiState.selectedProfile?.let { profile ->
                viewModel.onEvent(HomeEvent.StartVpn(profile.id))
            }
        }
    }

    fun requestVpnAndStart() {
        val profile = uiState.selectedProfile
        if (profile == null) {
            viewModel.onEvent(HomeEvent.NoProfileSelected)
            return
        }
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) {
            vpnLauncher.launch(vpnIntent)
        } else {
            viewModel.onEvent(HomeEvent.StartVpn(profile.id))
        }
    }

    val isRunning = uiState.coreState == CoreState.RUNNING

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Cloud,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "CloudNG",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(HomeEvent.TestLatency) }) {
                            Icon(Icons.Default.NetworkCheck, "Test Latency")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                ConnectButton(
                    state = uiState.coreState,
                    onClick = {
                        when (uiState.coreState) {
                            CoreState.IDLE, CoreState.ERROR -> requestVpnAndStart()
                            CoreState.RUNNING -> viewModel.onEvent(HomeEvent.StopVpn)
                            else -> {}
                        }
                    }
                )

                Spacer(Modifier.height(20.dp))
                StatusBadge(state = uiState.coreState)
                Spacer(Modifier.height(28.dp))

                ProfileSelector(
                    selectedProfile = uiState.selectedProfile,
                    profiles = uiState.profiles,
                    onSelect = { viewModel.onEvent(HomeEvent.SelectProfile(it)) },
                    onManage = onNavigateToProfiles
                )

                if (isRunning) {
                    Spacer(Modifier.height(16.dp))
                    TrafficCard(stats = uiState.traffic)
                }

                uiState.selectedProfile?.latencyMs?.let { ms ->
                    if (ms >= 0) {
                        Spacer(Modifier.height(16.dp))
                        LatencyChip(ms)
                    }
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }

    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HomeEvent.DismissError) },
            title = { Text("Connection Error") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HomeEvent.DismissError) }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ConnectButton(state: CoreState, onClick: () -> Unit) {
    val statusColor = when (state) {
        CoreState.RUNNING -> StatusConnected
        CoreState.STARTING, CoreState.STOPPING -> StatusConnecting
        CoreState.ERROR -> StatusError
        CoreState.IDLE -> StatusDisconnected
    }
    val animatedColor by animateColorAsState(statusColor, animationSpec = tween(500), label = "btnColor")
    val isTransitioning = state == CoreState.STARTING || state == CoreState.STOPPING

    val infiniteTransition = rememberInfiniteTransition(label = "buttonAnim")

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseOut), RepeatMode.Restart),
        label = "rippleAlpha"
    )
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseOut), RepeatMode.Restart),
        label = "rippleScale"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isTransitioning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state == CoreState.RUNNING) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(rippleScale)
                    .clip(CircleShape)
                    .background(animatedColor.copy(alpha = rippleAlpha * 0.3f))
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(rippleScale * 0.85f)
                    .clip(CircleShape)
                    .background(animatedColor.copy(alpha = rippleAlpha * 0.2f))
            )
        }

        Box(
            modifier = Modifier
                .size(156.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor.copy(alpha = 0.25f),
                            animatedColor.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(1.5.dp, animatedColor.copy(alpha = 0.4f), CircleShape)
                .clickable(
                    enabled = !isTransitioning,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedColor,
                                animatedColor.copy(alpha = 0.75f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isTransitioning) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (state == CoreState.RUNNING) Icons.Default.Stop else Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(state: CoreState) {
    val label = when (state) {
        CoreState.RUNNING -> "● Connected"
        CoreState.STARTING -> "◌ Connecting…"
        CoreState.STOPPING -> "◌ Disconnecting…"
        CoreState.ERROR -> "✕ Error"
        CoreState.IDLE -> "○ Disconnected"
    }
    val color = when (state) {
        CoreState.RUNNING -> StatusConnected
        CoreState.STARTING, CoreState.STOPPING -> StatusConnecting
        CoreState.ERROR -> StatusError
        CoreState.IDLE -> StatusDisconnected
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(color.copy(0.3f), color.copy(0.1f)))
        )
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ProfileSelector(
    selectedProfile: Profile?,
    profiles: List<Profile>,
    onSelect: (Profile) -> Unit,
    onManage: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (profiles.isNotEmpty()) expanded = true else onManage() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Cloud, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = selectedProfile?.name?.ifBlank { selectedProfile.address } ?: "No profile selected",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                if (selectedProfile != null) {
                    Text(
                        text = "${selectedProfile.protocol.name} · ${selectedProfile.address}:${selectedProfile.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                } else {
                    Text(
                        "Tap to select a profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            Icon(
                Icons.Default.ExpandMore, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                profile.name.ifBlank { profile.address },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${profile.protocol.name} · ${profile.address}:${profile.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    },
                    onClick = { onSelect(profile); expanded = false }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Manage profiles…", color = MaterialTheme.colorScheme.primary) },
                onClick = { expanded = false; onManage() },
                leadingIcon = {
                    Icon(Icons.Default.ManageAccounts, null, tint = MaterialTheme.colorScheme.primary)
                }
            )
        }
    }
}

@Composable
private fun TrafficCard(stats: com.cloudng.app.data.model.TrafficStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrafficItem(
                icon = Icons.Default.ArrowUpward,
                label = "Upload",
                speed = formatBytes(stats.uploadSpeed) + "/s",
                total = formatBytes(stats.uploadBytes),
                color = TrafficUp
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(52.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )
            TrafficItem(
                icon = Icons.Default.ArrowDownward,
                label = "Download",
                speed = formatBytes(stats.downloadSpeed) + "/s",
                total = formatBytes(stats.downloadBytes),
                color = TrafficDown
            )
        }
    }
}

@Composable
private fun TrafficItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    speed: String,
    total: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Text(speed, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = color)
        Text(
            total,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun LatencyChip(ms: Long) {
    val color = when {
        ms < 150 -> StatusConnected
        ms < 400 -> StatusConnecting
        else -> StatusError
    }
    val label = when {
        ms < 150 -> "Excellent"
        ms < 400 -> "Good"
        else -> "Poor"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.1f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(color.copy(0.4f), color.copy(0.1f)))
        )
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.SignalCellularAlt, null, tint = color, modifier = Modifier.size(14.dp))
            Text(
                "${ms}ms · $label",
                color = color,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576     -> "%.2f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
        else                   -> "$bytes B"
    }
}
