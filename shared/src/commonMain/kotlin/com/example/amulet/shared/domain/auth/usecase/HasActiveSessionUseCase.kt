package com.example.amulet.shared.domain.auth.usecase

import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.auth.repository.AuthRepository

class HasActiveSessionUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean = authRepository.hasActiveSession()
}
