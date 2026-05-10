package com.readtrack.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.readtrack.presentation.ui.addbook.AddBookScreen
import com.readtrack.presentation.ui.booklist.BookListDetailScreen
import com.readtrack.presentation.ui.booklist.BookListScreen
import com.readtrack.presentation.ui.books.BookDetailScreen
import com.readtrack.presentation.ui.books.BooksScreen
import com.readtrack.presentation.ui.home.HomeScreen
import com.readtrack.presentation.ui.settings.BackupSettingsScreen
import com.readtrack.presentation.ui.settings.AboutScreen
import com.readtrack.presentation.ui.settings.SettingsScreen
import com.readtrack.presentation.ui.settings.WidgetSettingsScreen
import com.readtrack.presentation.ui.stats.StatsScreen
import com.readtrack.presentation.ui.readinghistory.ReadingHistoryScreen
import com.readtrack.presentation.viewmodel.AddBookViewModel

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
    data object BookList : Screen("book_lists", "书单收藏夹", Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark)
    data object BookListDetail : Screen("book_list/{bookListId}", "书单详情", Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark) {
        fun createRoute(bookListId: Long) = "book_list/$bookListId"
    }
    data object ReadingHistory : Screen("reading_history", "阅读历史", Icons.Filled.DateRange, Icons.Outlined.DateRange)
    data object BackupSettings : Screen("backup_settings", "备份与恢复", Icons.Filled.CloudSync, Icons.Outlined.CloudSync)
    data object About : Screen("about", "关于", Icons.Filled.Info, Icons.Outlined.Info)
    data object WidgetSettings : Screen("widget_settings", "桌面小组件", Icons.Filled.Widgets, Icons.Outlined.Widgets)
}

@Composable
fun MainNavigation(
    pendingBookId: Long? = null,
    onPendingBookIdConsumed: () -> Unit = {},
    pendingWidgetSettings: Boolean = false,
    onPendingWidgetSettingsConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val bottomNavItems = listOf(Screen.Home, Screen.Books, Screen.Stats, Screen.Settings)

    LaunchedEffect(pendingBookId) {
        if (pendingBookId != null) {
            navController.navigate(Screen.BookDetail.createRoute(pendingBookId))
            onPendingBookIdConsumed()
        }
    }

    LaunchedEffect(pendingWidgetSettings) {
        if (pendingWidgetSettings) {
            navController.navigate(Screen.WidgetSettings.route)
            onPendingWidgetSettingsConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            val showBottomBar = currentDestination?.route?.let { route ->
                route in bottomNavItems.map { it.route }
            } ?: true

            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
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
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) +
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250)) +
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(250))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) +
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(300))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(250)) +
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(250))
            }
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
                    },
                    onBookListClick = {
                        navController.navigate(Screen.BookList.route)
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
                    onReadingHistoryClick = {
                        navController.navigate(Screen.ReadingHistory.route)
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToBackupSettings = {
                        navController.navigate(Screen.BackupSettings.route)
                    },
                    onNavigateToWidgetSettings = {
                        navController.navigate(Screen.WidgetSettings.route)
                    },
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route)
                    }
                )
            }

            composable(Screen.BookList.route) {
                BookListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onBookListClick = { bookListId ->
                        navController.navigate(Screen.BookListDetail.createRoute(bookListId))
                    }
                )
            }

            composable(
                route = Screen.BookListDetail.route,
                arguments = listOf(navArgument("bookListId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookListId = backStackEntry.arguments?.getLong("bookListId") ?: return@composable
                BookListDetailScreen(
                    bookListId = bookListId,
                    onNavigateBack = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    }
                )
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
                val viewModel: AddBookViewModel = hiltViewModel()
                
                AddBookScreen(
                    onNavigateBack = { navController.popBackStack() },
                    bookId = null,
                    onPickCover = { },
                    viewModel = viewModel
                )
            }
            
            composable(
                route = Screen.EditBook.route,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
                val viewModel: AddBookViewModel = hiltViewModel()

                AddBookScreen(
                    onNavigateBack = { navController.popBackStack() },
                    bookId = bookId,
                    onPickCover = { },
                    viewModel = viewModel
                )
            }

            composable(Screen.ReadingHistory.route) {
                ReadingHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    }
                )
            }

            composable(Screen.BackupSettings.route) {
                BackupSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.WidgetSettings.route) {
                WidgetSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
