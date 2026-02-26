package com.example.amulet.shared.domain.patterns.usecase

import com.example.amulet.shared.domain.playback.DevicePlaybackEngine
import com.example.amulet.shared.domain.playback.PlayableMedia
import com.example.amulet.shared.domain.playback.PlaybackState
import com.example.amulet.shared.domain.patterns.model.PatternSpec
import kotlinx.coroutines.flow.Flow

class PreviewPatternOnDeviceUseCase(
    private val engine: DevicePlaybackEngine
) {
    suspend operator fun invoke(spec: PatternSpec): Flow<PlaybackState> {
        engine.play(PlayableMedia.Preview(spec, 1.0))
        return engine.playbackState
    }
}
