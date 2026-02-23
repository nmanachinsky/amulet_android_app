package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * UseCase для получения паттерна по ID.
 */
class GetPatternByIdUseCase(
    private val repository: PatternsRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(id: PatternId): Flow<Pattern?> {
        return observeCurrentUserIdUseCase().flatMapLatest { userId ->
            userId?.let { repository.getPatternById(id) }
                ?: flowOf(null)
        }
    }
}
