package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.MoodRepository
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.practices.model.MoodSource
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.practices.model.PracticeSessionId
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case для обновления фидбека по сессии (рейтинг, настроение после и заметка)
 * с дополнительным логированием настроения в историю пользователя.
 */
class UpdateSessionFeedbackUseCase(
    private val repository: PracticesRepository,
    private val moodRepository: MoodRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    suspend operator fun invoke(
        sessionId: PracticeSessionId,
        rating: Int?,
        moodAfter: MoodKind?,
        note: String?,
    ): AppResult<PracticeSession> = withContext(dispatcher) {
        val result = repository.updateSessionFeedback(
            sessionId = sessionId,
            rating = rating,
            moodAfter = moodAfter,
            feedbackNote = note,
        )

        if (moodAfter != null) {
            getCurrentUserIdUseCase().onSuccess { userId ->
                moodRepository.logMood(
                    userId = userId,
                    mood = moodAfter,
                    source = MoodSource.PRACTICE_AFTER,
                )
            }
        }

        result
    }
}
