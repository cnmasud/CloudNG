package com.cloudng.app.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudng.app.core.CoreBridge
import com.cloudng.app.data.db.LogEventDao
import com.cloudng.app.data.model.LogEvent
import com.cloudng.app.data.model.LogSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogsUiState(
    val events: List<LogEvent> = emptyList(),
    val filterLevel: String? = null
)

sealed class LogsEvent {
    object ClearLogs : LogsEvent()
    data class SetFilter(val level: String?) : LogsEvent()
}

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logEventDao: LogEventDao,
    private val coreBridge: CoreBridge
) : ViewModel() {

    private val _filterLevel = MutableStateFlow<String?>(null)
    val uiState: StateFlow<LogsUiState> = combine(
        logEventDao.observeRecent(500),
        _filterLevel
    ) { events, level ->
        LogsUiState(
            events = if (level != null) events.filter { it.level == level } else events,
            filterLevel = level
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogsUiState())

    init {
        viewModelScope.launch {
            coreBridge.observeLogs().collect { line ->
                logEventDao.insert(
                    LogEvent(
                        source = LogSource.CORE,
                        level = "INFO",
                        tag = "core",
                        message = line
                    )
                )
                logEventDao.pruneOld()
            }
        }
    }

    fun onEvent(event: LogsEvent) {
        when (event) {
            LogsEvent.ClearLogs -> viewModelScope.launch { logEventDao.clear() }
            is LogsEvent.SetFilter -> _filterLevel.value = event.level
        }
    }
}
