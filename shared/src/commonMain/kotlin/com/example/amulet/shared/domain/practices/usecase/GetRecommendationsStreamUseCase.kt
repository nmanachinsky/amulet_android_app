package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.Practice
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class GetRecommendationsStreamUseCase(
    private val repository: PracticesRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(limit: Int? = null, goal: com.example.amulet.shared.domain.practices.model.PracticeGoal? = null): Flow<List<Practice>> =
        observeCurrentUserIdUseCase().flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(emptyList())
            }
            repository.getRecommendationsStream(userId, limit, goal)
        }
}
