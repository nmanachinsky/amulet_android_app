package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternFilter
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * UseCase для получения списка пресетов (ownerId = null).
 */
class GetPresetsUseCase(
    private val repository: PatternsRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Pattern>> {
        return observeCurrentUserIdUseCase().flatMapLatest { result ->
            result?.let { repository.getPatternsStream(PatternFilter(presetsOnly = true), it) }
                ?: flowOf(emptyList())
        }
    }
}
