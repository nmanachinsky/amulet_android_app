package com.example.amulet.data.patterns.seed.meditation

import com.example.amulet.data.patterns.seed.clip
import com.example.amulet.data.patterns.seed.lightPattern
import com.example.amulet.data.patterns.seed.pulseClips
import com.example.amulet.data.patterns.seed.ringTrack
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds

/**
 * Паттерны медитации "Осознанность для начинающих".
 *
 * Спокойная индиго-фиолетовая палитра. Фазы ведут от настройки через дыхание-якорь и
 * наблюдение к мягкому возврату; у каждой фазы свой характер свечения.
 */
internal object MindfulnessPatterns {

    private const val INDIGO = "#5C6BC0"
    private const val VIOLET = "#7E57C2"

    fun all(): List<Pattern> = listOf(
        overview(),
        settle(),
        anchor(),
        observe(),
        returnPhase(),
    )

    private fun overview(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_MINDFULNESS,
        title = "Осознанность",
        description = "Мягкое индиго-свечение, дышащее в спокойном ритме внимания",
        type = "MEDITATION",
        durationMs = 12_000,
        loop = true,
        tags = listOf("медитация", "осознанность", "спокойствие"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 12_000, color = INDIGO, fadeInMs = 5_000, fadeOutMs = 5_000)),
        ),
    )

    // Фаза = один паттерн полной длины фазы (см. PracticeScriptIds/скрипт), loop=false.
    private fun settle(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_MINDFULNESS_SETTLE,
        title = "Осознанность: настройка",
        description = "Приглушённое тепло, постепенно переходящее в индиго покоя",
        type = "MEDITATION",
        durationMs = 90_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(
                clip(startMs = 0, durationMs = 40_000, color = "#5D4037", fadeInMs = 12_000, fadeOutMs = 8_000),
                clip(startMs = 40_000, durationMs = 50_000, color = INDIGO, fadeInMs = 15_000, fadeOutMs = 15_000),
            ),
        ),
    )

    private fun anchor(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_MINDFULNESS_ANCHOR,
        title = "Осознанность: дыхание-якорь",
        description = "Нежная пульсация кольца как опора внимания на дыхании",
        type = "MEDITATION",
        durationMs = 180_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(ringTrack(*pulseClips(totalMs = 180_000, periodMs = 12_000, color = INDIGO).toTypedArray())),
    )

    private fun observe(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_MINDFULNESS_OBSERVE,
        title = "Осознанность: наблюдение",
        description = "Ровное фиолетовое свечение для наблюдения за мыслями без вовлечения",
        type = "MEDITATION",
        durationMs = 240_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(ringTrack(*pulseClips(totalMs = 240_000, periodMs = 24_000, color = VIOLET).toTypedArray())),
    )

    private fun returnPhase(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_MINDFULNESS_RETURN,
        title = "Осознанность: возврат",
        description = "Мягкое потепление света для возвращения в комнату",
        type = "MEDITATION",
        durationMs = 90_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 90_000, color = "#FFB74D", fadeInMs = 40_000, fadeOutMs = 30_000)),
        ),
    )
}
