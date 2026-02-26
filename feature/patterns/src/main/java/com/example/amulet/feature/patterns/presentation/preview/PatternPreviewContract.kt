package com.example.amulet.feature.patterns.presentation.preview

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternSpec
import com.example.amulet.shared.domain.playback.PlaybackState

data class PatternPreviewState(
    val pattern: Pattern? = null,
    val spec: PatternSpec? = null,
    val devices: List<Device> = emptyList(),
    val selectedDevice: Device? = null,
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = true,
    val isSendingToDevice: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val isDeviceConnected: Boolean = false,
    val batteryLevel: Int? = null,
    val error: AppError? = null
)

sealed interface PatternPreviewEvent {
    data object LoadDevices : PatternPreviewEvent
    data class SelectDevice(val deviceId: String) : PatternPreviewEvent
    data object PlayPause : PatternPreviewEvent
    data object Restart : PatternPreviewEvent
    data class UpdateLoop(val loop: Boolean) : PatternPreviewEvent
    data object SendToDevice : PatternPreviewEvent
    data object DismissError : PatternPreviewEvent
}

sealed interface PatternPreviewSideEffect {
    data class ShowSnackbar(val message: String) : PatternPreviewSideEffect
    data object ShowDeviceRequired : PatternPreviewSideEffect
    data class ShowBleConnectionError(val error: AppError) : PatternPreviewSideEffect
    data object NavigateToDeviceSelection : PatternPreviewSideEffect
}
