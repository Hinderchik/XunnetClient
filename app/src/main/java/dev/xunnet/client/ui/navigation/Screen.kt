package dev.xunnet.client.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Proxies : Screen("proxies")
    data object Subscriptions : Screen("subscriptions")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
}
