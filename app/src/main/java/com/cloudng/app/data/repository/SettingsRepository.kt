package com.cloudng.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.cloudng.app.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson
) {
    companion object {
        val KEY_AUTO_START = booleanPreferencesKey("auto_start")
        val KEY_KILL_SWITCH = booleanPreferencesKey("kill_switch")
        val KEY_SAFE_MODE = booleanPreferencesKey("safe_mode")
        val KEY_SOCKS_PORT = intPreferencesKey("socks_port")
        val KEY_HTTP_PORT = intPreferencesKey("http_port")
        val KEY_MTU = intPreferencesKey("mtu")
        val KEY_ALLOW_LAN = booleanPreferencesKey("allow_lan")
        val KEY_LOG_LEVEL = stringPreferencesKey("log_level")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_TELEMETRY = booleanPreferencesKey("telemetry")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_SELECTED_PROFILE = stringPreferencesKey("selected_profile_id")
        val KEY_DNS_CONFIG = stringPreferencesKey("dns_config_json")
        val KEY_ROUTING_MODE = stringPreferencesKey("routing_mode")
        val KEY_PER_APP_MODE = stringPreferencesKey("per_app_mode")
        val KEY_BYPASSED_APPS = stringPreferencesKey("bypassed_apps_json")
        val KEY_PROXIED_APPS = stringPreferencesKey("proxied_apps_json")
    }

    val appSettings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                autoStartOnBoot = prefs[KEY_AUTO_START] ?: false,
                killSwitch = prefs[KEY_KILL_SWITCH] ?: true,
                safeMode = prefs[KEY_SAFE_MODE] ?: false,
                socksPort = prefs[KEY_SOCKS_PORT] ?: 10808,
                httpPort = prefs[KEY_HTTP_PORT] ?: 10809,
                mtu = prefs[KEY_MTU] ?: 1500,
                allowLan = prefs[KEY_ALLOW_LAN] ?: false,
                logLevel = prefs[KEY_LOG_LEVEL]?.let { runCatching { LogLevel.valueOf(it) }.getOrNull() } ?: LogLevel.WARNING,
                theme = prefs[KEY_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
                language = prefs[KEY_LANGUAGE] ?: "system",
                telemetryEnabled = prefs[KEY_TELEMETRY] ?: false,
                onboardingComplete = prefs[KEY_ONBOARDING_DONE] ?: false,
                lastSelectedProfileId = prefs[KEY_SELECTED_PROFILE]
            )
        }

    val dnsConfig: Flow<DnsConfig> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[KEY_DNS_CONFIG]?.let {
                runCatching { gson.fromJson(it, DnsConfig::class.java) }.getOrNull()
            } ?: DnsConfig()
        }

    val routingConfig: Flow<RoutingConfig> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val mode = prefs[KEY_ROUTING_MODE]?.let { runCatching { RoutingMode.valueOf(it) }.getOrNull() } ?: RoutingMode.BYPASS_LAN
            val perApp = prefs[KEY_PER_APP_MODE]?.let { runCatching { PerAppProxyMode.valueOf(it) }.getOrNull() } ?: PerAppProxyMode.DISABLED
            val bypassed = prefs[KEY_BYPASSED_APPS]?.let {
                runCatching { gson.fromJson(it, Array<String>::class.java).toList() }.getOrNull()
            } ?: emptyList()
            val proxied = prefs[KEY_PROXIED_APPS]?.let {
                runCatching { gson.fromJson(it, Array<String>::class.java).toList() }.getOrNull()
            } ?: emptyList()
            RoutingConfig(mode = mode, bypassedApps = bypassed, proxiedApps = proxied, perAppMode = perApp)
        }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_START] = settings.autoStartOnBoot
            prefs[KEY_KILL_SWITCH] = settings.killSwitch
            prefs[KEY_SAFE_MODE] = settings.safeMode
            prefs[KEY_SOCKS_PORT] = settings.socksPort
            prefs[KEY_HTTP_PORT] = settings.httpPort
            prefs[KEY_MTU] = settings.mtu
            prefs[KEY_ALLOW_LAN] = settings.allowLan
            prefs[KEY_LOG_LEVEL] = settings.logLevel.name
            prefs[KEY_THEME] = settings.theme.name
            prefs[KEY_LANGUAGE] = settings.language
            prefs[KEY_TELEMETRY] = settings.telemetryEnabled
            prefs[KEY_ONBOARDING_DONE] = settings.onboardingComplete
            settings.lastSelectedProfileId?.let { prefs[KEY_SELECTED_PROFILE] = it }
        }
    }

    suspend fun setSelectedProfile(id: String) {
        dataStore.edit { it[KEY_SELECTED_PROFILE] = id }
    }

    suspend fun setOnboardingComplete() {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun updateDnsConfig(config: DnsConfig) {
        dataStore.edit { it[KEY_DNS_CONFIG] = gson.toJson(config) }
    }

    suspend fun updateRoutingConfig(config: RoutingConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_ROUTING_MODE] = config.mode.name
            prefs[KEY_PER_APP_MODE] = config.perAppMode.name
            prefs[KEY_BYPASSED_APPS] = gson.toJson(config.bypassedApps)
            prefs[KEY_PROXIED_APPS] = gson.toJson(config.proxiedApps)
        }
    }
}
