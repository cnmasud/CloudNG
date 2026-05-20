package com.cloudng.app.data.repository

import com.cloudng.app.data.db.ProfileDao
import com.cloudng.app.data.model.Profile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao
) {
    fun observeAll(): Flow<List<Profile>> = dao.observeAll()

    suspend fun getById(id: String): Profile? = dao.getById(id)

    suspend fun save(profile: Profile) = dao.upsert(profile)

    suspend fun saveAll(profiles: List<Profile>) = dao.upsertAll(profiles)

    suspend fun delete(profile: Profile) = dao.delete(profile)

    suspend fun deleteBySubscriptionId(subId: String) = dao.deleteBySubscriptionId(subId)

    suspend fun updateLatency(id: String, latencyMs: Long) = dao.updateLatency(id, latencyMs)
}
