package com.cloudng.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class Protocol {
    VMESS, VLESS, TROJAN, SHADOWSOCKS, SOCKS5, HTTP, WIREGUARD, UNKNOWN
}

enum class Network {
    TCP, WS, GRPC, QUIC, HTTP2, KCP, UNKNOWN
}

enum class TlsType {
    NONE, TLS, REALITY
}

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val address: String = "",
    val port: Int = 443,
    val protocol: Protocol = Protocol.UNKNOWN,
    val network: Network = Network.TCP,
    val tls: TlsType = TlsType.NONE,
    val uuid: String = "",
    val alterId: Int = 0,
    val security: String = "auto",
    val password: String = "",
    val method: String = "",
    val path: String = "",
    val host: String = "",
    val sni: String = "",
    val fingerprint: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val flow: String = "",
    val remarks: String = "",
    val subscriptionId: String? = null,
    val isActive: Boolean = false,
    val latencyMs: Long = -1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
