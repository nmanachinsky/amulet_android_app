package com.example.amulet.shared.domain.privacy.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.user.repository.UserRepository

/**
 * Запускает процесс удаления аккаунта пользователя.
 */
class RequestAccountDeletionUseCase(
    private val userRepository: UserRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        val userId = getCurrentUserIdUseCase().component1() ?: return AppResult(AppError.Unauthorized)
        return userRepository.requestAccountDeletion(userId)
    }
}
