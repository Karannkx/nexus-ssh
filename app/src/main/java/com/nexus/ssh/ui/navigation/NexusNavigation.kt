package com.nexus.ssh.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.nexus.ssh.ui.screens.dashboard.DashboardScreen
import com.nexus.ssh.ui.screens.servers.AddServerScreen
import com.nexus.ssh.ui.screens.terminal.TerminalScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Terminal : Screen("terminal", "Terminal", Icons.Default.Terminal)
    object Files : Screen("files", "Files", Icons.Default.Folder)
    object Docker : Screen("docker", "Docker", Icons.Default.Layers)
    object Services : Screen("services", "Services", Icons.Default.Build)
    object Monitoring : Screen("monitoring", "Monitoring", Icons.Default.Analytics)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AddServer : Screen("add_server", "Add Server", Icons.Default.Add)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Terminal,
    Screen.Files,
    Screen.Docker,
    Screen.Services,
    Screen.Monitoring,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.AddServer.route) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToAddServer = { navController.navigate(Screen.AddServer.route) },
                    onNavigateToTerminal = { navController.navigate(Screen.Terminal.route) }
                )
            }
            composable(Screen.Terminal.route) {
                TerminalScreen()
            }
            composable(Screen.AddServer.route) {
                AddServerScreen(
                    onDismiss = { navController.popBackStack() },
                    onServerSaved = { navController.popBackStack() }
                )
            }
            composable(Screen.Files.route) {
                // SFTP File manager destination
                DashboardScreen(onNavigateToAddServer = {}, onNavigateToTerminal = {})
            }
            composable(Screen.Docker.route) {
                DashboardScreen(onNavigateToAddServer = {}, onNavigateToTerminal = {})
            }
            composable(Screen.Services.route) {
                DashboardScreen(onNavigateToAddServer = {}, onNavigateToTerminal = {})
            }
            composable(Screen.Monitoring.route) {
                DashboardScreen(onNavigateToAddServer = {}, onNavigateToTerminal = {})
            }
            composable(Screen.Settings.route) {
                DashboardScreen(onNavigateToAddServer = {}, onNavigateToTerminal = {})
            }
        }
    }
}
