package com.cloudng.app.core

import android.net.Uri
import android.util.Base64
import com.cloudng.app.data.model.Network
import com.cloudng.app.data.model.Profile
import com.cloudng.app.data.model.Protocol
import com.cloudng.app.data.model.TlsType
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

sealed class ParseResult {
    data class Success(val profiles: List<Profile>) : ParseResult()
    data class Failure(val reason: String) : ParseResult()
}

@Singleton
class ProfileParser @Inject constructor() {

    fun parse(raw: String): ParseResult {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("vmess://") -> parseVmess(trimmed)
            trimmed.startsWith("vless://") -> parseVless(trimmed)
            trimmed.startsWith("trojan://") -> parseTrojan(trimmed)
            trimmed.startsWith("ss://") -> parseShadowsocks(trimmed)
            trimmed.startsWith("{") -> parseJsonConfig(trimmed)
            else -> tryBase64Multi(trimmed)
        }
    }

    private fun tryBase64Multi(raw: String): ParseResult {
        return try {
            val decoded = String(Base64.decode(raw, Base64.DEFAULT or Base64.URL_SAFE))
            val lines = decoded.lines().filter { it.isNotBlank() }
            val results = lines.mapNotNull { line ->
                (parse(line) as? ParseResult.Success)?.profiles?.firstOrNull()
            }
            if (results.isNotEmpty()) ParseResult.Success(results)
            else ParseResult.Failure("No valid profiles found in decoded content")
        } catch (e: Exception) {
            ParseResult.Failure("Unrecognized format")
        }
    }

    private fun parseVmess(uri: String): ParseResult {
        return try {
            val b64 = uri.removePrefix("vmess://")
            val json = String(Base64.decode(b64, Base64.DEFAULT or Base64.URL_SAFE))
            val obj = JsonParser.parseString(json).asJsonObject
            val profile = Profile(
                name = obj.getStringOrEmpty("ps"),
                address = obj.getStringOrEmpty("add"),
                port = obj.get("port")?.let {
                    if (it.isJsonPrimitive) it.asString.toIntOrNull() ?: 443 else 443
                } ?: 443,
                protocol = Protocol.VMESS,
                uuid = obj.getStringOrEmpty("id"),
                alterId = obj.get("aid")?.asString?.toIntOrNull() ?: 0,
                security = obj.getStringOrEmpty("scy").ifBlank { "auto" },
                network = obj.getStringOrEmpty("net").toNetwork(),
                path = obj.getStringOrEmpty("path"),
                host = obj.getStringOrEmpty("host"),
                tls = if (obj.getStringOrEmpty("tls") == "tls") TlsType.TLS else TlsType.NONE,
                sni = obj.getStringOrEmpty("sni"),
                remarks = obj.getStringOrEmpty("ps")
            )
            ParseResult.Success(listOf(profile))
        } catch (e: Exception) {
            ParseResult.Failure("VMess parse error: ${e.message}")
        }
    }

    private fun parseVless(uri: String): ParseResult {
        return try {
            val u = Uri.parse(uri)
            val params = u.query?.toQueryMap() ?: emptyMap()
            val profile = Profile(
                name = u.fragment ?: u.host ?: "",
                address = u.host ?: "",
                port = u.port.takeIf { it > 0 } ?: 443,
                protocol = Protocol.VLESS,
                uuid = u.userInfo ?: "",
                network = (params["type"] ?: "tcp").toNetwork(),
                tls = when (params["security"]) {
                    "tls" -> TlsType.TLS
                    "reality" -> TlsType.REALITY
                    else -> TlsType.NONE
                },
                sni = params["sni"] ?: "",
                path = params["path"] ?: "",
                host = params["host"] ?: "",
                flow = params["flow"] ?: "",
                publicKey = params["pbk"] ?: "",
                shortId = params["sid"] ?: "",
                fingerprint = params["fp"] ?: "",
                remarks = u.fragment ?: ""
            )
            ParseResult.Success(listOf(profile))
        } catch (e: Exception) {
            ParseResult.Failure("VLESS parse error: ${e.message}")
        }
    }

    private fun parseTrojan(uri: String): ParseResult {
        return try {
            val u = Uri.parse(uri)
            val params = u.query?.toQueryMap() ?: emptyMap()
            val profile = Profile(
                name = u.fragment ?: u.host ?: "",
                address = u.host ?: "",
                port = u.port.takeIf { it > 0 } ?: 443,
                protocol = Protocol.TROJAN,
                password = u.userInfo ?: "",
                tls = TlsType.TLS,
                sni = params["sni"] ?: "",
                network = (params["type"] ?: "tcp").toNetwork(),
                path = params["path"] ?: "",
                host = params["host"] ?: "",
                remarks = u.fragment ?: ""
            )
            ParseResult.Success(listOf(profile))
        } catch (e: Exception) {
            ParseResult.Failure("Trojan parse error: ${e.message}")
        }
    }

    private fun parseShadowsocks(uri: String): ParseResult {
        return try {
            val noPrefix = uri.removePrefix("ss://")
            val hashIdx = noPrefix.indexOf('#')
            val remarks = if (hashIdx >= 0) Uri.decode(noPrefix.substring(hashIdx + 1)) else ""
            val main = if (hashIdx >= 0) noPrefix.substring(0, hashIdx) else noPrefix
            val atIdx = main.lastIndexOf('@')
            val (methodAndPass, hostAndPort) = if (atIdx >= 0) {
                val decoded = String(Base64.decode(main.substring(0, atIdx), Base64.DEFAULT or Base64.URL_SAFE))
                decoded to main.substring(atIdx + 1)
            } else {
                val decoded = String(Base64.decode(main, Base64.DEFAULT or Base64.URL_SAFE))
                val inner = decoded.substringBefore('@') to decoded.substringAfterLast('@')
                inner
            }
            val method = methodAndPass.substringBefore(':')
            val password = methodAndPass.substringAfter(':')
            val host = hostAndPort.substringBeforeLast(':')
            val port = hostAndPort.substringAfterLast(':').toIntOrNull() ?: 8388
            ParseResult.Success(listOf(Profile(
                name = remarks.ifBlank { host },
                address = host,
                port = port,
                protocol = Protocol.SHADOWSOCKS,
                method = method,
                password = password,
                remarks = remarks
            )))
        } catch (e: Exception) {
            ParseResult.Failure("Shadowsocks parse error: ${e.message}")
        }
    }

    private fun parseJsonConfig(json: String): ParseResult {
        return ParseResult.Failure("Raw JSON import: use file import flow")
    }

    private fun com.google.gson.JsonObject.getStringOrEmpty(key: String): String =
        get(key)?.takeIf { !it.isJsonNull }?.asString ?: ""

    private fun String.toNetwork(): Network = when (lowercase()) {
        "ws" -> Network.WS
        "grpc" -> Network.GRPC
        "quic" -> Network.QUIC
        "h2", "http" -> Network.HTTP2
        "kcp" -> Network.KCP
        else -> Network.TCP
    }

    private fun String.toQueryMap(): Map<String, String> {
        return split("&").mapNotNull {
            val kv = it.split("=", limit = 2)
            if (kv.size == 2) Uri.decode(kv[0]) to Uri.decode(kv[1]) else null
        }.toMap()
    }
}
