package com.example.amulet.shared.domain.user.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.shared.domain.user.repository.UserRepository

/**
 * UseCase для сохранения локальных настроек пользователя (часовой пояс, язык).
 *
 * Значения хранятся только локально и не синхронизируются с backend.
 * Пустые строки нормализуются в null, чтобы в кэше не оставались "пустые" настройки.
 */
class UpdateLocalUserPreferencesUseCase(
    private val userRepository: UserRepository,
) {

    suspend operator fun invoke(
        userId: UserId,
        timezone: String?,
        language: String?,
    ): AppResult<Unit> {
        return userRepository.updateLocalPreferences(
            userId = userId,
            timezone = timezone?.takeIf { it.isNotBlank() },
            language = language?.takeIf { it.isNotBlank() },
        )
    }
}
