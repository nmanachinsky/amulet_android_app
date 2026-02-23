package com.example.amulet.shared.domain.auth.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.repository.AuthRepository
import com.example.amulet.shared.domain.user.repository.UserRepository
import com.github.michaelbull.result.flatMap

class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(idToken: String, rawNonce: String?): AppResult<Unit> =
        authRepository
            .signInWithGoogle(idToken, rawNonce)
            .flatMap { userId -> userRepository.ensureProfileLoaded(userId) }
}
