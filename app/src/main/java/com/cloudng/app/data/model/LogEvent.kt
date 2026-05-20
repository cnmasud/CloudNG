package com.cloudng.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogSource { CORE, APP, VPN }

@Entity(tableName = "log_events")
data class LogEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val source: LogSource = LogSource.APP,
    val tag: String = "",
    val message: String = ""
)
