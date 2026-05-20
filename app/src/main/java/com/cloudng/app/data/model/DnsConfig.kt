package com.cloudng.app.data.model

enum class DnsMode {
    SYSTEM, DOH, DOT, FAKE_DNS
}

data class DnsConfig(
    val mode: DnsMode = DnsMode.SYSTEM,
    val primaryServer: String = "",
    val fallbackServer: String = "",
    val enableFakeDns: Boolean = false,
    val domesticDns: String = "223.5.5.5",
    val remoteDns: String = "https://1.1.1.1/dns-query",
    val enableDnsCache: Boolean = true
)
