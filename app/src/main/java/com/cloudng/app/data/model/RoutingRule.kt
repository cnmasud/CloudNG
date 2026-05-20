package com.cloudng.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RoutingMode {
    GLOBAL_PROXY,
    BYPASS_LAN,
    BYPASS_MAINLAND,
    CUSTOM
}

enum class RuleAction {
    PROXY, DIRECT, BLOCK
}

enum class RuleType {
    DOMAIN, DOMAIN_SUFFIX, DOMAIN_KEYWORD, IP_CIDR, GEOIP, GEOSITE, PORT, PROCESS
}

@Entity(tableName = "routing_rules")
data class RoutingRule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: RuleType = RuleType.DOMAIN,
    val value: String = "",
    val action: RuleAction = RuleAction.PROXY,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val description: String = ""
)

data class RoutingConfig(
    val mode: RoutingMode = RoutingMode.BYPASS_LAN,
    val customRules: List<RoutingRule> = emptyList(),
    val bypassedApps: List<String> = emptyList(),
    val proxiedApps: List<String> = emptyList(),
    val perAppMode: PerAppProxyMode = PerAppProxyMode.DISABLED
)

enum class PerAppProxyMode {
    DISABLED, BYPASS_SELECTED, PROXY_ONLY_SELECTED
}
