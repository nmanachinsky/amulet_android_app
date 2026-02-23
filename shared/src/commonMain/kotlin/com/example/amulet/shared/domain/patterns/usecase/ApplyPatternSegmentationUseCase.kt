package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.flow.firstOrNull

/**
 * Обвязочный use case для применения разбиения паттерна на сегменты.
 */
class ApplyPatternSegmentationUseCase(
    private val repository: PatternsRepository,
    private val slicePatternIntoSegments: SlicePatternIntoSegmentsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {

    suspend operator fun invoke(
        patternId: PatternId,
        markersMs: List<Int>,
    ): AppResult<Unit> {
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        
        val basePattern = repository.getPatternById(patternId, userId).firstOrNull()
            ?: return Err(AppError.NotFound)

        return slicePatternIntoSegments(basePattern, markersMs).andThen { segments ->
            if (segments.isEmpty()) {
                Err(AppError.Validation(mapOf("segments" to "No segments produced for given markers")))
            } else {
                repository.upsertSegmentsForPattern(patternId, segments, userId)
            }
        }
    }
}
