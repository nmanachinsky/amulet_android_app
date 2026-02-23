package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.model.PublishMetadata
import com.github.michaelbull.result.getOrElse

/**
 * UseCase для публикации паттерна в общий каталог.
 */
class PublishPatternUseCase(
    private val repository: PatternsRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        id: PatternId,
        metadata: PublishMetadata
    ): AppResult<Pattern> {
        Logger.d("Публикация паттерна: $id, заголовок: ${metadata.title}", "PublishPatternUseCase")
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.publishPattern(id, metadata, userId)
    }
}
