package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.MoodRepository
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.practices.model.MoodSource
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.github.michaelbull.result.getOrElse

/**
 * Use case для логирования выбора настроения пользователем.
 * По умолчанию считается, что источник — экран Practices Home.
 */
class LogMoodSelectionUseCase(
    private val repository: MoodRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {

    suspend operator fun invoke(
        mood: MoodKind,
        source: MoodSource = MoodSource.PRACTICES_HOME,
    ): AppResult<Unit> {
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.logMood(userId = userId, mood = mood, source = source)
    }
}
