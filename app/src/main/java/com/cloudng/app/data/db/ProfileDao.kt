package com.cloudng.app.data.db

import androidx.room.*
import com.cloudng.app.data.model.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): Profile?

    @Query("SELECT * FROM profiles WHERE subscriptionId = :subId")
    suspend fun getBySubscriptionId(subId: String): List<Profile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: Profile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(profiles: List<Profile>)

    @Delete
    suspend fun delete(profile: Profile)

    @Query("DELETE FROM profiles WHERE subscriptionId = :subId")
    suspend fun deleteBySubscriptionId(subId: String)

    @Query("UPDATE profiles SET latencyMs = :latency WHERE id = :id")
    suspend fun updateLatency(id: String, latency: Long)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int
}
