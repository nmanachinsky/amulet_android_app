package com.example.amulet.shared.domain.auth.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.first

class GetCurrentUserIdUseCase(
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    suspend operator fun invoke(): AppResult<UserId> {
        val userId = observeCurrentUserIdUseCase().first()
        return userId?.let { Ok(it) }
            ?: Err(AppError.Unauthorized)
    }
}