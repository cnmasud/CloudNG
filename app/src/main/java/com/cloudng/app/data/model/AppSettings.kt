package com.cloudng.app.data.model

enum class AppTheme { SYSTEM, LIGHT, DARK }
enum class LogLevel { DEBUG, INFO, WARNING, ERROR, NONE }

data class AppSettings(
    val autoStartOnBoot: Boolean = false,
    val killSwitch: Boolean = true,
    val safeMode: Boolean = false,
    val socksPort: Int = 10808,
    val httpPort: Int = 10809,
    val mtu: Int = 1500,
    val allowLan: Boolean = false,
    val logLevel: LogLevel = LogLevel.WARNING,
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "system",
    val telemetryEnabled: Boolean = false,
    val onboardingComplete: Boolean = false,
    val lastSelectedProfileId: String? = null
)
