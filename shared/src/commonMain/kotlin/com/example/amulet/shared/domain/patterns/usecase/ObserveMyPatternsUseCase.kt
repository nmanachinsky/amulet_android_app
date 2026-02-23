package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.Pattern
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * UseCase для наблюдения за паттернами текущего пользователя.
 */
class ObserveMyPatternsUseCase(
    private val repository: PatternsRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Pattern>> {
        return observeCurrentUserIdUseCase().flatMapLatest { userId ->
            userId?.let { repository.getMyPatternsStream(it) }
                ?: flowOf(emptyList())
        }
    }
}
