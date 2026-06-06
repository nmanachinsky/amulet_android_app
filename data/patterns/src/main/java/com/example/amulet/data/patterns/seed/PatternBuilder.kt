package com.example.amulet.data.patterns.seed

import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.model.PatternKind
import com.example.amulet.shared.domain.patterns.model.PatternSpec
import com.example.amulet.shared.domain.patterns.model.PatternTimeline
import com.example.amulet.shared.domain.patterns.model.ReviewStatus
import com.example.amulet.shared.domain.patterns.model.TargetGroup
import com.example.amulet.shared.domain.patterns.model.TargetLed
import com.example.amulet.shared.domain.patterns.model.TargetRing
import com.example.amulet.shared.domain.patterns.model.TimelineClip
import com.example.amulet.shared.domain.patterns.model.TimelineTrack

/** Версия прошивки амулета, под которую рассчитаны сид-паттерны практик. */
internal const val PRACTICE_PATTERN_HARDWARE_VERSION = 100

/** Количество светодиодов в кольце амулета (валидные индексы [TargetLed]: 0..7). */
internal const val RING_LED_COUNT = 8

/**
 * Лаконично собирает системный световой [Pattern] практики из таймлайна.
 *
 * Снимает повторяющийся boilerplate (owner/version/review/hardware/время) и оставляет
 * в каждом определении только содержательную часть -- список треков. `durationMs` паттерна
 * и его таймлайна всегда совпадают по построению.
 */
internal fun lightPattern(
    id: String,
    title: String,
    description: String,
    type: String,
    durationMs: Int,
    loop: Boolean,
    tags: List<String>,
    tracks: List<TimelineTrack>,
    now: Long = System.currentTimeMillis(),
): Pattern = Pattern(
    id = PatternId(id),
    version = 1,
    ownerId = null, // системный паттерн
    kind = PatternKind.LIGHT,
    spec = PatternSpec(
        type = type,
        hardwareVersion = PRACTICE_PATTERN_HARDWARE_VERSION,
        durationMs = durationMs,
        loop = loop,
        timeline = PatternTimeline(durationMs = durationMs, tracks = tracks),
    ),
    public = true,
    reviewStatus = ReviewStatus.APPROVED,
    hardwareVersion = PRACTICE_PATTERN_HARDWARE_VERSION,
    title = title,
    description = description,
    tags = tags,
    usageCount = 0,
    sharedWith = emptyList(),
    createdAt = now,
    updatedAt = now,
)

/** Клип таймлайна с мягкими нарастанием/затуханием. */
internal fun clip(
    startMs: Int,
    durationMs: Int,
    color: String,
    fadeInMs: Int = 0,
    fadeOutMs: Int = 0,
): TimelineClip = TimelineClip(
    startMs = startMs,
    durationMs = durationMs,
    color = color,
    fadeInMs = fadeInMs,
    fadeOutMs = fadeOutMs,
)

/** Трек по всему кольцу. */
internal fun ringTrack(vararg clips: TimelineClip, priority: Int = 0): TimelineTrack =
    TimelineTrack(target = TargetRing, priority = priority, clips = clips.toList())

/** Трек одного светодиода. */
internal fun ledTrack(index: Int, vararg clips: TimelineClip, priority: Int = 1): TimelineTrack =
    TimelineTrack(target = TargetLed(index), priority = priority, clips = clips.toList())

/** Трек группы светодиодов. */
internal fun groupTrack(
    indices: List<Int>,
    vararg clips: TimelineClip,
    priority: Int = 0,
): TimelineTrack =
    TimelineTrack(target = TargetGroup(indices), priority = priority, clips = clips.toList())

/**
 * Последовательность из плавных пульсаций одного цвета, покрывающая [totalMs] периодами по
 * [periodMs]. Используется, чтобы заполнить длинную фазу (вдох-якорь, удержание фокуса)
 * мягким «дыханием» света. [totalMs] должно делиться на [periodMs] нацело.
 */
internal fun pulseClips(
    totalMs: Int,
    periodMs: Int,
    color: String,
    fadeRatio: Float = 0.45f,
): List<TimelineClip> {
    require(totalMs % periodMs == 0) { "totalMs ($totalMs) должно делиться на periodMs ($periodMs)" }
    val fade = (periodMs * fadeRatio).toInt()
    return (0 until totalMs / periodMs).map { index ->
        clip(
            startMs = index * periodMs,
            durationMs = periodMs,
            color = color,
            fadeInMs = fade,
            fadeOutMs = fade,
        )
    }
}
