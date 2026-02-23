package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.model.PatternUpdate
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse

/**
 * UseCase для обновления паттерна с оптимистической блокировкой.
 */
class UpdatePatternUseCase(
    private val repository: PatternsRepository,
    private val validator: PatternValidator,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        id: PatternId,
        version: Int,
        updates: PatternUpdate
    ): AppResult<Pattern> {
        Logger.d("Обновление паттерна: $id, версия: $version", "UpdatePatternUseCase")
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        val validationResult = updates.spec?.let { spec ->
            Logger.d("Валидация изменений в спецификации", "UpdatePatternUseCase")
            validator.validate(spec)
        } ?: Ok(Unit)
        
        return validationResult.andThen {
            Logger.d("Валидация пройдена, обновление паттерна", "UpdatePatternUseCase")
            repository.updatePattern(id, version, updates, userId)
        }
    }
}
