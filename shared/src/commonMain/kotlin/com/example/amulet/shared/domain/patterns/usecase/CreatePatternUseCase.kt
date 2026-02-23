package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternDraft
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse

/**
 * UseCase для создания паттерна с валидацией.
 */
class CreatePatternUseCase(
    private val repository: PatternsRepository,
    private val validator: PatternValidator,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        draft: PatternDraft
    ): AppResult<Pattern> {
        Logger.d("Создание паттерна: ${draft.title}, тип: ${draft.kind}", "CreatePatternUseCase")
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return validator.validate(draft.spec).andThen {
            Logger.d("Валидация пройдена, создание паттерна", "CreatePatternUseCase")
            repository.createPattern(draft, userId)
        }
    }
}
