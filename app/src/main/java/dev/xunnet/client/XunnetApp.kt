package dev.xunnet.client

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.xunnet.client.ui.navigation.Screen
import dev.xunnet.client.ui.screens.DashboardScreen
import dev.xunnet.client.ui.screens.FederationScreen
import dev.xunnet.client.ui.screens.ProxiesScreen
import dev.xunnet.client.ui.screens.SettingsScreen
import dev.xunnet.client.ui.screens.SubscriptionsScreen

private data class BottomNavItem(val screen: Screen, val labelRes: Int, val icon: ImageVector)

@Composable
fun XunnetApp() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem(Screen.Dashboard, R.string.nav_dashboard, Icons.Default.Home),
        BottomNavItem(Screen.Proxies, R.string.nav_proxies, Icons.Default.Storage),
        BottomNavItem(Screen.Subscriptions, R.string.nav_subscriptions, Icons.Default.RssFeed),
        BottomNavItem(Screen.Federation, R.string.nav_federation, Icons.Default.Language),
        BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { (screen, labelRes, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(labelRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Proxies.route) { ProxiesScreen() }
            composable(Screen.Subscriptions.route) { SubscriptionsScreen() }
            composable(Screen.Federation.route) { FederationScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
