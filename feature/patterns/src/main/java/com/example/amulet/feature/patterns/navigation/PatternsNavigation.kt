package com.example.amulet.feature.patterns.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.amulet.feature.patterns.presentation.editor.PatternEditorRoute
import com.example.amulet.feature.patterns.presentation.editor.PatternElementEditorRoute
import com.example.amulet.feature.patterns.presentation.editor.PatternEditorViewModel
import com.example.amulet.feature.patterns.presentation.list.PatternsListRoute
import com.example.amulet.feature.patterns.presentation.preview.PatternPreviewRoute
import com.example.amulet.shared.domain.patterns.PreviewCache
import com.example.amulet.shared.domain.patterns.model.PatternSpec
import java.util.UUID

object PatternsGraph {
    const val route: String = "patterns_graph"
}

fun NavController.navigateToPatternPicker() {
    navigate(PatternsDestination.picker) {
        launchSingleTop = true
    }
}

object PatternsDestination {
    const val list: String = "patterns/list"
    const val picker: String = "patterns/picker"
    const val editor: String = "patterns/editor?patternId={patternId}"
    const val preview: String = "patterns/preview?patternId={patternId}&key={key}"
    const val editorTimeline: String = "patterns/editor/timeline"
}

fun NavController.navigateToPatternsList(popUpToInclusive: Boolean = false) {
    navigate(PatternsDestination.list) {
        if (popUpToInclusive) {
            popUpTo(PatternsDestination.list) { inclusive = true }
        }
        launchSingleTop = true
    }
}

fun NavController.navigateToPatternEditor(patternId: String? = null) {
    val route = if (patternId != null) {
        "patterns/editor?patternId=$patternId"
    } else {
        "patterns/editor"
    }
    navigate(route) {
        launchSingleTop = true
    }
}

fun NavController.navigateToPatternPreview(patternId: String) {
    navigate("patterns/preview?patternId=$patternId") {
        launchSingleTop = true
    }
}

fun NavController.navigateToPatternPreview(spec: PatternSpec) {
    val key = UUID.randomUUID().toString()
    PreviewCache.put(key, spec)
    navigate("patterns/preview?key=$key") {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.patternsGraph(
    navController: NavController,
) {
    // Константы для анимаций
    val animationDuration = 300
    
    navigation(startDestination = PatternsDestination.list, route = PatternsGraph.route) {
        // Список паттернов
        composable(
            route = PatternsDestination.list,

        ) {
            PatternsListRoute(
                onNavigateToEditor = { patternId ->
                    navController.navigateToPatternEditor(patternId)
                },
                onNavigateToPreview = { patternId ->
                    navController.navigateToPatternPreview(patternId)
                }
            )
        }

        composable(
            route = PatternsDestination.picker,
        ) {
            PatternsListRoute(
                pickerMode = true,
                onNavigateBack = { navController.popBackStack() },
                onPickPattern = { patternId ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedPatternId", patternId)
                    navController.popBackStack()
                },
                onNavigateToEditor = { patternId ->
                    if (patternId != null) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedPatternId", patternId)
                        navController.popBackStack()
                    }
                },
                onNavigateToPreview = { patternId ->
                    navController.navigateToPatternPreview(patternId)
                },
            )
        }

        // Редактор паттернов
        composable(
            route = PatternsDestination.editor,
            arguments = listOf(
                navArgument("patternId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            
        ) {
            PatternEditorRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = navController::navigateToPatternPreview,
                onNavigateToTimelineEditor = {
                    navController.navigate(PatternsDestination.editorTimeline)
                }
            )
        }

        // Полноэкранный редактор таймлайна
        composable(
            route = PatternsDestination.editorTimeline,
        ) {
            val parentEntry = navController.getBackStackEntry(PatternsDestination.editor)
            val vm: PatternEditorViewModel = hiltViewModel(parentEntry)
            PatternElementEditorRoute(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Предпросмотр паттерна
        composable(
            route = PatternsDestination.preview,
            arguments = listOf(
                navArgument("patternId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("key") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val patternId = backStackEntry.arguments?.getString("patternId")
            val previewKey = backStackEntry.arguments?.getString("key")
            PatternPreviewRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = {
                    patternId?.let { navController.navigateToPatternEditor(it) }
                }
            )
        }
    }
}

