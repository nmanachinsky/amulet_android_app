package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.user.model.UserPreferences
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.github.michaelbull.result.getOrElse

class UpdateUserPreferencesUseCase(
    private val repository: PracticesRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(preferences: UserPreferences): AppResult<Unit> {
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.updateUserPreferences(userId, preferences)
    }
}
