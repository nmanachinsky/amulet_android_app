package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.github.michaelbull.result.getOrElse

/**
 * UseCase, который гарантирует наличие паттерна локально.
 * Если паттерна нет в БД, репозиторий попытается загрузить его с сервера и сохранить.
 */
class EnsurePatternLoadedUseCase(
    private val repository: PatternsRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(id: PatternId): AppResult<Unit> {
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.ensurePatternLoaded(id, userId)
    }
}
