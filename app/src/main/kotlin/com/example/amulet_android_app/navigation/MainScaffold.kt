package com.example.amulet_android_app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.feature.dashboard.navigation.navigateToDashboard
import com.example.amulet.feature.practices.navigation.navigateToPracticesHome
import com.example.amulet.feature.hugs.navigation.navigateToHugs
import com.example.amulet.feature.patterns.navigation.navigateToPatternsList
import com.example.amulet_android_app.navigation.navigateToSettings
import com.example.amulet_android_app.R
import com.example.amulet.core.design.components.navigation.AmuletBottomNavigationBar
import com.example.amulet.core.design.components.navigation.BottomNavItem
import com.example.amulet.core.design.scaffold.LocalScaffoldState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Scaffold с упрощенным управлением через ScaffoldState.
 * 
 * Использует глобальный LocalScaffoldState, предоставленный на уровне AmuletApp.
 * 
 * Управление:
 * - Bottom bar устанавливается автоматически для основных экранов
 * - Top bar, FAB и другие элементы управляются напрямую из экранов через scaffoldState.updateConfig
 * - Цвета берутся из темы Material 3 автоматически
 * - При переходах навигационный граф обнуляет конфиг (scaffoldState.reset())
 *
 * @param navController NavHostController для навигации
 * @param modifier Modifier для scaffold
 * @param content Контент приложения (обычно NavHost)
 */
@Composable
fun MainScaffold(
    navController: NavHostController,
    authState: AuthState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scaffoldState = LocalScaffoldState.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Управляем только bottom bar для основных экранов
    // Все остальное (topBar, FAB) управляется напрямую из экранов
    LaunchedEffect(currentRoute) {
        val showBottomBar = currentRoute != null && shouldShowBottomBar(currentRoute)
        
        scaffoldState.updateConfig {
            copy(
                bottomBar = if (showBottomBar) {
                    {
                        AppBottomNavigationBar(
                            navController = navController,
                            currentRoute = currentRoute.orEmpty(),
                            isGuest = authState is AuthState.Guest
                        )
                    }
                } else {
                    {}
                }
            )
        }
    }

    val config = scaffoldState.config

    // Scaffold без цветов - берет автоматически из Material 3 темы
    Scaffold(
        modifier = modifier,
        topBar = config.topBar,
        bottomBar = config.bottomBar,
        snackbarHost = config.snackbarHost,
        floatingActionButton = config.floatingActionButton,
        floatingActionButtonPosition = config.floatingActionButtonPosition,
        contentWindowInsets = config.contentWindowInsets
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}

/**
 * Bottom Navigation Bar приложения.
 * Показывается на главных экранах (dashboard, practices, hugs, patterns, settings).
 */
@Composable
private fun AppBottomNavigationBar(
    navController: NavHostController,
    currentRoute: String,
    isGuest: Boolean
) {
    val scaffoldState = LocalScaffoldState.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(isGuest) {
        if (isGuest) {
            scaffoldState.updateConfig {
                copy(snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                })
            }
        }
    }

    fun onHugsClick() {
        if (isGuest) {
            CoroutineScope(Dispatchers.Main).launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.guest_hugs_restricted),
                    duration = SnackbarDuration.Short
                )
            }
        } else {
            navController.navigateToHugs(popUpToInclusive = true)
        }
    }

    AmuletBottomNavigationBar(
        items = getBottomNavItems(isGuest),
        selectedRoute = currentRoute,
        onItemSelected = { item ->
            when (item.route) {
                "dashboard/main" -> navController.navigateToDashboard(popUpToInclusive = true)
                "practices/home" -> navController.navigateToPracticesHome(popUpToInclusive = true)
                "hugs/main" -> onHugsClick()
                "patterns/list" -> navController.navigateToPatternsList()
                "settings/main" -> navController.navigateToSettings()
            }
        }
    )
}

/**
 * Проверяет, нужно ли показывать bottom bar для данного route.
 */
private fun shouldShowBottomBar(route: String): Boolean {
    // Bottom bar показывается только на основных экранах
    return route.startsWith("dashboard") ||
           route.startsWith("practices") ||
           route.startsWith("hugs") ||
           route.startsWith("patterns") ||
           route.startsWith("settings")
}

/**
 * Получить список элементов bottom navigation
 */
@Composable
private fun getBottomNavItems(isGuest: Boolean) = listOf(
    BottomNavItem(
        route = "dashboard/main",
        icon = Icons.Default.Dashboard,
        label = stringResource(R.string.bottom_nav_home)
    ),
    BottomNavItem(
        route = "practices/home",
        icon = Icons.Default.MenuBook,
        label = stringResource(R.string.bottom_nav_library)
    ),
    BottomNavItem(
        route = "hugs/main",
        icon = Icons.Default.EmojiPeople,
        label = stringResource(R.string.bottom_nav_hugs),
        enabled = !isGuest
    ),
    BottomNavItem(
        route = "patterns/list",
        icon = Icons.Default.AutoGraph,
        label = stringResource(R.string.bottom_nav_patterns)
    ),
    BottomNavItem(
        route = "settings/main",
        icon = Icons.Default.Settings,
        label = stringResource(R.string.bottom_nav_settings)
    )
)
