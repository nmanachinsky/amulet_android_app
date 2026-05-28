package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.repository.DeviceConnectionRepository
import com.example.amulet.shared.domain.devices.repository.DeviceRegistryRepository

/**
 * Use case для отвязки устройства: разрывает активное BLE-соединение и удаляет запись из локальной БД.
 */
class RemoveDeviceUseCase(
    private val deviceRegistryRepository: DeviceRegistryRepository,
    private val deviceConnectionRepository: DeviceConnectionRepository
) {
    suspend operator fun invoke(deviceId: DeviceId): AppResult<Unit> {
        // Разрываем активное BLE-соединение перед удалением (best-effort).
        // Иначе амулет остаётся подключённым по GATT, не уходит в рекламу и не находится
        // при повторном сканировании — до перезапуска приложения, когда соединение рвётся ОС.
        // Приложение держит одно активное соединение, поэтому отключаем текущее.
        deviceConnectionRepository.disconnectFromDevice()
        return deviceRegistryRepository.removeDevice(deviceId)
    }
}
