package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class GetSessionsHistoryStreamUseCase(
    private val repository: PracticesRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(limit: Int? = null): Flow<List<PracticeSession>> = observeCurrentUserIdUseCase().flatMapLatest { userId ->
        if (userId == null) {
            return@flatMapLatest flowOf(emptyList())
        }
        repository.getSessionsHistoryStream(userId, limit)
    }
}
