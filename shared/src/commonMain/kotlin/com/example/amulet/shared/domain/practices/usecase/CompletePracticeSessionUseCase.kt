package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.courses.usecase.CompleteCourseItemUseCase
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.practices.model.PracticeSessionSource
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Use case завершения активной сессии практики с логированием рейтинга и заметки.
 */
class CompletePracticeSessionUseCase(
    private val repository: PracticesRepository,
    private val completeCourseItemUseCase: CompleteCourseItemUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    suspend operator fun invoke(
        rating: Int?,
        note: String?,
    ): AppResult<Unit> = withContext(dispatcher) {
        val userIdResult = getCurrentUserIdUseCase()
        if (userIdResult.isErr) {
            return@withContext Err(userIdResult.error)
        }
        val userId = if(userIdResult.isOk) userIdResult.value else return@withContext Err(AppError.Unauthorized)

        val session = repository.getActiveSessionStream(userId).firstOrNull()
            ?: return@withContext Err(AppError.NotFound)

        val stopped = repository.stopSession(session.id, completed = true)
        val completedSession = stopped.component1()
            ?: return@withContext stopped.map { }

        onSessionCompleted(completedSession)

        repository.updateSessionFeedback(
            sessionId = session.id,
            rating = rating,
            moodAfter = null,
            feedbackNote = note,
        ).map { }
    }

    private suspend fun onSessionCompleted(session: PracticeSession) {
        when (val src = session.source) {
            is PracticeSessionSource.FromCourse -> {
                completeCourseItemUseCase(
                    courseId = src.courseId,
                    itemId = src.itemId,
                )
            }
            is PracticeSessionSource.FromSchedule,
            is PracticeSessionSource.ScheduleSkip,
            PracticeSessionSource.Manual,
            is PracticeSessionSource.Unknown,
            null -> Unit
        }
    }
}
