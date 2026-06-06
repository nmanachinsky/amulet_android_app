package com.example.amulet.data.patterns.seed.meditation

import com.example.amulet.data.patterns.seed.clip
import com.example.amulet.data.patterns.seed.groupTrack
import com.example.amulet.data.patterns.seed.lightPattern
import com.example.amulet.data.patterns.seed.pulseClips
import com.example.amulet.data.patterns.seed.ringTrack
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds

/**
 * Паттерны медитации "Для фокуса".
 *
 * Прохладная синяя палитра. Свет собирается к одной точке (передние диоды), стабилизируется
 * в ровное свечение, долго удерживается с едва заметным дыханием и мягко возвращается.
 */
internal object FocusMeditationPatterns {

    private const val BLUE = "#2196F3"
    private const val BLUE_SOFT = "#64B5F6"

    private val FRONT = listOf(0, 1)

    fun all(): List<Pattern> = listOf(
        overview(),
        choose(),
        stabilize(),
        sustain(),
        returnPhase(),
    )

    private fun overview(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_FOCUS,
        title = "Медитация для фокуса",
        description = "Ровное синее свечение с лёгким акцентом -- точкой концентрации",
        type = "MEDITATION",
        durationMs = 15_000,
        loop = true,
        tags = listOf("медитация", "фокус", "концентрация"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 15_000, color = BLUE, fadeInMs = 2_000, fadeOutMs = 2_000)),
            groupTrack(FRONT, clip(startMs = 5_000, durationMs = 5_000, color = BLUE_SOFT, fadeInMs = 1_500, fadeOutMs = 1_500), priority = 1),
        ),
    )

    // Фаза = один паттерн полной длины фазы, loop=false.
    private fun choose(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_FOCUS_CHOOSE,
        title = "Фокус: выбор объекта",
        description = "Свет собирается к передней точке концентрации",
        type = "MEDITATION",
        durationMs = 120_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 120_000, color = "#1A2A3A", fadeInMs = 30_000, fadeOutMs = 30_000)),
            groupTrack(FRONT, clip(startMs = 0, durationMs = 120_000, color = BLUE, fadeInMs = 50_000, fadeOutMs = 20_000), priority = 1),
        ),
    )

    private fun stabilize(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_FOCUS_STABILIZE,
        title = "Фокус: стабилизация",
        description = "Ровное устойчивое свечение для удержания внимания",
        type = "MEDITATION",
        durationMs = 300_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 300_000, color = BLUE, fadeInMs = 60_000, fadeOutMs = 60_000)),
        ),
    )

    private fun sustain(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_FOCUS_SUSTAIN,
        title = "Фокус: удержание",
        description = "Долгое стабильное свечение с едва заметным дыханием",
        type = "MEDITATION",
        durationMs = 360_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(ringTrack(*pulseClips(totalMs = 360_000, periodMs = 30_000, color = BLUE).toTypedArray())),
    )

    private fun returnPhase(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_FOCUS_RETURN,
        title = "Фокус: возврат",
        description = "Мягкое потепление света при возвращении внимания в комнату",
        type = "MEDITATION",
        durationMs = 120_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 120_000, color = "#90CAF9", fadeInMs = 45_000, fadeOutMs = 45_000)),
        ),
    )
}
