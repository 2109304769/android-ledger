package com.androidledger.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.androidledger.ui.addtransaction.AddTransactionScreen
import com.androidledger.ui.csvimport.CsvImportScreen
import com.androidledger.ui.dashboard.DashboardScreen
import com.androidledger.ui.quickentry.QuickEntryScreen
import com.androidledger.ui.settings.SettingsScreen
import com.androidledger.ui.transactiondetail.TransactionDetailScreen
import com.androidledger.ui.transactions.TransactionsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "\u9996\u9875",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object AddTransaction : Screen(
        route = "add_transaction",
        title = "\u8BB0\u8D26",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircle
    )

    data object Transactions : Screen(
        route = "transactions",
        title = "\u8D26\u5355",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    )

    data object Settings : Screen(
        route = "settings",
        title = "\u8BBE\u7F6E",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.AddTransaction,
    Screen.Transactions,
    Screen.Settings
)

@Composable
fun AppNavigation(startRoute: String = Screen.Dashboard.route) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "quick_entry") {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(text = screen.title) },
                            selected = selected,
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
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToTransaction = { transactionId ->
                        navController.navigate("transaction_detail/$transactionId")
                    },
                    onNavigateToAddTransaction = {
                        navController.navigate(Screen.AddTransaction.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToQuickEntry = {
                        navController.navigate("quick_entry")
                    }
                )
            }
            composable(Screen.AddTransaction.route) {
                AddTransactionScreen()
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    onNavigateToTransaction = { transactionId ->
                        navController.navigate("transaction_detail/$transactionId")
                    },
                    onNavigateToCsvImport = {
                        navController.navigate("csv_import")
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = "transaction_detail/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
            ) {
                TransactionDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("csv_import") {
                CsvImportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("quick_entry") {
                QuickEntryScreen(
                    onNavigateToStandard = {
                        navController.navigate(Screen.AddTransaction.route) {
                            popUpTo("quick_entry") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
