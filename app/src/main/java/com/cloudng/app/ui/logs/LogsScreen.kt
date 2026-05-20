package com.cloudng.app.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudng.app.data.model.LogEvent
import com.cloudng.app.ui.components.CloudNGTopBar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(onBack: () -> Unit, viewModel: LogsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.events.size) {
        if (uiState.events.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            CloudNGTopBar(
                title = "Logs (${uiState.events.size})",
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            listOf(null, "DEBUG", "INFO", "WARNING", "ERROR").forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level ?: "All levels") },
                                    onClick = {
                                        viewModel.onEvent(LogsEvent.SetFilter(level))
                                        showFilterMenu = false
                                    },
                                    trailingIcon = {
                                        if (uiState.filterLevel == level) {
                                            Icon(Icons.Default.FilterList, null,
                                                modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.onEvent(LogsEvent.ClearLogs) }) {
                        Icon(Icons.Default.ClearAll, "Clear")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0A0E14))
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                reverseLayout = false
            ) {
                items(uiState.events, key = { it.id }) { event ->
                    LogLine(event)
                }
            }
        }
    }
}

@Composable
private fun LogLine(event: LogEvent) {
    val fmt = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val levelColor = when (event.level) {
        "ERROR"   -> Color(0xFFFF5252)
        "WARNING" -> Color(0xFFFFD740)
        "DEBUG"   -> Color(0xFF64FFDA)
        else      -> Color(0xFFB0BEC5)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = fmt.format(Date(event.timestamp)),
            color = Color(0xFF546E7A),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = levelColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = event.level.padEnd(5),
                color = levelColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = "[${event.tag}] ${event.message}",
            color = Color(0xFFCFD8DC),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}
