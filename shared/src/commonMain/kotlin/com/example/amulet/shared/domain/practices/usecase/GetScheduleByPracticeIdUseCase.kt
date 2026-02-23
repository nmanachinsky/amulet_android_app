package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticeSchedule
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class GetScheduleByPracticeIdUseCase(
    private val repository: PracticesRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    operator fun invoke(practiceId: PracticeId): Flow<PracticeSchedule?> =
        observeCurrentUserIdUseCase().flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(null)
            }
            repository.getScheduleByPracticeId(userId, practiceId)
        }
}
