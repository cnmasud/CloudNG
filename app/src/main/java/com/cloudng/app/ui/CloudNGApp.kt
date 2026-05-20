package com.cloudng.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cloudng.app.ui.dns.DnsScreen
import com.cloudng.app.ui.home.HomeScreen
import com.cloudng.app.ui.logs.LogsScreen
import com.cloudng.app.ui.navigation.Screen
import com.cloudng.app.ui.navigation.bottomNavItems
import com.cloudng.app.ui.onboarding.OnboardingScreen
import com.cloudng.app.ui.profiles.ProfileEditScreen
import com.cloudng.app.ui.profiles.ProfilesScreen
import com.cloudng.app.ui.routing.PerAppProxyScreen
import com.cloudng.app.ui.routing.RoutingScreen
import com.cloudng.app.ui.settings.SettingsScreen
import com.cloudng.app.ui.subscriptions.SubscriptionsScreen
import com.cloudng.app.ui.theme.CloudNGTheme

@Composable
fun CloudNGAppRoot(onboardingComplete: Boolean) {
    CloudNGTheme {
        if (!onboardingComplete) {
            val navController = rememberNavController()
            NavHost(navController, startDestination = Screen.Onboarding.route) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Home.route) { MainScaffold() }
            }
        } else {
            MainScaffold()
        }
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val topLevelRoutes = bottomNavItems.map { it.screen.route }.toSet()
    val showBottomBar = currentDest?.route in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDest?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToProfiles = {
                        navController.navigate(Screen.Profiles.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Profiles.route) {
                ProfilesScreen(
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.ProfileEdit.withId(id))
                    },
                    onProfileSelected = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = Screen.ProfileEdit.route,
                arguments = listOf(
                    androidx.navigation.navArgument("profileId") {
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backEntry ->
                val profileId = backEntry.arguments?.getString("profileId")
                ProfileEditScreen(
                    profileId = profileId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Subscriptions.route) {
                SubscriptionsScreen()
            }
            composable(Screen.Routing.route) {
                RoutingScreen(
                    onNavigateToPerApp = { navController.navigate(Screen.PerAppProxy.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PerAppProxy.route) {
                PerAppProxyScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Dns.route) {
                DnsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Logs.route) {
                LogsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToDns = { navController.navigate(Screen.Dns.route) },
                    onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
