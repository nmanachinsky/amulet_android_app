package com.example.amulet.data.patterns.seed.other

import com.example.amulet.data.patterns.seed.clip
import com.example.amulet.data.patterns.seed.ledTrack
import com.example.amulet.data.patterns.seed.lightPattern
import com.example.amulet.data.patterns.seed.ringTrack
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds

/**
 * Паттерны смешанных и продвинутых практик -- по одному уникальному паттерну на практику.
 * Каждый паттерн отражает цель практики собственной палитрой и характером движения.
 */
internal object MixedPracticePatterns {

    fun all(): List<Pattern> = listOf(
        morning(),
        stressRelief(),
        anxiety(),
        mood(),
        deepMeditation(),
    )

    /** Утренний ритуал: рассвет от глубокого оранжевого к яркому жёлтому. */
    private fun morning(): Pattern = lightPattern(
        id = PracticePatternIds.MIXED_MORNING,
        title = "Утренний ритуал",
        description = "Свет рассвета: от глубокого оранжевого к яркому утреннему жёлтому",
        type = "MIXED",
        durationMs = 14_000,
        loop = true,
        tags = listOf("утро", "энергия", "ритуал"),
        tracks = listOf(
            ringTrack(
                clip(startMs = 0, durationMs = 5_000, color = "#FF6F00", fadeInMs = 2_500, fadeOutMs = 500),
                clip(startMs = 5_000, durationMs = 4_000, color = "#FFB300", fadeInMs = 500, fadeOutMs = 500),
                clip(startMs = 9_000, durationMs = 5_000, color = "#FFEB3B", fadeInMs = 500, fadeOutMs = 2_500),
            ),
        ),
    )

    /** Экспресс-снятие стресса: успокаивающие импульсы от пурпурного к синему. */
    private fun stressRelief(): Pattern = lightPattern(
        id = PracticePatternIds.MIXED_STRESS_RELIEF,
        title = "Экспресс-снятие стресса",
        description = "Успокаивающие импульсы, остывающие от пурпурного к спокойному синему",
        type = "MIXED",
        durationMs = 10_000,
        loop = true,
        tags = listOf("стресс", "быстро", "успокоение"),
        tracks = listOf(
            ringTrack(
                clip(startMs = 0, durationMs = 5_000, color = "#7B1FA2", fadeInMs = 1_500, fadeOutMs = 1_500),
                clip(startMs = 5_000, durationMs = 5_000, color = "#303F9F", fadeInMs = 1_500, fadeOutMs = 2_500),
            ),
        ),
    )

    /** Работа с тревожностью: заземляющее глубокое бирюзовое с удлинённым выдохом. */
    private fun anxiety(): Pattern = lightPattern(
        id = PracticePatternIds.MIXED_ANXIETY,
        title = "Работа с тревожностью",
        description = "Заземляющее глубокое бирюзовое свечение с долгим успокаивающим выдохом",
        type = "MIXED",
        durationMs = 12_000,
        loop = true,
        tags = listOf("тревога", "заземление", "спокойствие"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 12_000, color = "#00695C", fadeInMs = 3_500, fadeOutMs = 7_500)),
        ),
    )

    /** Улучшение настроения: тёплые коралово-розовые волны. */
    private fun mood(): Pattern = lightPattern(
        id = PracticePatternIds.MIXED_MOOD,
        title = "Улучшение настроения",
        description = "Тёплые коралово-розовые волны, мягко поднимающие настроение",
        type = "MIXED",
        durationMs = 10_000,
        loop = true,
        tags = listOf("настроение", "позитив", "тепло"),
        tracks = listOf(
            ringTrack(
                clip(startMs = 0, durationMs = 5_000, color = "#EC407A", fadeInMs = 1_500, fadeOutMs = 1_000),
                clip(startMs = 5_000, durationMs = 5_000, color = "#FFA726", fadeInMs = 1_000, fadeOutMs = 1_500),
            ),
        ),
    )

    /** Глубокая медитация: очень медленное тёмно-индиго с одиночным "странствующим" диодом. */
    private fun deepMeditation(): Pattern = lightPattern(
        id = PracticePatternIds.ADVANCED_DEEP_MEDITATION,
        title = "Глубокая медитация",
        description = "Очень медленное тёмно-индиго свечение с одиночной странствующей точкой",
        type = "MIXED",
        durationMs = 24_000,
        loop = true,
        tags = listOf("глубокая", "продвинутая", "исследование"),
        tracks = buildList {
            add(ringTrack(clip(startMs = 0, durationMs = 24_000, color = "#1A237E", fadeInMs = 8_000, fadeOutMs = 8_000)))
            // Одна точка медленно обходит кольцо -- ориентир внимания в глубине.
            val stepMs = 24_000 / 8
            for (index in 0 until 8) {
                add(
                    ledTrack(
                        index = index,
                        clip(startMs = index * stepMs, durationMs = stepMs, color = "#5C6BC0", fadeInMs = 600, fadeOutMs = 600),
                        priority = 1,
                    ),
                )
            }
        },
    )
}
