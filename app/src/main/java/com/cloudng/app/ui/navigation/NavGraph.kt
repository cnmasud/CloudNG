package com.cloudng.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Onboarding   : Screen("onboarding")
    object Home         : Screen("home")
    object Profiles     : Screen("profiles")
    object ProfileEdit  : Screen("profile_edit?profileId={profileId}") {
        fun withId(id: String?) = if (id != null) "profile_edit?profileId=$id" else "profile_edit"
    }
    object Subscriptions : Screen("subscriptions")
    object Routing      : Screen("routing")
    object PerAppProxy  : Screen("per_app_proxy")
    object Dns          : Screen("dns")
    object Logs         : Screen("logs")
    object Settings     : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,          "Home",     Icons.Default.Home),
    BottomNavItem(Screen.Profiles,      "Profiles", Icons.AutoMirrored.Filled.List),
    BottomNavItem(Screen.Subscriptions, "Subs",     Icons.Default.Refresh),
    BottomNavItem(Screen.Routing,       "Routing",  Icons.Default.AccountTree),
    BottomNavItem(Screen.Settings,      "Settings", Icons.Default.Settings)
)
