package com.example.amulet.shared.domain.auth.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.model.AuthState
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
                (state as? AuthState.Guest)?.userId
            }
            if (userId != null) {
                Logger.d("Enabling guest mode for userId=$userId", tag = "EnableGuestModeUseCase")
                userRepository.createGuestUser(userId)
            }
        }
        return result
    }
}
