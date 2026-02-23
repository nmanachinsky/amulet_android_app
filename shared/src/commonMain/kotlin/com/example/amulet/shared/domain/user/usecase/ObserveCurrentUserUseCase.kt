package com.example.amulet.shared.domain.user.usecase

import com.example.amulet.shared.domain.user.model.User
import com.example.amulet.shared.domain.user.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * UseCase для наблюдения за текущим пользователем из БД.
 * Использует UserSessionProvider для получения ID, затем запрашивает данные из репозитория.
 */
class ObserveCurrentUserUseCase(
    private val userRepository: UserRepository,
    private val sessionProvider: UserSessionProvider
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<User?> {
        return sessionProvider.sessionContext
            .flatMapLatest { context ->
                when (context) {
                    is UserSessionContext.LoggedIn -> {
                        userRepository.observeUser(context.userId)
                    }
                    else -> flowOf(null)
                }
            }
    }
}
