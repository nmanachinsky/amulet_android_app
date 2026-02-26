package com.example.amulet.shared.domain.playback

import com.example.amulet.shared.core.AppResult
import kotlinx.coroutines.flow.Flow

interface DevicePlaybackEngine {
    val playbackState: Flow<PlaybackState>

    suspend fun play(media: PlayableMedia): AppResult<Unit>

    suspend fun stop(): AppResult<Unit>
}
