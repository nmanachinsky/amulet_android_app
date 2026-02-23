package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.PatternMarkers
import com.github.michaelbull.result.getOrElse

class UpsertPatternMarkersUseCase(
    private val repository: PatternsRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {

    suspend operator fun invoke(markers: PatternMarkers): AppResult<Unit> {
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.upsertPatternMarkers(markers, userId)
    }
}
