package com.example.amulet.data.patterns.seed.breathing

import com.example.amulet.data.patterns.seed.clip
import com.example.amulet.data.patterns.seed.lightPattern
import com.example.amulet.data.patterns.seed.ringTrack
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds
import com.example.amulet.shared.domain.patterns.model.TimelineClip

/**
 * Паттерны практики "Бодрящее дыхание".
 *
 * Намеренная противоположность спокойному 4-7-8: частые яркие тёплые пульсации (жёлтый ->
 * оранжевый -> красно-оранжевый) на активной фазе и мягкое зелёное свечение на отдыхе.
 * Именно этот контраст закрывает исходную жалобу "спокойное = энергичное".
 */
internal object EnergizingBreathingPatterns {

    private const val PULSE_WARM_1 = "#FFEB3B"
    private const val PULSE_WARM_2 = "#FF9800"
    private const val PULSE_WARM_3 = "#FF5722"
    private const val REST_COLOR = "#66BB6A"

    private const val PULSE_MS = 500

    fun all(): List<Pattern> = listOf(
        overview(),
        prepare(),
        active(),
        rest(),
        finish(),
    )

    /** Обзорный паттерн: серия быстрых разгорающихся тёплых импульсов. */
    private fun overview(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_ENERGIZING,
        title = "Бодрящее дыхание",
        description = "Частые яркие импульсы для прилива энергии и ясности",
        type = "ENERGIZING",
        durationMs = 8 * PULSE_MS,
        loop = true,
        tags = listOf("дыхание", "энергия", "бодрость"),
        tracks = listOf(ringTrack(*rapidPulses(count = 8).toTypedArray())),
    )

    private fun prepare(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_ENERGIZING_PREPARE,
        title = "Бодрящее дыхание: подготовка",
        description = "Тёплое разгорание перед активной фазой",
        type = "ENERGIZING",
        durationMs = 15_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 15_000, color = "#FF8F00", fadeInMs = 8_000, fadeOutMs = 4_000)),
        ),
    )

    /** Активная фаза: 20 секунд быстрых ярких импульсов в такт активному дыханию. */
    private fun active(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_ENERGIZING_ACTIVE,
        title = "Бодрящее дыхание: активная фаза",
        description = "Быстрые яркие пульсации в такт активному дыханию",
        type = "ENERGIZING",
        durationMs = 40 * PULSE_MS,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(ringTrack(*rapidPulses(count = 40).toTypedArray())),
    )

    /** Фаза отдыха: 10 секунд мягкого успокаивающего зелёного свечения. */
    private fun rest(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_ENERGIZING_REST,
        title = "Бодрящее дыхание: отдых",
        description = "Спокойное свечение для восстановления между раундами",
        type = "ENERGIZING",
        durationMs = 10_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 10_000, color = REST_COLOR, fadeInMs = 3_000, fadeOutMs = 4_000)),
        ),
    )

    private fun finish(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_ENERGIZING_FINISH,
        title = "Бодрящее дыхание: завершение",
        description = "Мягкий переход от энергии к спокойствию",
        type = "ENERGIZING",
        durationMs = 15_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 15_000, color = "#43A047", fadeInMs = 3_000, fadeOutMs = 8_000)),
        ),
    )

    /** Последовательность из [count] коротких тёплых импульсов с чередованием оттенков. */
    private fun rapidPulses(count: Int): List<TimelineClip> {
        val palette = listOf(PULSE_WARM_1, PULSE_WARM_2, PULSE_WARM_3)
        return (0 until count).map { index ->
            clip(
                startMs = index * PULSE_MS,
                durationMs = PULSE_MS,
                color = palette[index % palette.size],
                fadeInMs = 120,
                fadeOutMs = 180,
            )
        }
    }
}
