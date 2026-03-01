package com.example.amulet.core.ble.internal

import java.util.UUID

/**
 * Константы GATT профиля для амулета.
 * Согласно docs/20_DATA_LAYER/03_BLE_PROTOCOL.md
 */
object GattConstants {
    
    // Battery Service (стандартный)
    val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    val BATTERY_LEVEL_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")
    
    // Nordic UART Service
    val NORDIC_UART_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val NORDIC_UART_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // Write
    val NORDIC_UART_RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Notify
    
    // Amulet Device Service
    val AMULET_DEVICE_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789ABC")
    val AMULET_DEVICE_INFO_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789ABD") // Read/Write
    val AMULET_DEVICE_STATUS_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789ABE") // Read/Notify
    val AMULET_OTA_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789ABF") // Write
    val AMULET_ANIMATION_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789AC0") // Write/Notify
    
    // Client Characteristic Configuration Descriptor (для включения уведомлений)
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    
    // MTU размеры
    const val DEFAULT_MTU = 23 // Минимальный MTU по спецификации BLE
    const val PREFERRED_MTU = 512 // Предпочитаемый MTU для больших передач
    const val MAX_MTU = 517 // Максимальный MTU по спецификации BLE 4.2+
    
    // Размеры пакетов
    const val MAX_ATTRIBUTE_LENGTH = 512 // Максимальная длина атрибута
    const val CHUNK_SIZE = 256 // Размер чанка для OTA передачи
    // Для анимаций ограничиваем чанк так, чтобы текстовая команда ADD_SEGMENTS гарантированно влезала в буфер прошивки.
    // 120 байт -> base64 ~160 символов, общая длина строки существенно меньше 256.
    const val ANIMATION_PAYLOAD_CHUNK_SIZE = 120
    
    // Таймауты
    const val CONNECTION_TIMEOUT_MS = 15_000L
    const val COMMAND_TIMEOUT_MS = 10_000L
    const val OTA_CHUNK_TIMEOUT_MS = 30_000L
    const val ANIMATION_TIMEOUT_MS = 60_000L
    const val DISCOVERY_TIMEOUT_MS = 60_000L
}
