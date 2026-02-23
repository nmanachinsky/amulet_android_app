package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticeScript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Use case для получения скрипта практики по её идентификатору.
 * Если практика не найдена или скрипт отсутствует, возвращает null.
 */
class GetPracticeScriptUseCase(
    private val practicesRepository: PracticesRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase,
) {

    operator fun invoke(practiceId: PracticeId): Flow<PracticeScript?> =
        observeCurrentUserIdUseCase().flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(null)
            }
            practicesRepository.getPracticeById(userId, practiceId).map { it?.script }
        }
}
