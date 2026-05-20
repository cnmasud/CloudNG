package com.cloudng.app.core

import android.content.Context
import android.util.Log
import com.cloudng.app.data.model.AppSettings
import com.cloudng.app.data.model.DnsConfig
import com.cloudng.app.data.model.DnsMode
import com.cloudng.app.data.model.Network
import com.cloudng.app.data.model.Profile
import com.cloudng.app.data.model.Protocol
import com.cloudng.app.data.model.RuleAction
import com.cloudng.app.data.model.RuleType
import com.cloudng.app.data.model.RoutingConfig
import com.cloudng.app.data.model.RoutingMode
import com.cloudng.app.data.model.TlsType
import com.cloudng.app.data.model.TrafficStats
import com.cloudng.app.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import libv2ray.ProcessFinder
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XrayCoreBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : CoreBridge {

    private val _state = MutableStateFlow(CoreState.IDLE)
    private val _traffic = MutableStateFlow(TrafficStats.EMPTY)
    private var currentSettings: AppSettings = AppSettings()
    private var coreController: CoreController? = null
    private val logBuffer = mutableListOf<String>()
    private var socketProtector: ((Int) -> Boolean)? = null

    override fun registerSocketProtector(protector: (Int) -> Boolean) {
        socketProtector = protector
    }

    private val coreCallback = object : CoreCallbackHandler {
        override fun onEmitStatus(l: Long, s: String): Long {
            Log.d(TAG, "[core] $s")
            synchronized(logBuffer) { logBuffer.add(s) }
            return 0
        }
        override fun startup(): Long {
            Log.i(TAG, "Core startup callback")
            return 0
        }
        override fun shutdown(): Long {
            Log.i(TAG, "Core shutdown callback")
            _state.value = CoreState.IDLE
            return 0
        }
    }

    companion object {
        private const val TAG = "XrayCoreBridge"
    }

    private fun ensureCoreEnvInit() {
        val assetDir = File(context.filesDir, "xray_assets").also { it.mkdirs() }
        listOf("geoip.dat", "geosite.dat").forEach { name ->
            val dest = File(assetDir, name)
            if (!dest.exists()) {
                runCatching {
                    context.assets.open(name).use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
        Libv2ray.initCoreEnv(assetDir.absolutePath, context.filesDir.absolutePath)
    }

    override suspend fun start(
        profile: Profile,
        routingConfig: RoutingConfig,
        dnsConfig: DnsConfig,
        tunFd: Int
    ): Result<Unit> {
        return try {
            _state.value = CoreState.STARTING
            currentSettings = settingsRepository.appSettings.first()
            val config = generateConfig(profile, routingConfig, dnsConfig, tunFd)
            withContext(Dispatchers.IO) {
                ensureCoreEnvInit()
                val controller = Libv2ray.newCoreController(coreCallback)
                coreController = controller
                val prot = socketProtector
                if (prot != null) {
                    controller.registerProcessFinder(object : ProcessFinder {
                        override fun findProcessByConnection(
                            localAddr: String, localPort: String,
                            uid: Long,
                            remoteAddr: String, remotePort: Long
                        ): Long {
                            return 0
                        }
                    })
                }
                controller.startLoop(config, tunFd)
            }
            _state.value = CoreState.RUNNING
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Core start failed", e)
            _state.value = CoreState.ERROR
            Result.failure(e)
        }
    }

    override suspend fun stop(): Result<Unit> {
        return try {
            _state.value = CoreState.STOPPING
            withContext(Dispatchers.IO) {
                coreController?.stopLoop()
                coreController = null
            }
            _state.value = CoreState.IDLE
            _traffic.value = TrafficStats.EMPTY
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Core stop failed", e)
            coreController = null
            _state.value = CoreState.IDLE
            Result.failure(e)
        }
    }

    override fun status(): CoreStatus = CoreStatus(state = _state.value, coreVersion = coreVersion())

    override suspend fun ping(profile: Profile): PingResult {
        return withContext(Dispatchers.IO) {
            val hosts = listOf("1.1.1.1" to 80, "8.8.8.8" to 80)
            var best = Long.MAX_VALUE
            for ((host, port) in hosts) {
                try {
                    val start = System.currentTimeMillis()
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(host, port), 3000)
                    socket.close()
                    val elapsed = System.currentTimeMillis() - start
                    if (elapsed < best) best = elapsed
                } catch (_: Exception) {}
            }
            if (best == Long.MAX_VALUE)
                PingResult(profileId = profile.id, latencyMs = -1, success = false, error = "Unreachable")
            else
                PingResult(profileId = profile.id, latencyMs = best, success = true)
        }
    }

    override suspend fun pingAll(profiles: List<Profile>): List<PingResult> =
        profiles.map { ping(it) }

    override suspend fun updateSubscription(url: String, userAgent: String): Result<String> =
        Result.failure(UnsupportedOperationException("Use HTTP client to fetch subscription"))

    override fun observeState(): Flow<CoreState> = _state.asStateFlow()

    override fun observeTraffic(): Flow<TrafficStats> = flow {
        var prevUp = 0L
        var prevDown = 0L
        while (true) {
            delay(1000)
            val ctrl = coreController
            if (_state.value == CoreState.RUNNING && ctrl != null) {
                val up = runCatching { ctrl.queryStats("proxy", "uplink") }.getOrDefault(0L)
                val down = runCatching { ctrl.queryStats("proxy", "downlink") }.getOrDefault(0L)
                emit(TrafficStats(
                    uploadBytes = up,
                    downloadBytes = down,
                    uploadSpeed = (up - prevUp).coerceAtLeast(0L),
                    downloadSpeed = (down - prevDown).coerceAtLeast(0L)
                ))
                prevUp = up
                prevDown = down
            }
        }
    }

    override fun observeLogs(): Flow<String> = flow {
        var lastIdx = 0
        while (true) {
            delay(500)
            val snapshot = synchronized(logBuffer) {
                if (lastIdx < logBuffer.size) logBuffer.subList(lastIdx, logBuffer.size).toList()
                else emptyList()
            }
            snapshot.forEach { emit(it) }
            lastIdx += snapshot.size
        }
    }

    override fun coreVersion(): String = runCatching { Libv2ray.checkVersionX() }.getOrDefault("libv2ray-v26.5.9")

    override fun generateConfig(
        profile: Profile,
        routingConfig: RoutingConfig,
        dnsConfig: DnsConfig,
        tunFd: Int
    ): String {
        val config = JSONObject()

        config.put("log", JSONObject().apply {
            put("loglevel", "info")
            put("access", "")
            put("error", "")
        })

        config.put("policy", JSONObject().apply {
            put("levels", JSONObject().apply {
                put("0", JSONObject().apply {
                    put("statsUserUplink", true)
                    put("statsUserDownlink", true)
                })
            })
            put("system", JSONObject().apply {
                put("statsOutboundUplink", true)
                put("statsOutboundDownlink", true)
            })
        })

        config.put("stats", JSONObject())

        config.put("inbounds", JSONArray().apply {
            // tun2socks connects here via SOCKS5 — listen on loopback port 10808
            put(JSONObject().apply {
                put("tag", "socks-tun")
                put("protocol", "socks")
                put("port", 10808)
                put("listen", "127.0.0.1")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                    put("userLevel", 0)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http"); put("tls"); put("quic")
                    })
                    put("routeOnly", false)
                })
            })
            // External SOCKS proxy for manual use
            put(JSONObject().apply {
                put("tag", "socks")
                put("port", currentSettings.socksPort)
                put("listen", "0.0.0.0")
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http"); put("tls")
                    })
                })
            })
            put(JSONObject().apply {
                put("tag", "http")
                put("port", currentSettings.httpPort)
                put("listen", "0.0.0.0")
                put("protocol", "http")
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http"); put("tls")
                    })
                })
            })
        })

        config.put("outbounds", JSONArray().apply {
            put(buildOutbound(profile))
            put(JSONObject().apply {
                put("tag", "direct")
                put("protocol", "freedom")
            })
            put(JSONObject().apply {
                put("tag", "block")
                put("protocol", "blackhole")
            })
        })

        config.put("dns", buildDns(dnsConfig))
        config.put("routing", buildRouting(routingConfig))

        return config.toString(2)
    }

    private fun buildOutbound(profile: Profile): JSONObject {
        return JSONObject().apply {
            put("tag", "proxy")
            when (profile.protocol) {
                Protocol.VLESS -> {
                    put("protocol", "vless")
                    put("settings", JSONObject().apply {
                        put("vnext", JSONArray().apply {
                            put(JSONObject().apply {
                                put("address", profile.address)
                                put("port", profile.port)
                                put("users", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("id", profile.uuid)
                                        put("encryption", "none")
                                        if (profile.flow.isNotBlank()) put("flow", profile.flow)
                                    })
                                })
                            })
                        })
                    })
                    put("streamSettings", buildStreamSettings(profile))
                }
                Protocol.VMESS -> {
                    put("protocol", "vmess")
                    put("settings", JSONObject().apply {
                        put("vnext", JSONArray().apply {
                            put(JSONObject().apply {
                                put("address", profile.address)
                                put("port", profile.port)
                                put("users", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("id", profile.uuid)
                                        put("alterId", profile.alterId)
                                        put("security", profile.security.ifBlank { "auto" })
                                    })
                                })
                            })
                        })
                    })
                    put("streamSettings", buildStreamSettings(profile))
                }
                Protocol.TROJAN -> {
                    put("protocol", "trojan")
                    put("settings", JSONObject().apply {
                        put("servers", JSONArray().apply {
                            put(JSONObject().apply {
                                put("address", profile.address)
                                put("port", profile.port)
                                put("password", profile.password)
                            })
                        })
                    })
                    put("streamSettings", buildStreamSettings(profile))
                }
                Protocol.SHADOWSOCKS -> {
                    put("protocol", "shadowsocks")
                    put("settings", JSONObject().apply {
                        put("servers", JSONArray().apply {
                            put(JSONObject().apply {
                                put("address", profile.address)
                                put("port", profile.port)
                                put("method", profile.method)
                                put("password", profile.password)
                            })
                        })
                    })
                }
                else -> {
                    put("protocol", "freedom")
                }
            }
            put("mux", JSONObject().apply {
                put("enabled", false)
            })
        }
    }

    private fun buildStreamSettings(profile: Profile): JSONObject {
        return JSONObject().apply {
            val net = when (profile.network) {
                Network.WS -> "ws"
                Network.GRPC -> "grpc"
                Network.HTTP2 -> "h2"
                Network.KCP -> "kcp"
                Network.QUIC -> "quic"
                else -> "tcp"
            }
            put("network", net)

            when (profile.network) {
                Network.WS -> put("wsSettings", JSONObject().apply {
                    put("path", profile.path.ifBlank { "/" })
                    put("headers", JSONObject().apply {
                        if (profile.host.isNotBlank()) put("Host", profile.host)
                    })
                })
                Network.GRPC -> put("grpcSettings", JSONObject().apply {
                    put("serviceName", profile.path)
                })
                Network.HTTP2 -> put("httpSettings", JSONObject().apply {
                    put("path", profile.path.ifBlank { "/" })
                    if (profile.host.isNotBlank()) {
                        put("host", JSONArray().apply { put(profile.host) })
                    }
                })
                else -> {}
            }

            when (profile.tls) {
                TlsType.TLS -> put("security", "tls").also {
                    put("tlsSettings", JSONObject().apply {
                        if (profile.sni.isNotBlank()) put("serverName", profile.sni)
                        if (profile.fingerprint.isNotBlank()) put("fingerprint", profile.fingerprint)
                        put("allowInsecure", false)
                        put("alpn", JSONArray().apply {
                            if (profile.network == Network.HTTP2) put("h2")
                            put("http/1.1")
                        })
                    })
                }
                TlsType.REALITY -> put("security", "reality").also {
                    put("realitySettings", JSONObject().apply {
                        if (profile.sni.isNotBlank()) put("serverName", profile.sni)
                        if (profile.fingerprint.isNotBlank()) put("fingerprint", profile.fingerprint)
                        if (profile.publicKey.isNotBlank()) put("publicKey", profile.publicKey)
                        if (profile.shortId.isNotBlank()) put("shortId", profile.shortId)
                    })
                }
                TlsType.NONE -> put("security", "none")
            }
        }
    }

    private fun buildDns(dnsConfig: DnsConfig): JSONObject {
        val servers = JSONArray()
        when (dnsConfig.mode) {
            DnsMode.DOH -> {
                servers.put(dnsConfig.remoteDns.ifBlank { "https://1.1.1.1/dns-query" })
                servers.put("8.8.8.8")
            }
            DnsMode.DOT -> {
                servers.put("tls:${dnsConfig.primaryServer.ifBlank { "1.1.1.1" }}")
                servers.put("8.8.8.8")
            }
            else -> {
                servers.put("8.8.8.8")
                servers.put("1.1.1.1")
                servers.put("localhost")
            }
        }
        return JSONObject().apply {
            put("servers", servers)
        }
    }

    private fun buildRouting(routingConfig: RoutingConfig): JSONObject {
        return JSONObject().apply {
            put("domainStrategy", "IPIfNonMatch")
            put("rules", JSONArray().apply {
                when (routingConfig.mode) {
                    RoutingMode.GLOBAL_PROXY -> {
                        put(JSONObject().apply {
                            put("type", "field")
                            put("outboundTag", "proxy")
                            put("port", "0-65535")
                        })
                    }
                    RoutingMode.BYPASS_LAN -> {
                        put(JSONObject().apply {
                            put("type", "field")
                            put("ip", JSONArray().apply {
                                put("geoip:private")
                                put("127.0.0.0/8")
                                put("10.0.0.0/8")
                                put("172.16.0.0/12")
                                put("192.168.0.0/16")
                            })
                            put("outboundTag", "direct")
                        })
                        put(JSONObject().apply {
                            put("type", "field")
                            put("outboundTag", "proxy")
                            put("port", "0-65535")
                        })
                    }
                    RoutingMode.BYPASS_MAINLAND -> {
                        put(JSONObject().apply {
                            put("type", "field")
                            put("ip", JSONArray().apply { put("geoip:private") })
                            put("outboundTag", "direct")
                        })
                        put(JSONObject().apply {
                            put("type", "field")
                            put("ip", JSONArray().apply { put("geoip:cn") })
                            put("outboundTag", "direct")
                        })
                        put(JSONObject().apply {
                            put("type", "field")
                            put("domain", JSONArray().apply { put("geosite:cn") })
                            put("outboundTag", "direct")
                        })
                        put(JSONObject().apply {
                            put("type", "field")
                            put("outboundTag", "proxy")
                            put("port", "0-65535")
                        })
                    }
                    RoutingMode.CUSTOM -> {
                        routingConfig.customRules.filter { it.enabled }.forEach { rule ->
                            put(JSONObject().apply {
                                put("type", "field")
                                val outbound = when (rule.action) {
                                    RuleAction.PROXY -> "proxy"
                                    RuleAction.DIRECT -> "direct"
                                    RuleAction.BLOCK -> "block"
                                }
                                put("outboundTag", outbound)
                                when (rule.type) {
                                    RuleType.DOMAIN, RuleType.DOMAIN_SUFFIX, RuleType.DOMAIN_KEYWORD, RuleType.GEOSITE ->
                                        put("domain", JSONArray().apply { put(rule.value) })
                                    RuleType.IP_CIDR, RuleType.GEOIP ->
                                        put("ip", JSONArray().apply { put(rule.value) })
                                    RuleType.PORT ->
                                        put("port", rule.value)
                                    RuleType.PROCESS ->
                                        put("process", JSONArray().apply { put(rule.value) })
                                }
                            })
                        }
                        put(JSONObject().apply {
                            put("type", "field")
                            put("outboundTag", "proxy")
                            put("port", "0-65535")
                        })
                    }
                }
            })
        }
    }
}
