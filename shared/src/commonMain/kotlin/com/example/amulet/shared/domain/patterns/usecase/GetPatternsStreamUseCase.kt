package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * UseCase для получения списка паттернов с фильтрацией.
 */
class GetPatternsStreamUseCase(
    private val repository: PatternsRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        filter: PatternFilter
    ): Flow<List<Pattern>> {
        return observeCurrentUserIdUseCase().flatMapLatest { userId ->
            userId?.let { repository.getPatternsStream(filter, it) }
                ?: flowOf(emptyList())
        }
    }
}
