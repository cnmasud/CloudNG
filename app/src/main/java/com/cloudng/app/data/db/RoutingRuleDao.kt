package com.cloudng.app.data.db

import androidx.room.*
import com.cloudng.app.data.model.RoutingRule
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutingRuleDao {
    @Query("SELECT * FROM routing_rules ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<RoutingRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RoutingRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<RoutingRule>)

    @Delete
    suspend fun delete(rule: RoutingRule)

    @Query("DELETE FROM routing_rules")
    suspend fun deleteAll()
}
