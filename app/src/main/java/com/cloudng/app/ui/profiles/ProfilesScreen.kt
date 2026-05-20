package com.cloudng.app.ui.profiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudng.app.data.model.Profile
import com.cloudng.app.ui.components.ConfirmDeleteDialog
import com.cloudng.app.ui.components.EmptyState
import com.cloudng.app.ui.theme.StatusConnected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    onNavigateToEdit: (String?) -> Unit,
    onProfileSelected: () -> Unit = {},
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showImportSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Profile?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profiles") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ProfilesEvent.PingAll) },
                        enabled = uiState.profiles.isNotEmpty() && uiState.pingingProfileIds.isEmpty()) {
                        Icon(Icons.Default.NetworkCheck, "Ping All")
                    }
                    IconButton(onClick = { showImportSheet = true }) {
                        Icon(Icons.Default.Download, "Import")
                    }
                    IconButton(onClick = { onNavigateToEdit(null) }) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.profiles.isEmpty()) {
            EmptyState("No profiles yet.\nTap + to add one or import from clipboard / QR.",
                Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = profile.id == uiState.selectedProfileId,
                        isPinging = profile.id in uiState.pingingProfileIds,
                        onSelect = {
                            viewModel.onEvent(ProfilesEvent.SelectProfile(profile))
                            onProfileSelected()
                        },
                        onEdit = { onNavigateToEdit(profile.id) },
                        onDelete = { deleteTarget = profile },
                        onPing = { viewModel.onEvent(ProfilesEvent.PingProfile(profile)) }
                    )
                }
            }
        }
    }

    if (showImportSheet) {
        ImportBottomSheet(
            onDismiss = { showImportSheet = false },
            onImportText = { text ->
                viewModel.onEvent(ProfilesEvent.ImportFromText(text))
                showImportSheet = false
            },
            onImportClipboard = {
                viewModel.onEvent(ProfilesEvent.ImportFromClipboard)
                showImportSheet = false
            }
        )
    }

    deleteTarget?.let { profile ->
        ConfirmDeleteDialog(
            message = "Delete profile \"${profile.name}\"?",
            onConfirm = {
                viewModel.onEvent(ProfilesEvent.DeleteProfile(profile))
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    val snackMsg = uiState.importError ?: uiState.importSuccess
    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(ProfilesEvent.DismissMessages)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    isSelected: Boolean,
    isPinging: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPing: () -> Unit
) {
    val protocolColor = when (profile.protocol) {
        com.cloudng.app.data.model.Protocol.VLESS -> androidx.compose.ui.graphics.Color(0xFF7C4DFF)
        com.cloudng.app.data.model.Protocol.VMESS -> androidx.compose.ui.graphics.Color(0xFF00BCD4)
        com.cloudng.app.data.model.Protocol.TROJAN -> androidx.compose.ui.graphics.Color(0xFFFF6D00)
        com.cloudng.app.data.model.Protocol.SHADOWSOCKS -> androidx.compose.ui.graphics.Color(0xFF00C853)
        com.cloudng.app.data.model.Protocol.WIREGUARD -> androidx.compose.ui.graphics.Color(0xFF2979FF)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onSelect, onLongClick = onDelete),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(protocolColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile.protocol.name.take(2),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = protocolColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        profile.name.ifBlank { profile.address },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StatusConnected.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusConnected,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    "${profile.address}:${profile.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isPinging) {
                        val rotation by rememberInfiniteTransition(label = "ping").animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
                            label = "rot"
                        )
                        Icon(
                            Icons.Default.Sync, null,
                            modifier = Modifier.size(12.dp).graphicsLayer { rotationZ = rotation },
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Testing...", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    } else if (profile.latencyMs >= 0) {
                        val latencyColor = when {
                            profile.latencyMs < 150 -> StatusConnected
                            profile.latencyMs < 400 -> androidx.compose.ui.graphics.Color(0xFFFFAB00)
                            else -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
                        }
                        val dot = when {
                            profile.latencyMs < 150 -> "●"
                            profile.latencyMs < 400 -> "●"
                            else -> "●"
                        }
                        Text(dot, style = MaterialTheme.typography.labelSmall, color = latencyColor)
                        Text("${profile.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = latencyColor)
                    }
                }
            }
            IconButton(onClick = onPing, enabled = !isPinging, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.NetworkCheck, "Ping",
                    modifier = Modifier.size(18.dp),
                    tint = if (isPinging) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                           else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "Edit",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportBottomSheet(
    onDismiss: () -> Unit,
    onImportText: (String) -> Unit,
    onImportClipboard: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Import Profile", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Paste profile URI / base64 link") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            Button(
                onClick = { if (text.isNotBlank()) onImportText(text) },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank()
            ) { Text("Import from Text") }
            OutlinedButton(onClick = onImportClipboard, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ContentPaste, null)
                Spacer(Modifier.width(8.dp))
                Text("Import from Clipboard")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
