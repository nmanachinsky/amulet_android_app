package com.example.amulet.shared.domain.devices.model

import com.example.amulet.shared.domain.patterns.model.Easing
import com.example.amulet.shared.domain.patterns.model.MixMode
import com.example.amulet.shared.domain.devices.model.Rgb

/**
 * Один бинарный сегмент таймлайна устройства (SegmentLinearRgbV2).
 * См. docs/20_DATA_LAYER/03_BLE_PROTOCOL.md (struct SegmentLinearRgbV2).
 */
data class DeviceTimelineSegment(
    val targetMask: Int,          // битовая маска LED (0-255)
    val priority: Int,            // 0..255 (меньше = раньше применяется)
    val mixMode: MixMode,         // OVERRIDE / ADDITIVE
    val startMs: Long,            // 0..2^32-1
    val durationMs: Long,         // 0..2^32-1
    val fadeInMs: Int,            // 0..2^16-1
    val fadeOutMs: Int,           // 0..2^16-1
    val easingIn: Easing,         // пока только LINEAR
    val easingOut: Easing,        // пока только LINEAR
    val color: Rgb                // базовый цвет сегмента
) {
    fun toByteArray(): ByteArray {
        // Строго по SegmentLinearRgbV2 (little-endian)
        // Структура: 1 (opcode) + 1 (targetMask) + 1 (priority) + 1 (mixMode)
        //          + 4 (startMs) + 4 (durationMs)
        //          + 2 (fadeInMs) + 2 (fadeOutMs)
        //          + 1 (easingIn) + 1 (easingOut)
        //          + 3 (RGB) = 21 байт
        val result = ByteArray(21)
        var i = 0

        // opcode = 0x01 (LINEAR_RGB)
        result[i++] = 0x01

        result[i++] = (targetMask and 0xFF).toByte()
        result[i++] = (priority and 0xFF).toByte()
        result[i++] = mixMode.toByte()

        writeUInt32LE(startMs, result, i)
        i += 4
        writeUInt32LE(durationMs, result, i)
        i += 4
        writeUInt16LE(fadeInMs, result, i)
        i += 2
        writeUInt16LE(fadeOutMs, result, i)
        i += 2

        result[i++] = easingIn.toByte()
        result[i++] = easingOut.toByte()

        result[i++] = (color.red and 0xFF).toByte()
        result[i++] = (color.green and 0xFF).toByte()
        result[i] = (color.blue and 0xFF).toByte()

        return result
    }

    private fun MixMode.toByte(): Byte = when (this) {
        MixMode.OVERRIDE -> 0
        MixMode.ADDITIVE -> 1
    }.toByte()

    private fun Easing.toByte(): Byte = when (this) {
        Easing.LINEAR -> 0
    }.toByte()

    private fun writeUInt32LE(value: Long, target: ByteArray, offset: Int) {
        val v = value.coerceIn(0L, 0xFFFF_FFFFL)
        target[offset] = (v and 0xFF).toByte()
        target[offset + 1] = ((v ushr 8) and 0xFF).toByte()
        target[offset + 2] = ((v ushr 16) and 0xFF).toByte()
        target[offset + 3] = ((v ushr 24) and 0xFF).toByte()
    }

    private fun writeUInt16LE(value: Int, target: ByteArray, offset: Int) {
        val v = value.coerceIn(0, 0xFFFF)
        target[offset] = (v and 0xFF).toByte()
        target[offset + 1] = ((v ushr 8) and 0xFF).toByte()
    }
}

/**
 * План анимации для устройства на уровне сегментов таймлайна.
 * Используется как доменный контракт. Склейка в байты происходит в Data-слое.
 */
data class DeviceAnimationPlan(
    val id: String,
    val totalDurationMs: Long,
    val segments: List<DeviceTimelineSegment>,
    val isPreview: Boolean = false
)
