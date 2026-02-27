package com.example.amulet.feature.hugs.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.amulet.feature.hugs.presentation.details.HugDetailsRoute
import com.example.amulet.feature.hugs.presentation.emotions.HugsEmotionsRoute
import com.example.amulet.feature.hugs.presentation.emotions.editor.HugsEmotionEditorRoute
import com.example.amulet.feature.hugs.presentation.main.HugsRoute
import com.example.amulet.feature.hugs.presentation.pairing.HugsPairingRoute
import com.example.amulet.feature.hugs.presentation.settings.HugsSettingsRoute
import com.example.amulet.feature.patterns.navigation.navigateToPatternEditor
import com.example.amulet.feature.patterns.navigation.navigateToPatternPicker

object HugsGraph {
    const val route: String = "hugs_graph"
}

object HugsDestination {
    const val main: String = "hugs/main"
    const val settings: String = "hugs/settings"
    const val emotions: String = "hugs/emotions"
    const val emotionEditor: String = "hugs/emotions/editor?emotionId={emotionId}"
    const val pairing: String = "hugs/pairing"
    const val details: String = "hugs/details/{hugId}"
}

fun NavController.navigateToHugs(popUpToInclusive: Boolean = false) {
    navigate(HugsDestination.main) {
        if (popUpToInclusive) {
            popUpTo(HugsDestination.main) { inclusive = true }
        }
        launchSingleTop = true
    }
}


fun NavController.navigateToHugsSettings() {
    navigate(HugsDestination.settings) {
        launchSingleTop = true
    }
}

fun NavController.navigateToHugsEmotions() {
    navigate(HugsDestination.emotions) {
        launchSingleTop = true
    }
}

fun NavController.navigateToHugsEmotionEditor(emotionId: String? = null) {
    val route = if (emotionId == null) {
        "hugs/emotions/editor?emotionId="
    } else {
        "hugs/emotions/editor?emotionId=$emotionId"
    }
    navigate(route) {
        launchSingleTop = true
    }
}

fun NavController.navigateToHugsPairing() {
    navigate(HugsDestination.pairing) {
        launchSingleTop = true
    }
}

fun NavController.navigateToHugDetails(hugId: String) {
    navigate("hugs/details/$hugId") {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.hugsGraph(
    navController: NavController,
) {
    navigation(startDestination = HugsDestination.main, route = HugsGraph.route) {
        composable(
            route = HugsDestination.main,
            deepLinks = listOf(
                navDeepLink { uriPattern = "amulet://hugs" },
                navDeepLink { uriPattern = "https://amulet.app/hugs" },
                navDeepLink { uriPattern = "https://amuletinvite.vercel.app/hugs" }
            )
        ) {
            HugsRoute(
                onOpenSettings = { navController.navigateToHugsSettings() },
                onOpenEmotions = { navController.navigateToHugsEmotions() },
                onOpenPairing = { navController.navigateToHugsPairing() },
            )
        }

        composable(route = HugsDestination.settings) {
            HugsSettingsRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = HugsDestination.emotions) {
            HugsEmotionsRoute(
                onNavigateBack = { navController.popBackStack() },
                onOpenEmotionEditor = { emotionId ->
                    navController.navigateToHugsEmotionEditor(emotionId)
                },
            )
        }

        composable(
            route = HugsDestination.emotionEditor,
            arguments = listOf(
                navArgument("emotionId") { type = NavType.StringType; nullable = true },
            )
        ) { backStackEntry ->
            val selectedPatternId by backStackEntry
                .savedStateHandle
                .getStateFlow<String?>("selectedPatternId", null)
                .collectAsStateWithLifecycle()

            HugsEmotionEditorRoute(
                onNavigateBack = { navController.popBackStack() },
                onOpenPatternPicker = {
                    navController.navigateToPatternPicker()
                },
                selectedPatternId = selectedPatternId,
                onConsumeSelectedPatternId = {
                    backStackEntry.savedStateHandle["selectedPatternId"] = null
                }
            )
        }

        composable(
            route = HugsDestination.pairing + "?code={code}&inviterName={inviterName}",
            arguments = listOf(
                navArgument("code") { type = NavType.StringType; nullable = true },
                navArgument("inviterName") { type = NavType.StringType; nullable = true },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "amulet://hugs/pair?code={code}&inviterName={inviterName}" },
                navDeepLink { uriPattern = "https://amulet.app/hugs/pair?code={code}&inviterName={inviterName}" },
                navDeepLink { uriPattern = "https://amuletinvite.vercel.app/hugs/pair?code={code}&inviterName={inviterName}" },
            )
        ) {
            HugsPairingRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = HugsDestination.details,
            arguments = listOf(
                navArgument("hugId") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "amulet://hugs/{hugId}" },
                navDeepLink { uriPattern = "https://amulet.app/hugs/{hugId}" }
            )
        ) { backStackEntry ->
            val hugId = backStackEntry.arguments?.getString("hugId") ?: return@composable
            HugDetailsRoute(hugId = hugId)
        }
    }
}
