package dev.xunnet.client

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.xunnet.client.ui.screens.ProxiesScreen
import dev.xunnet.client.ui.screens.SettingsScreen
import dev.xunnet.client.ui.screens.SubscriptionsScreen

private data class BottomNavItem(val screen: Screen, val labelRes: Int, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XunnetApp() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem(Screen.Dashboard, R.string.nav_dashboard, Icons.Default.Home),
        BottomNavItem(Screen.Proxies, R.string.nav_proxies, Icons.Default.Storage)
    )

    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_subscriptions)) },
                            onClick = {
                                menuOpen = false
                                navController.navigate(Screen.Subscriptions.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_settings)) },
                            onClick = {
                                menuOpen = false
                                navController.navigate(Screen.Settings.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                )
            )
        },
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
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
