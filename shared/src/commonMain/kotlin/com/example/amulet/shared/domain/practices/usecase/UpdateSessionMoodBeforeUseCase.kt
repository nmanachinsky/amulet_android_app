package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.MoodRepository
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.github.michaelbull.result.onSuccess
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.practices.model.MoodSource
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.practices.model.PracticeSessionId

class UpdateSessionMoodBeforeUseCase(
    private val practicesRepository: PracticesRepository,
    private val moodRepository: MoodRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
) {

    suspend operator fun invoke(
        sessionId: PracticeSessionId,
        moodBefore: MoodKind?,
    ): AppResult<PracticeSession> {
        val result = practicesRepository.updateSessionMoodBefore(
            sessionId = sessionId,
            moodBefore = moodBefore,
        )

        if (moodBefore != null) {
            getCurrentUserIdUseCase().onSuccess { userId ->
                moodRepository.logMood(
                    userId = userId,
                    mood = moodBefore,
                    source = MoodSource.PRACTICE_BEFORE,
                )
            }
        }

        return result
    }
}
