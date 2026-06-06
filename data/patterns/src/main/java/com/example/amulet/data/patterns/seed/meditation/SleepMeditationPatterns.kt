package com.example.amulet.data.patterns.seed.meditation

import com.example.amulet.data.patterns.seed.clip
import com.example.amulet.data.patterns.seed.lightPattern
import com.example.amulet.data.patterns.seed.pulseClips
import com.example.amulet.data.patterns.seed.ringTrack
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds

/**
 * Паттерны медитации "Перед сном".
 *
 * Глубокая фиолетово-синяя палитра, которая с каждой фазой становится темнее и медленнее:
 * успокоение -> волна расслабления -> образ покоя -> растворение света почти до нуля.
 */
internal object SleepMeditationPatterns {

    private const val DEEP_VIOLET = "#311B92"
    private const val DEEP_INDIGO = "#283593"
    private const val NIGHT = "#1A237E"

    fun all(): List<Pattern> = listOf(
        overview(),
        settle(),
        wave(),
        imagery(),
        dissolve(),
    )

    private fun overview(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_SLEEP,
        title = "Медитация перед сном",
        description = "Глубокое фиолетовое свечение, медленно угасающее как засыпание",
        type = "MEDITATION",
        durationMs = 20_000,
        loop = true,
        tags = listOf("медитация", "сон", "вечер"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 20_000, color = DEEP_VIOLET, fadeInMs = 6_000, fadeOutMs = 12_000)),
        ),
    )

    // Фаза = один паттерн полной длины фазы, loop=false.
    private fun settle(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_SLEEP_SETTLE,
        title = "Перед сном: успокоение",
        description = "Мягкое глубокое свечение, отпускающее напряжение дня",
        type = "MEDITATION",
        durationMs = 240_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 240_000, color = "#4527A0", fadeInMs = 80_000, fadeOutMs = 80_000)),
        ),
    )

    private fun wave(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_SLEEP_WAVE,
        title = "Перед сном: волна расслабления",
        description = "Очень медленные волны света от стоп к голове",
        type = "MEDITATION",
        durationMs = 360_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(ringTrack(*pulseClips(totalMs = 360_000, periodMs = 60_000, color = DEEP_VIOLET).toTypedArray())),
    )

    private fun imagery(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_SLEEP_IMAGERY,
        title = "Перед сном: образ покоя",
        description = "Тихое глубокое свечение для спокойной визуализации",
        type = "MEDITATION",
        durationMs = 360_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 360_000, color = DEEP_INDIGO, fadeInMs = 120_000, fadeOutMs = 120_000)),
        ),
    )

    private fun dissolve(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_SLEEP_DISSOLVE,
        title = "Перед сном: растворение",
        description = "Долгое затухание света почти до полной темноты",
        type = "MEDITATION",
        durationMs = 240_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 240_000, color = NIGHT, fadeInMs = 20_000, fadeOutMs = 200_000)),
        ),
    )
}
