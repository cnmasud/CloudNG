package com.cloudng.app.ui.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudng.app.data.model.*
import com.cloudng.app.ui.components.CloudNGTopBar
import java.util.UUID
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.MenuAnchorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    profileId: String?,
    onBack: () -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = remember(profileId, uiState.profiles) {
        profileId?.let { id -> uiState.profiles.find { it.id == id } }
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var address by remember(existing) { mutableStateOf(existing?.address ?: "") }
    var port by remember(existing) { mutableStateOf(existing?.port?.toString() ?: "443") }
    var protocol by remember(existing) { mutableStateOf(existing?.protocol ?: Protocol.VMESS) }
    var uuid by remember(existing) { mutableStateOf(existing?.uuid ?: "") }
    var password by remember(existing) { mutableStateOf(existing?.password ?: "") }
    var network by remember(existing) { mutableStateOf(existing?.network ?: Network.TCP) }
    var tls by remember(existing) { mutableStateOf(existing?.tls ?: TlsType.NONE) }
    var sni by remember(existing) { mutableStateOf(existing?.sni ?: "") }
    var path by remember(existing) { mutableStateOf(existing?.path ?: "") }
    var host by remember(existing) { mutableStateOf(existing?.host ?: "") }
    var showPass by remember { mutableStateOf(false) }

    val portError = port.toIntOrNull()?.let { it !in 1..65535 } ?: true

    Scaffold(
        topBar = {
            CloudNGTopBar(
                title = if (profileId == null) "Add Profile" else "Edit Profile",
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = {
                            if (!portError && address.isNotBlank()) {
                                val profile = Profile(
                                    id = existing?.id ?: UUID.randomUUID().toString(),
                                    name = name.ifBlank { address },
                                    address = address,
                                    port = port.toIntOrNull() ?: 443,
                                    protocol = protocol,
                                    uuid = uuid,
                                    password = password,
                                    network = network,
                                    tls = tls,
                                    sni = sni,
                                    path = path,
                                    host = host,
                                    remarks = name
                                )
                                viewModel.onEvent(ProfilesEvent.SaveProfile(profile))
                                onBack()
                            }
                        },
                        enabled = !portError && address.isNotBlank()
                    ) { Text("Save") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Name / Remarks") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = address, onValueChange = { address = it },
                label = { Text("Server Address *") }, modifier = Modifier.fillMaxWidth(),
                isError = address.isBlank(),
                supportingText = { if (address.isBlank()) Text("Required") })

            OutlinedTextField(value = port, onValueChange = { port = it },
                label = { Text("Port *") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = portError,
                supportingText = { if (portError) Text("1–65535") })

            DropdownField(
                label = "Protocol",
                options = Protocol.entries.map { it.name },
                selected = protocol.name,
                onSelect = { protocol = Protocol.valueOf(it) }
            )

            when (protocol) {
                Protocol.VMESS, Protocol.VLESS -> {
                    OutlinedTextField(value = uuid, onValueChange = { uuid = it },
                        label = { Text("UUID") }, modifier = Modifier.fillMaxWidth())
                }
                Protocol.TROJAN, Protocol.SHADOWSOCKS, Protocol.SOCKS5, Protocol.HTTP -> {
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        }
                    )
                }
                else -> {}
            }

            DropdownField(
                label = "Network",
                options = Network.entries.map { it.name },
                selected = network.name,
                onSelect = { network = Network.valueOf(it) }
            )

            DropdownField(
                label = "TLS",
                options = TlsType.entries.map { it.name },
                selected = tls.name,
                onSelect = { tls = TlsType.valueOf(it) }
            )

            if (tls != TlsType.NONE) {
                OutlinedTextField(value = sni, onValueChange = { sni = it },
                    label = { Text("SNI") }, modifier = Modifier.fillMaxWidth())
            }

            if (network == Network.WS) {
                OutlinedTextField(value = path, onValueChange = { path = it },
                    label = { Text("Path") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = host, onValueChange = { host = it },
                    label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
