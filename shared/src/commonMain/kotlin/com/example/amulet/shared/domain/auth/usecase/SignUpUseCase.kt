package com.example.amulet.shared.domain.auth.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.model.UserCredentials
import com.example.amulet.shared.domain.auth.repository.AuthRepository
import com.example.amulet.shared.domain.user.repository.UserRepository
import com.github.michaelbull.result.flatMap

class SignUpUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(credentials: UserCredentials): AppResult<Unit> =
        authRepository
            .signUp(credentials)
            .flatMap { userId -> userRepository.preloadProfileToCache(userId) }

}
