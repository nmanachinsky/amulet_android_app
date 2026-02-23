package com.example.amulet.shared.domain.privacy.usecase

import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.privacy.model.UserConsents
import com.example.amulet.shared.domain.user.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.time.ExperimentalTime

/**
 * Стрим текущих согласий пользователя.
 */
@OptIn(ExperimentalTime::class)
class GetUserConsentsUseCase(
    private val userRepository: UserRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<UserConsents> = observeCurrentUserIdUseCase()
        .flatMapLatest { userId ->
            userId?.let { userRepository.getUserConsentsStream(it) }
                ?: flowOf(UserConsents())
        }
}
