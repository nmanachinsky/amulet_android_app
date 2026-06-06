package com.example.amulet.data.patterns.seed.breathing

import com.example.amulet.data.patterns.seed.clip
import com.example.amulet.data.patterns.seed.ledTrack
import com.example.amulet.data.patterns.seed.lightPattern
import com.example.amulet.data.patterns.seed.ringTrack
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds
import com.example.amulet.shared.domain.patterns.model.TimelineTrack

/**
 * Паттерны практики "Дыхание 4-7-8" -- эталон качества библиотеки.
 *
 * Палитра: мягкий зелёный на вдохе (рост), тёплый янтарь "обегающих" диодов на задержке,
 * прохладный синий на длинном выдохе. Один цикл -- 4 + 7 + 8 = 19 секунд.
 */
internal object Breathing478Patterns {

    private const val INHALE_COLOR = "#4CAF50"
    private const val HOLD_COLOR = "#FFC107"
    private const val EXHALE_COLOR = "#2196F3"

    fun all(): List<Pattern> = listOf(
        overview(),
        prepare(),
        inhale(),
        hold(),
        exhale(),
        finish(),
    )

    /** Обзорный зацикленный паттерн: полный цикл 4-7-8 для предпросмотра и устройства. */
    private fun overview(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_478,
        title = "Дыхание 4-7-8",
        description = "Полный цикл успокаивающего дыхания: вдох, задержка, длинный выдох",
        type = "BREATHING",
        durationMs = 19_000,
        loop = true,
        tags = listOf("дыхание", "сон", "успокоение"),
        tracks = buildList {
            add(
                ringTrack(
                    clip(startMs = 0, durationMs = 4_000, color = INHALE_COLOR, fadeInMs = 3_700, fadeOutMs = 300),
                    clip(startMs = 11_000, durationMs = 8_000, color = EXHALE_COLOR, fadeInMs = 300, fadeOutMs = 7_700),
                ),
            )
            addAll(holdRingDots(startMs = 4_000, endMs = 11_000))
        },
    )

    private fun prepare(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_478_PREPARE,
        title = "Дыхание 4-7-8: подготовка",
        description = "Мягкое приглушённое свечение для настройки перед практикой",
        type = "BREATHING_478",
        durationMs = 5_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 5_000, color = "#37474F", fadeInMs = 1_500, fadeOutMs = 1_500)),
        ),
    )

    private fun inhale(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_478_INHALE,
        title = "Дыхание 4-7-8: вдох",
        description = "Медленное нарастание яркости по кольцу на фазе вдоха",
        type = "BREATHING_478",
        durationMs = 4_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 4_000, color = INHALE_COLOR, fadeInMs = 3_700, fadeOutMs = 300)),
        ),
    )

    private fun hold(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_478_HOLD,
        title = "Дыхание 4-7-8: задержка",
        description = "Поочерёдное зажигание диодов по кругу в фазе задержки дыхания",
        type = "BREATHING_478",
        durationMs = 7_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = (0 until 7).map { index ->
            ledTrack(
                index = index,
                clip(startMs = index * 1_000, durationMs = 1_000, color = HOLD_COLOR, fadeInMs = 200, fadeOutMs = 300),
            )
        },
    )

    private fun exhale(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_478_EXHALE,
        title = "Дыхание 4-7-8: выдох",
        description = "Плавное затухание света на длинном выдохе",
        type = "BREATHING_478",
        durationMs = 8_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 8_000, color = EXHALE_COLOR, fadeInMs = 300, fadeOutMs = 7_700)),
        ),
    )

    private fun finish(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_478_FINISH,
        title = "Дыхание 4-7-8: завершение",
        description = "Мягкое угасание свечения для выхода из практики",
        type = "BREATHING_478",
        durationMs = 10_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 10_000, color = "#607D8B", fadeInMs = 1_500, fadeOutMs = 5_000)),
        ),
    )

    /** Восемь диодов, поочерёдно зажигающихся по кругу и тянущихся до конца фазы задержки. */
    private fun holdRingDots(startMs: Int, endMs: Int): List<TimelineTrack> {
        val span = endMs - startMs
        val stepMs = span / RING_DOTS
        return (0 until RING_DOTS).map { index ->
            val clipStart = startMs + index * stepMs
            ledTrack(
                index = index,
                clip(
                    startMs = clipStart,
                    durationMs = endMs - clipStart,
                    color = HOLD_COLOR,
                    fadeInMs = 300,
                    fadeOutMs = 400,
                ),
            )
        }
    }

    private const val RING_DOTS = 8
}
