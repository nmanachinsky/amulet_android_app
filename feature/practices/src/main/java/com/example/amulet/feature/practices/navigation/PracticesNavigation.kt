package com.example.amulet.feature.practices.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.example.amulet.feature.practices.presentation.home.PracticesHomeRoute
import com.example.amulet.feature.practices.presentation.course.CourseDetailsRoute
import com.example.amulet.feature.practices.presentation.details.PracticeDetailsRoute
import com.example.amulet.feature.practices.presentation.search.PracticesSearchRoute
import com.example.amulet.feature.practices.presentation.session.PracticeSessionRoute
import com.example.amulet.feature.practices.presentation.calendar.CalendarRoute
import com.example.amulet.feature.practices.presentation.editor.PracticeEditorRoute

object PracticesGraph {
    const val route: String = "practices_graph"
}

object PracticesDestination {
    const val home: String = "practices/home"
    const val practiceDetails: String = "practices/practice/{practiceId}"
    const val courseDetails: String = "practices/course/{courseId}"
    const val search: String = "practices/search"
    const val calendar: String = "practices/calendar"
    const val practiceSession: String = "practices/session/{practiceId}"
    const val practiceEditor: String = "practices/editor?practiceId={practiceId}"
}

fun NavController.navigateToPracticesHome(popUpToInclusive: Boolean = false) {
    navigate(PracticesDestination.home) {
        if (popUpToInclusive) {
            popUpTo(PracticesDestination.home) { inclusive = true }
        }
        launchSingleTop = true
    }
}

fun NavController.navigateToPracticeDetails(practiceId: String) {
    navigate("practices/practice/$practiceId") { launchSingleTop = true }
}

fun NavController.navigateToPracticeSession(practiceId: String) {
    navigate("practices/session/$practiceId") { launchSingleTop = true }
}

fun NavController.navigateToPracticeEditor(practiceId: String? = null) {
    val route = if (practiceId != null) {
        "practices/editor?practiceId=$practiceId"
    } else {
        "practices/editor"
    }
    navigate(route) { launchSingleTop = true }
}

fun NavController.navigateToCourseDetails(courseId: String) {
    navigate("practices/course/$courseId") { launchSingleTop = true }
}

fun NavController.navigateToPracticeSchedule(practiceId: String) {
    navigateToCalendarWithPractice(practiceId)
}

fun NavController.navigateToPracticesSearch() {
    navigate(PracticesDestination.search) { launchSingleTop = true }
}

fun NavController.navigateToCalendar() {
    navigate(PracticesDestination.calendar) { launchSingleTop = true }
}

fun NavController.navigateToCalendarWithPractice(practiceId: String) {
    navigate("${PracticesDestination.calendar}?practiceId=$practiceId") { launchSingleTop = true }
}

fun NavGraphBuilder.practicesGraph(
    navController: NavController,
    onNavigateToPairing: () -> Unit,
) {
    navigation(startDestination = PracticesDestination.home, route = PracticesGraph.route) {
        composable(route = PracticesDestination.home) {
            PracticesHomeRoute(
                onOpenPractice = { id -> navController.navigateToPracticeDetails(id) },
                onOpenSession = { id -> navController.navigateToPracticeSession(id) },
                onOpenCourse = { id -> navController.navigateToCourseDetails(id) },
                onOpenSchedule = { navController.navigateToCalendar() },
                onOpenSearch = { navController.navigateToPracticesSearch() },
                onOpenPracticeEditor = { navController.navigateToPracticeEditor() },
            )
        }
        composable(route = PracticesDestination.practiceDetails) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("practiceId") ?: return@composable
            PracticeDetailsRoute(
                practiceId = id,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPattern = { patternId -> navController.navigate("patterns/preview?patternId=$patternId") },
                onNavigateToPlan = { practiceIdForPlan -> navController.navigateToPracticeSchedule(practiceIdForPlan) },
                onNavigateToCourse = { courseId -> navController.navigateToCourseDetails(courseId) },
                onNavigateToPairing = onNavigateToPairing,
                onNavigateToSession = { practiceIdForSession -> navController.navigateToPracticeSession(practiceIdForSession) },
                onNavigateToEdit = { practiceIdForEdit -> navController.navigateToPracticeEditor(practiceIdForEdit) },
            )
        }
        composable(
            route = PracticesDestination.practiceEditor,
            arguments = listOf(
                navArgument("practiceId") {
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("practiceId")
            PracticeEditorRoute(
                practiceId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = PracticesDestination.practiceSession,
            deepLinks = listOf(
                navDeepLink { uriPattern = "amulet://practices/session/{practiceId}" },
                navDeepLink { uriPattern = "https://amulet.app/practices/session/{practiceId}" },
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("practiceId") ?: return@composable
            PracticeSessionRoute(
                practiceId = id,
                onNavigateBack = { navController.popBackStack() },
                onOpenSchedule = { practiceIdForPlan -> navController.navigateToPracticeSchedule(practiceIdForPlan) }
            )
        }
        composable(route = PracticesDestination.courseDetails) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("courseId") ?: return@composable
            CourseDetailsRoute(
                courseId = id,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSchedule = { navController.navigateToCalendar() },
                onOpenPractice = { practiceId -> navController.navigateToPracticeDetails(practiceId) }
            )
        }
        composable(route = PracticesDestination.search) {
            PracticesSearchRoute(
                onOpenPractice = { id -> navController.navigateToPracticeDetails(id) },
                onOpenCourse = { id -> navController.navigateToCourseDetails(id) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${PracticesDestination.calendar}?practiceId={practiceId}",
            arguments = listOf(
                navArgument("practiceId") {
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val practiceId = backStackEntry.arguments?.getString("practiceId")
            CalendarRoute(
                initialPracticeId = practiceId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPractice = { id ->
                    navController.navigateToPracticeSession(id)
                },
                onNavigateToCourse = { id ->
                    navController.navigateToCourseDetails(id)
                }
            )
        }
    }
}
