package com.example.amulet.shared.domain.auth.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.repository.AuthRepository
import com.example.amulet.shared.domain.user.repository.UserRepository

class EnableGuestModeUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        val result = authRepository.startGuestSession()
        if (result.isOk) {
            val userId = authRepository.authState.value.let { state ->
                (state as? com.example.amulet.shared.domain.auth.model.AuthState.Guest)?.userId
            }
            if (userId != null) {
                userRepository.createGuestUser(userId)
            }
        }
        return result
    }
}
