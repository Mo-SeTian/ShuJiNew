package com.readtrack.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.readtrack.presentation.ui.addbook.AddBookScreen
import com.readtrack.presentation.ui.books.BookDetailScreen
import com.readtrack.presentation.ui.books.BooksScreen
import com.readtrack.presentation.ui.home.HomeScreen
import com.readtrack.presentation.ui.settings.SettingsScreen
import com.readtrack.presentation.ui.stats.StatsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Books : Screen("books", "我的书籍", Icons.Default.MenuBook)
    data object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
    data object BookDetail : Screen("book/{bookId}", "书籍详情", Icons.Default.Book) {
        fun createRoute(bookId: Long) = "book/$bookId"
    }
    data object AddBook : Screen("add_book", "添加书籍", Icons.Default.Add)
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(Screen.Home, Screen.Books, Screen.Stats, Screen.Settings)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Hide bottom bar for detail screens
            val showBottomBar = currentDestination?.route?.let { route ->
                bottomNavItems.any { it.route == route }
            } ?: true

            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    }
                )
            }

            composable(Screen.Books.route) {
                BooksScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    },
                    onAddBookClick = {
                        navController.navigate(Screen.AddBook.route)
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) {
                BookDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddBook.route) {
                AddBookScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
