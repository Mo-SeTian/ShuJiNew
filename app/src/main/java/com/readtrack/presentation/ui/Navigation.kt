package com.readtrack.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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

sealed class Screen(
    val route: String, 
    val title: String, 
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "首页", Icons.Filled.Home, Icons.Outlined.Home)
    data object Books : Screen("books", "我的书籍", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    data object Stats : Screen("stats", "统计", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    data object Settings : Screen("settings", "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object BookDetail : Screen("book/{bookId}", "书籍详情", Icons.Filled.Book, Icons.Outlined.Book) {
        fun createRoute(bookId: Long) = "book/$bookId"
    }
    data object AddBook : Screen("add_book", "添加书籍", Icons.Filled.Add, Icons.Outlined.Add)
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
                route in bottomNavItems.map { it.route }
            } ?: true

            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
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
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
                BookDetailScreen(
                    bookId = bookId,
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
