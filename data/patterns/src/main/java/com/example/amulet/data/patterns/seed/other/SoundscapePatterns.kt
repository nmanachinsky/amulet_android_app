package com.example.amulet.data.patterns.seed.other

import com.example.amulet.data.patterns.seed.clip
import com.example.amulet.data.patterns.seed.groupTrack
import com.example.amulet.data.patterns.seed.ledTrack
import com.example.amulet.data.patterns.seed.lightPattern
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds

/**
 * Паттерны звуковых ландшафтов -- по одному уникальному паттерну на практику.
 *
 * Океан: холодные синие "перекаты" прибоя по группам диодов. Лес: тёплые зелёные слои
 * листвы с редкими янтарными вспышками-"птицами" на отдельных диодах.
 */
internal object SoundscapePatterns {

    fun all(): List<Pattern> = listOf(
        ocean(),
        forest(),
    )

    /** Океан: плавные синие волны, перекатывающиеся по кольцу как прибой. */
    private fun ocean(): Pattern = lightPattern(
        id = PracticePatternIds.SOUNDSCAPE_OCEAN,
        title = "Звуки океана",
        description = "Плавные синие волны, перекатывающиеся по кольцу как морской прибой",
        type = "SOUNDSCAPE",
        durationMs = 30_000,
        loop = true,
        tags = listOf("звуки", "океан", "расслабление"),
        tracks = listOf(
            groupTrack(listOf(0, 1, 2), clip(startMs = 0, durationMs = 12_000, color = "#0277BD", fadeInMs = 3_000, fadeOutMs = 3_000)),
            groupTrack(listOf(3, 4, 5), clip(startMs = 9_000, durationMs = 12_000, color = "#039BE5", fadeInMs = 3_000, fadeOutMs = 3_000), priority = 1),
            groupTrack(listOf(6, 7), clip(startMs = 18_000, durationMs = 12_000, color = "#4FC3F7", fadeInMs = 3_000, fadeOutMs = 3_000), priority = 2),
        ),
    )

    /** Лес: тёплые зелёные слои листвы и редкие янтарные вспышки-птицы. */
    private fun forest(): Pattern = lightPattern(
        id = PracticePatternIds.SOUNDSCAPE_FOREST,
        title = "Лесные звуки",
        description = "Тёплое зелёное свечение листвы с редкими янтарными вспышками птиц",
        type = "SOUNDSCAPE",
        durationMs = 30_000,
        loop = true,
        tags = listOf("звуки", "лес", "природа"),
        tracks = listOf(
            groupTrack(listOf(0, 1, 2, 3), clip(startMs = 0, durationMs = 16_000, color = "#2E7D32", fadeInMs = 4_000, fadeOutMs = 4_000)),
            groupTrack(listOf(4, 5, 6, 7), clip(startMs = 12_000, durationMs = 16_000, color = "#66BB6A", fadeInMs = 4_000, fadeOutMs = 4_000), priority = 1),
            // Редкие вспышки-"птицы" на отдельных диодах.
            ledTrack(2, clip(startMs = 6_000, durationMs = 1_500, color = "#FFD54F", fadeInMs = 300, fadeOutMs = 900), priority = 2),
            ledTrack(5, clip(startMs = 21_000, durationMs = 1_500, color = "#FFCA28", fadeInMs = 300, fadeOutMs = 900), priority = 2),
        ),
    )
}
