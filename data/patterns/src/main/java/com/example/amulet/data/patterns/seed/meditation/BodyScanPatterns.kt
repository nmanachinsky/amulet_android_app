package com.example.amulet.data.patterns.seed.meditation

import com.example.amulet.data.patterns.seed.clip
import com.example.amulet.data.patterns.seed.groupTrack
import com.example.amulet.data.patterns.seed.lightPattern
import com.example.amulet.data.patterns.seed.ringTrack
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds

/**
 * Паттерны медитации "Сканирование тела".
 *
 * Тёплая бирюзово-зелёная палитра. Внимание движется снизу вверх -- это передано бегущей
 * по кольцу группой диодов (нижние -> корпус -> верхние) и финальным охватом всего кольца.
 */
internal object BodyScanPatterns {

    private const val TEAL = "#009688"
    private const val TEAL_SOFT = "#4DB6AC"

    private val LOWER = listOf(0, 1)
    private val TORSO = listOf(2, 3, 4)
    private val UPPER = listOf(5, 6, 7)

    fun all(): List<Pattern> = listOf(
        overview(),
        settle(),
        lower(),
        torso(),
        upper(),
        whole(),
    )

    /** Обзорный паттерн: мягкая бирюзовая волна, обегающая кольцо снизу вверх. */
    private fun overview(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_BODY_SCAN,
        title = "Сканирование тела",
        description = "Бирюзовая волна внимания, плавно проходящая по телу снизу вверх",
        type = "MEDITATION",
        durationMs = 16_000,
        loop = true,
        tags = listOf("медитация", "расслабление", "тело"),
        tracks = listOf(
            groupTrack(LOWER, clip(startMs = 0, durationMs = 6_000, color = TEAL, fadeInMs = 1_500, fadeOutMs = 1_500)),
            groupTrack(TORSO, clip(startMs = 5_000, durationMs = 6_000, color = TEAL, fadeInMs = 1_500, fadeOutMs = 1_500), priority = 1),
            groupTrack(UPPER, clip(startMs = 10_000, durationMs = 6_000, color = TEAL_SOFT, fadeInMs = 1_500, fadeOutMs = 1_500), priority = 2),
        ),
    )

    // Фаза = один паттерн полной длины фазы, loop=false.
    private fun settle(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_BODY_SCAN_SETTLE,
        title = "Сканирование тела: настройка",
        description = "Ровное приглушённое свечение всего кольца перед сканированием",
        type = "MEDITATION",
        durationMs = 120_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 120_000, color = TEAL_SOFT, fadeInMs = 45_000, fadeOutMs = 45_000)),
        ),
    )

    private fun lower(): Pattern = region(
        id = PracticePatternIds.MEDITATION_BODY_SCAN_LOWER,
        title = "Сканирование тела: стопы и ноги",
        description = "Внимание в нижней части тела",
        indices = LOWER,
    )

    private fun torso(): Pattern = region(
        id = PracticePatternIds.MEDITATION_BODY_SCAN_TORSO,
        title = "Сканирование тела: корпус",
        description = "Внимание в области живота, груди и спины",
        indices = TORSO,
    )

    private fun upper(): Pattern = region(
        id = PracticePatternIds.MEDITATION_BODY_SCAN_UPPER,
        title = "Сканирование тела: руки, шея и лицо",
        description = "Внимание в верхней части тела",
        indices = UPPER,
    )

    private fun whole(): Pattern = lightPattern(
        id = PracticePatternIds.MEDITATION_BODY_SCAN_WHOLE,
        title = "Сканирование тела: всё тело",
        description = "Мягкий охват вниманием всего тела целиком",
        type = "MEDITATION",
        durationMs = 240_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 240_000, color = TEAL, fadeInMs = 70_000, fadeOutMs = 70_000)),
        ),
    )

    /**
     * Подсветка одной телесной области (180 c): приглушённый фон по кольцу и устойчивое
     * выделение нужной группы диодов.
     */
    private fun region(id: String, title: String, description: String, indices: List<Int>): Pattern =
        lightPattern(
            id = id,
            title = title,
            description = description,
            type = "MEDITATION",
            durationMs = 180_000,
            loop = false,
            tags = listOf("internal_step"),
            tracks = listOf(
                ringTrack(clip(startMs = 0, durationMs = 180_000, color = "#26352F", fadeInMs = 50_000, fadeOutMs = 50_000)),
                groupTrack(indices, clip(startMs = 0, durationMs = 180_000, color = TEAL, fadeInMs = 60_000, fadeOutMs = 60_000), priority = 1),
            ),
        )
}
