package com.cloudng.app.data.db

import androidx.room.*
import com.cloudng.app.data.model.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: String): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subscription: Subscription)

    @Delete
    suspend fun delete(subscription: Subscription)

    @Query("UPDATE subscriptions SET lastUpdatedAt = :ts, profileCount = :count WHERE id = :id")
    suspend fun updateRefreshMeta(id: String, ts: Long, count: Int)
}
