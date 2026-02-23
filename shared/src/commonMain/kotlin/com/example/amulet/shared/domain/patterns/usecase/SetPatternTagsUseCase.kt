package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.github.michaelbull.result.getOrElse

class SetPatternTagsUseCase(
    private val repository: PatternsRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        patternId: PatternId,
        tags: List<String>
    ): AppResult<Unit> {
        Logger.d("Установка тегов паттерна: ${patternId.value} -> ${tags.size}", "SetPatternTagsUseCase")
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.setPatternTags(patternId, tags, userId)
    }
}
