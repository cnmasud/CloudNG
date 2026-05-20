package com.cloudng.app.core

import com.cloudng.app.data.model.DnsConfig
import com.cloudng.app.data.model.Profile
import com.cloudng.app.data.model.RoutingConfig
import com.cloudng.app.data.model.TrafficStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubCoreBridge @Inject constructor() : CoreBridge {

    private val _state = MutableStateFlow(CoreState.IDLE)
    private val _traffic = MutableStateFlow(TrafficStats.EMPTY)

    override suspend fun start(
        profile: Profile,
        routingConfig: RoutingConfig,
        dnsConfig: DnsConfig,
        tunFd: Int
    ): Result<Unit> {
        _state.value = CoreState.STARTING
        delay(800)
        _state.value = CoreState.RUNNING
        return Result.success(Unit)
    }

    override suspend fun stop(): Result<Unit> {
        _state.value = CoreState.STOPPING
        delay(400)
        _state.value = CoreState.IDLE
        _traffic.value = TrafficStats.EMPTY
        return Result.success(Unit)
    }

    override fun status(): CoreStatus =
        CoreStatus(state = _state.value, coreVersion = coreVersion())

    override suspend fun ping(profile: Profile): PingResult {
        delay(200)
        return PingResult(profileId = profile.id, latencyMs = (80..350).random().toLong(), success = true)
    }

    override suspend fun pingAll(profiles: List<Profile>): List<PingResult> =
        profiles.map { ping(it) }

    override suspend fun updateSubscription(url: String, userAgent: String): Result<String> {
        delay(1000)
        return Result.failure(UnsupportedOperationException("Stub: connect real core to fetch subscriptions"))
    }

    override fun observeState(): Flow<CoreState> = _state.asStateFlow()

    override fun observeTraffic(): Flow<TrafficStats> = flow {
        var up = 0L
        var down = 0L
        while (true) {
            delay(1000)
            if (_state.value == CoreState.RUNNING) {
                up += (1024..51200).random()
                down += (2048..102400).random()
                emit(TrafficStats(uploadBytes = up, downloadBytes = down,
                    uploadSpeed = (512..25600).random().toLong(),
                    downloadSpeed = (1024..51200).random().toLong()))
            }
        }
    }

    override fun observeLogs(): Flow<String> = flow {
        emit("[CloudNG] Core initialized (stub mode)")
    }

    override fun coreVersion(): String = "stub-1.0.0"

    override fun generateConfig(
        profile: Profile,
        routingConfig: RoutingConfig,
        dnsConfig: DnsConfig,
        tunFd: Int
    ): String = """{"stub": true, "profile": "${profile.name}"}"""

    override fun registerSocketProtector(protector: (Int) -> Boolean) = Unit
}
