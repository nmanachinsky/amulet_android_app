package com.example.amulet.shared.domain.user.usecase

import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.auth.repository.AuthRepository
import com.example.amulet.shared.domain.user.model.User
import com.example.amulet.shared.domain.user.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * UseCase для наблюдения за текущим пользователем из БД.
 * Использует AuthRepository для получения ID, затем запрашивает данные из репозитория.
 */
class ObserveCurrentUserUseCase(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<User?> {
        return authRepository.authState
            .flatMapLatest { context ->
                when (context) {
                    is AuthState.LoggedIn -> {
                        userRepository.observeUser(context.userId)
                    }
                    is AuthState.Guest -> {
                        userRepository.observeUser(context.userId)
                    }
                    else -> flowOf(null)
                }
            }
    }
}
