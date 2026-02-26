package com.example.amulet.shared.domain.devices.model

import com.example.amulet.shared.domain.patterns.model.Easing
import com.example.amulet.shared.domain.patterns.model.MixMode

/**
 * Один сегмент таймлайна устройства (SegmentLinearRgbV2).
 * Бинарная сериализация (toByteArray) находится в Data слое.
 * Cm. docs/20_DATA_LAYER/03_BLE_PROTOCOL.md (struct SegmentLinearRgbV2).
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
)

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
