package com.cloudng.app.ui.subscriptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudng.app.data.model.Subscription
import com.cloudng.app.data.model.UpdateInterval
import com.cloudng.app.ui.components.ConfirmDeleteDialog
import com.cloudng.app.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Subscription?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val snackMsg = uiState.errorMessage ?: uiState.successMessage
    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SubscriptionsEvent.DismissMessages)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions") },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.subscriptions.isEmpty()) {
            EmptyState("No subscriptions.\nTap + to add a subscription URL.", Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.subscriptions, key = { it.id }) { sub ->
                    SubscriptionCard(
                        subscription = sub,
                        isRefreshing = uiState.refreshingId == sub.id,
                        onRefresh = { viewModel.onEvent(SubscriptionsEvent.RefreshSubscription(sub)) },
                        onDelete = { deleteTarget = sub }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddSubscriptionSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { sub ->
                viewModel.onEvent(SubscriptionsEvent.AddSubscription(sub))
                showAddSheet = false
            }
        )
    }

    deleteTarget?.let { sub ->
        ConfirmDeleteDialog(
            message = "Delete subscription \"${sub.name}\"? All associated profiles will be removed.",
            onConfirm = {
                viewModel.onEvent(SubscriptionsEvent.DeleteSubscription(sub))
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

}

@Composable
private fun SubscriptionCard(
    subscription: Subscription,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudDownload, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        subscription.name.ifBlank { "Unnamed" },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                    Text(
                        subscription.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Refresh, "Refresh",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetaChip(
                    icon = Icons.Default.List,
                    label = "${subscription.profileCount} profiles"
                )
                MetaChip(
                    icon = Icons.Default.Schedule,
                    label = subscription.updateInterval.name.replace('_', ' ')
                )
                if (subscription.lastUpdatedAt > 0) {
                    MetaChip(
                        icon = Icons.Default.Update,
                        label = fmt.format(Date(subscription.lastUpdatedAt))
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubscriptionSheet(
    onDismiss: () -> Unit,
    onAdd: (Subscription) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf(UpdateInterval.DAILY) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Subscription", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text("Subscription URL *") },
                modifier = Modifier.fillMaxWidth(),
                isError = url.isNotBlank() && !url.startsWith("http"),
                supportingText = {
                    if (url.isNotBlank() && !url.startsWith("http")) Text("Must be http(s) URL")
                }
            )
            Text("Auto-update interval", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UpdateInterval.entries.forEach { iv ->
                    FilterChip(
                        selected = interval == iv,
                        onClick = { interval = iv },
                        label = { Text(iv.name) }
                    )
                }
            }
            Button(
                onClick = {
                    if (url.startsWith("http")) {
                        onAdd(Subscription(
                            name = name.ifBlank { url },
                            url = url,
                            updateInterval = interval
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.startsWith("http")
            ) { Text("Add Subscription") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
