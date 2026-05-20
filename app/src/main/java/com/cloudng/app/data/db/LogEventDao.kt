package com.cloudng.app.data.db

import androidx.room.*
import com.cloudng.app.data.model.LogEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEventDao {
    @Query("SELECT * FROM log_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<LogEvent>>

    @Insert
    suspend fun insert(event: LogEvent)

    @Query("DELETE FROM log_events WHERE id NOT IN (SELECT id FROM log_events ORDER BY timestamp DESC LIMIT 1000)")
    suspend fun pruneOld()

    @Query("DELETE FROM log_events")
    suspend fun clear()
}
