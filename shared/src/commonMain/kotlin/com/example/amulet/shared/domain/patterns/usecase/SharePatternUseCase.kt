package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.getOrElse

/**
 * UseCase для шаринга паттерна с другими пользователями.
 */
class SharePatternUseCase(
    private val repository: PatternsRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        id: PatternId,
        userIds: List<UserId>
    ): AppResult<Unit> {
        Logger.d("Шаринг паттерна: $id, пользователям: ${userIds.size}", "SharePatternUseCase")
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.sharePattern(id, userIds, userId)
    }
}
