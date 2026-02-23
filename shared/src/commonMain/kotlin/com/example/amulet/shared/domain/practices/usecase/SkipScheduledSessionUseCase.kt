package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.ScheduledSession
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.github.michaelbull.result.getOrElse

class SkipScheduledSessionUseCase(
    private val repository: PracticesRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(session: ScheduledSession): AppResult<Unit> {
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.skipScheduledSession(userId, session)
    }
}
