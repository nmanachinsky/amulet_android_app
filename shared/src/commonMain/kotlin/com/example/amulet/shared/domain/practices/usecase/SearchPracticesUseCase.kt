package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.Practice
import com.example.amulet.shared.domain.practices.model.PracticeFilter
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.github.michaelbull.result.getOrElse

class SearchPracticesUseCase(
    private val repository: PracticesRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        query: String,
        filter: PracticeFilter
    ): AppResult<List<Practice>> {
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.search(userId, query, filter)
    }
}
