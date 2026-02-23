package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class GetActiveSessionStreamUseCase(
    private val repository: PracticesRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    operator fun invoke(): Flow<PracticeSession?> = observeCurrentUserIdUseCase().flatMapLatest { userId ->
        if (userId == null) {
            return@flatMapLatest flowOf(null)
        }
        repository.getActiveSessionStream(userId)
    }
}
