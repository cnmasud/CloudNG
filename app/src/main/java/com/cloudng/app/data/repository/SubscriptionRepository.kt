package com.cloudng.app.data.repository

import com.cloudng.app.core.ParseResult
import com.cloudng.app.core.ProfileParser
import com.cloudng.app.data.db.SubscriptionDao
import com.cloudng.app.data.model.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val dao: SubscriptionDao,
    private val profileRepository: ProfileRepository,
    private val parser: ProfileParser
) {
    fun observeAll(): Flow<List<Subscription>> = dao.observeAll()

    suspend fun getById(id: String): Subscription? = dao.getById(id)

    suspend fun save(subscription: Subscription) = dao.upsert(subscription)

    suspend fun delete(subscription: Subscription) {
        profileRepository.deleteBySubscriptionId(subscription.id)
        dao.delete(subscription)
    }

    suspend fun refresh(subscription: Subscription): Result<Int> {
        return runCatching {
            val raw = withContext(Dispatchers.IO) { fetchUrl(subscription.url, subscription.userAgent) }
            profileRepository.deleteBySubscriptionId(subscription.id)
            val parseResult = parser.parse(raw)
            val profiles = when (parseResult) {
                is ParseResult.Success -> parseResult.profiles.map { it.copy(subscriptionId = subscription.id) }
                is ParseResult.Failure -> emptyList()
            }
            profileRepository.saveAll(profiles)
            dao.updateRefreshMeta(subscription.id, System.currentTimeMillis(), profiles.size)
            profiles.size
        }
    }

    private fun fetchUrl(url: String, userAgent: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", userAgent)
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code for $url")
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }
}
