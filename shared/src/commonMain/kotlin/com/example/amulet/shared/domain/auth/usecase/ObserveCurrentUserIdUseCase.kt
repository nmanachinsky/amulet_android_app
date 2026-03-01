package com.example.amulet.shared.domain.auth.usecase

import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.auth.repository.AuthRepository
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCurrentUserIdUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<UserId?> = authRepository.authState.map { state ->
        Logger.d("Auth state: $state", "ObserveCurrentUserIdUseCase")
        when (state) {
            is AuthState.LoggedIn -> state.userId
            is AuthState.Guest -> state.userId
            else -> null
        }
    }
}