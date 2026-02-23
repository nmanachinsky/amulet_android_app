package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.github.michaelbull.result.getOrElse

/**
 * UseCase для удаления тега из паттерна.
 */
class RemoveTagFromPatternUseCase(
    private val repository: PatternsRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        patternId: PatternId,
        tag: String
    ): AppResult<Unit> {
        Logger.d("Удаление тега из паттерна: $patternId, тег: $tag", "RemoveTagFromPatternUseCase")
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.removeTag(patternId, tag, userId)
    }
}
