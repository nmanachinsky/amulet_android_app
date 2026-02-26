package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.playback.DevicePlaybackEngine

class ClearCurrentDevicePatternUseCase(
    private val engine: DevicePlaybackEngine,
) {
    suspend operator fun invoke(): AppResult<Unit> = engine.stop()
}
