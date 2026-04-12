package com.readtrack.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.readtrack.presentation.ui.addbook.CoverPickerScreen
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
    data object EditBook : Screen("edit_book/{bookId}", "编辑书籍", Icons.Filled.Edit, Icons.Outlined.Edit) {
        fun createRoute(bookId: Long) = "edit_book/$bookId"
    }
    data object CoverPicker : Screen("cover_picker?coverUri={coverUri}", "选择封面", Icons.Filled.Image, Icons.Outlined.Image) {
        fun createRoute(coverUri: String? = null) = 
            if (coverUri != null) "cover_picker?coverUri=$coverUri" else "cover_picker"
    }
}

private val animationSpec = tween<Float>(durationMillis = 150)

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(Screen.Home, Screen.Books, Screen.Stats, Screen.Settings)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // 只在主页面显示底部导航
            val showBottomBar = bottomNavItems.any { screen ->
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            }
            
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) screen.selectedIcon else screen.unselectedIcon,
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
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec) + slideInHorizontally(animationSpec) { it / 4 } },
            exitTransition = { fadeOut(animationSpec) + slideOutHorizontally(animationSpec) { -it / 4 } },
            popEnterTransition = { fadeIn(animationSpec) + slideInHorizontally(animationSpec) { -it / 4 } },
            popExitTransition = { fadeOut(animationSpec) + slideOutHorizontally(animationSpec) { it / 4 } }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToBook = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    },
                    onNavigateToBooks = {
                        navController.navigate(Screen.Books.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            composable(Screen.Books.route) {
                BooksScreen(
                    onNavigateToBook = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    },
                    onAddBook = {
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
                val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
                BookDetailScreen(
                    bookId = bookId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditBook = {
                        navController.navigate(Screen.EditBook.createRoute(bookId))
                    }
                )
            }
            
            composable(Screen.AddBook.route) {
                AddBookScreen(
                    navController = navController,
                    bookId = null
                )
            }
            
            composable(
                route = Screen.EditBook.route,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getLong("bookId")
                AddBookScreen(
                    navController = navController,
                    bookId = bookId
                )
            }
            
            composable(
                route = Screen.CoverPicker.route,
                arguments = listOf(
                    navArgument("coverUri") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val coverUri = backStackEntry.arguments?.getString("coverUri")
                
                CoverPickerScreen(
                    initialCoverUri = coverUri,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
