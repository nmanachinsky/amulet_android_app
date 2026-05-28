package com.example.amulet.feature.devices.presentation.details

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.devices.model.FirmwareUpdate

/**
 * Контракт для экрана деталей устройства.
 */

data class DeviceDetailsState(
    val device: Device? = null,
    val firmwareUpdate: FirmwareUpdate? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: AppError? = null,
    val isDeviceOnline: Boolean = false,
    val batteryLevel: Int? = null,
)

sealed interface DeviceDetailsEvent {
    data class UpdateName(val name: String) : DeviceDetailsEvent
    data class UpdateBrightness(val brightness: Double) : DeviceDetailsEvent
    data class UpdateHaptics(val haptics: Double) : DeviceDetailsEvent
    data object UnclaimDevice : DeviceDetailsEvent
    data object NavigateToOta : DeviceDetailsEvent
    data object DismissError : DeviceDetailsEvent
    data class SaveSettings(val name: String) : DeviceDetailsEvent
    data object Reconnect : DeviceDetailsEvent
}

sealed interface DeviceDetailsSideEffect {
    data class NavigateToOta(val deviceId: String) : DeviceDetailsSideEffect
    data object DeviceUnclaimedNavigateBack : DeviceDetailsSideEffect
    data object SettingsSavedNavigateBack : DeviceDetailsSideEffect
    data class ShowError(val error: AppError) : DeviceDetailsSideEffect
}
