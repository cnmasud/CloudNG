package com.cloudng.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class UpdateInterval {
    MANUAL, HOURLY, EVERY_6H, DAILY, WEEKLY
}

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val url: String = "",
    val enabled: Boolean = true,
    val updateInterval: UpdateInterval = UpdateInterval.DAILY,
    val lastUpdatedAt: Long = 0L,
    val profileCount: Int = 0,
    val userAgent: String = "CloudNG/1.0",
    val createdAt: Long = System.currentTimeMillis()
)
