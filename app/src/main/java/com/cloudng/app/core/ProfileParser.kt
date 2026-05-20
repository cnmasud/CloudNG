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
            isClashYaml(trimmed) -> parseClashYaml(trimmed)
            else -> tryBase64Multi(trimmed)
        }
    }

    private fun isClashYaml(raw: String): Boolean =
        raw.contains("proxies:") && (raw.contains("type: vless") || raw.contains("type: vmess") ||
                raw.contains("type: trojan") || raw.contains("type: ss"))

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

    private fun parseClashYaml(raw: String): ParseResult {
        return try {
            val profiles = mutableListOf<Profile>()
            val lines = raw.lines()
            var inProxies = false
            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed == "proxies:" -> { inProxies = true; continue }
                    trimmed.startsWith("proxy-groups:") || trimmed.startsWith("rules:") -> inProxies = false
                    inProxies && trimmed.startsWith("- {") -> {
                        val map = parseInlineMap(trimmed.removePrefix("- "))
                        val p = clashMapToProfile(map)
                        if (p != null) profiles.add(p)
                    }
                    inProxies && trimmed.startsWith("- name:") -> {
                        // block-style proxy — collect until next "- " entry
                    }
                }
            }
            if (profiles.isEmpty()) ParseResult.Failure("No proxies found in Clash YAML")
            else ParseResult.Success(profiles)
        } catch (e: Exception) {
            ParseResult.Failure("Clash YAML parse error: ${e.message}")
        }
    }

    private fun parseInlineMap(inline: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val content = inline.trim().removeSurrounding("{", "}").trim()

        // Handle nested maps like reality-opts: { public-key: X, short-id: Y }
        val nestedRegex = Regex("""([\w-]+):\s*\{([^}]*)\}""")
        var remaining = content
        nestedRegex.findAll(content).forEach { match ->
            val parentKey = match.groupValues[1]
            val innerContent = match.groupValues[2]
            val innerMap = parseInlineMap("{$innerContent}")
            innerMap.forEach { (k, v) -> result["$parentKey.$k"] = v }
            remaining = remaining.replace(match.value, "")
        }

        // Parse remaining key: value pairs
        val kvRegex = Regex("""([\w-]+):\s*(?:'([^']*)'|"([^"]*)"|(\S+))""")
        kvRegex.findAll(remaining).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].ifEmpty {
                match.groupValues[3].ifEmpty { match.groupValues[4] }
            }
            if (key.isNotBlank()) result[key] = value.trimEnd(',')
        }
        return result
    }

    private fun clashMapToProfile(map: Map<String, String>): Profile? {
        val type = map["type"] ?: return null
        val name = map["name"] ?: return null
        val server = map["server"] ?: return null
        val port = map["port"]?.toIntOrNull() ?: return null
        val uuid = map["uuid"] ?: ""
        val password = map["password"] ?: ""

        // Skip info-only entries (traffic/expiry notices)
        val infoPatterns = listOf("剩余", "流量", "距离", "套餐", "到期", "重置")
        if (infoPatterns.any { name.contains(it) }) return null

        val network = (map["network"] ?: "tcp").toNetwork()
        val hasTls = map["tls"] == "true"
        val hasReality = map.containsKey("reality-opts.public-key")
        val flow = map["flow"] ?: ""
        val sni = map["servername"] ?: map["sni"] ?: ""
        val publicKey = map["reality-opts.public-key"] ?: ""
        val shortId = map["reality-opts.short-id"] ?: ""
        val fingerprint = map["client-fingerprint"] ?: ""

        val tlsType = when {
            hasReality -> TlsType.REALITY
            hasTls -> TlsType.TLS
            else -> TlsType.NONE
        }

        return when (type) {
            "vless" -> Profile(
                name = name,
                address = server,
                port = port,
                protocol = Protocol.VLESS,
                uuid = uuid,
                network = network,
                tls = tlsType,
                sni = sni,
                flow = flow,
                publicKey = publicKey,
                shortId = shortId,
                fingerprint = fingerprint,
                remarks = name
            )
            "vmess" -> Profile(
                name = name,
                address = server,
                port = port,
                protocol = Protocol.VMESS,
                uuid = uuid,
                alterId = map["alterId"]?.toIntOrNull() ?: 0,
                security = map["cipher"] ?: "auto",
                network = network,
                tls = if (hasTls) TlsType.TLS else TlsType.NONE,
                sni = sni,
                remarks = name
            )
            "trojan" -> Profile(
                name = name,
                address = server,
                port = port,
                protocol = Protocol.TROJAN,
                password = password,
                network = network,
                tls = TlsType.TLS,
                sni = sni,
                remarks = name
            )
            "ss" -> Profile(
                name = name,
                address = server,
                port = port,
                protocol = Protocol.SHADOWSOCKS,
                method = map["cipher"] ?: "aes-256-gcm",
                password = password,
                remarks = name
            )
            else -> null
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
