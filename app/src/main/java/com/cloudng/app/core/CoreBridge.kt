package com.cloudng.app.core

import com.cloudng.app.data.model.DnsConfig
import com.cloudng.app.data.model.Profile
import com.cloudng.app.data.model.RoutingConfig
import com.cloudng.app.data.model.TrafficStats
import kotlinx.coroutines.flow.Flow

enum class CoreState { IDLE, STARTING, RUNNING, STOPPING, ERROR }

data class CoreStatus(
    val state: CoreState = CoreState.IDLE,
    val errorMessage: String? = null,
    val coreVersion: String = ""
)

data class PingResult(
    val profileId: String,
    val latencyMs: Long,
    val success: Boolean,
    val error: String? = null
)

interface CoreBridge {
    suspend fun start(profile: Profile, routingConfig: RoutingConfig, dnsConfig: DnsConfig, tunFd: Int = -1): Result<Unit>
    suspend fun stop(): Result<Unit>
    fun status(): CoreStatus
    suspend fun ping(profile: Profile): PingResult
    suspend fun pingAll(profiles: List<Profile>): List<PingResult>
    suspend fun updateSubscription(url: String, userAgent: String): Result<String>
    fun observeState(): Flow<CoreState>
    fun observeTraffic(): Flow<TrafficStats>
    fun observeLogs(): Flow<String>
    fun coreVersion(): String
    fun generateConfig(profile: Profile, routingConfig: RoutingConfig, dnsConfig: DnsConfig, tunFd: Int = -1): String
    fun registerSocketProtector(protector: (Int) -> Boolean)
}
