package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.github.michaelbull.result.getOrElse

/**
 * UseCase для удаления паттерна с проверкой зависимостей.
 */
class DeletePatternUseCase(
    private val repository: PatternsRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        id: PatternId
    ): AppResult<Unit> {
        Logger.d("Удаление паттерна: $id", "DeletePatternUseCase")
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.deletePattern(id, userId)
    }
}
