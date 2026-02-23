package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.PracticeAudioMode
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.practices.model.PracticeSessionSource
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.github.michaelbull.result.getOrElse

class StartPracticeUseCase(
    private val repository: PracticesRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        practiceId: PracticeId,
        intensity: Double? = null,
        brightness: Double? = null,
        vibrationLevel: Double? = null,
        audioMode: PracticeAudioMode? = null,
        source: PracticeSessionSource? = PracticeSessionSource.Manual,
    ): AppResult<PracticeSession> {
        val userId = getCurrentUserIdUseCase().getOrElse { return AppResult(it) }
        return repository.startPractice(
            userId = userId,
            practiceId = practiceId,
            intensity = intensity,
            brightness = brightness,
            vibrationLevel = vibrationLevel,
            audioMode = audioMode,
            source = source,
        )
    }
}
