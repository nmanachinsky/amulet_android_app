package com.example.amulet.data.patterns.seed.breathing

import com.example.amulet.data.patterns.seed.clip
import com.example.amulet.data.patterns.seed.ledTrack
import com.example.amulet.data.patterns.seed.lightPattern
import com.example.amulet.data.patterns.seed.ringTrack
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds

/**
 * Паттерны практики "Квадратное дыхание" (4-4-4-4).
 *
 * Метафора квадрата: четыре равные фазы по 4 секунды и четыре "угловых" диода (верх, право,
 * низ, лево), поочерёдно очерчивающие квадрат. Палитра холодная и уравновешенная, чтобы
 * отличаться от тёплого 4-7-8: голубой вдох, бирюзовая задержка, индиго выдох, тёмная пауза.
 */
internal object BoxBreathingPatterns {

    private const val INHALE_COLOR = "#26C6DA"
    private const val HOLD_COLOR = "#00897B"
    private const val EXHALE_COLOR = "#5C6BC0"
    private const val PAUSE_COLOR = "#455A64"

    private const val PHASE_MS = 4_000

    // "Углы" квадрата: верх, право, низ, лево.
    private const val CORNER_TOP = 0
    private const val CORNER_RIGHT = 2
    private const val CORNER_BOTTOM = 4
    private const val CORNER_LEFT = 6

    fun all(): List<Pattern> = listOf(
        overview(),
        prepare(),
        inhale(),
        hold(),
        exhale(),
        pause(),
        finish(),
    )

    /** Обзорный зацикленный паттерн: квадрат из четырёх фаз с обегающими углами. */
    private fun overview(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_BOX,
        title = "Квадратное дыхание",
        description = "Четыре равные фазы по 4 секунды, очерчивающие квадрат дыхания",
        type = "BREATHING_BOX",
        durationMs = 4 * PHASE_MS,
        loop = true,
        tags = listOf("дыхание", "стресс", "баланс"),
        tracks = listOf(
            ringTrack(
                clip(startMs = 0, durationMs = PHASE_MS, color = INHALE_COLOR, fadeInMs = PHASE_MS, fadeOutMs = 0),
                clip(startMs = PHASE_MS, durationMs = PHASE_MS, color = HOLD_COLOR, fadeInMs = 400, fadeOutMs = 400),
                clip(startMs = 2 * PHASE_MS, durationMs = PHASE_MS, color = EXHALE_COLOR, fadeInMs = 0, fadeOutMs = PHASE_MS),
                clip(startMs = 3 * PHASE_MS, durationMs = PHASE_MS, color = PAUSE_COLOR, fadeInMs = 300, fadeOutMs = 3_000),
            ),
            cornerAccent(CORNER_TOP, phaseIndex = 0, color = INHALE_COLOR),
            cornerAccent(CORNER_RIGHT, phaseIndex = 1, color = HOLD_COLOR),
            cornerAccent(CORNER_BOTTOM, phaseIndex = 2, color = EXHALE_COLOR),
            cornerAccent(CORNER_LEFT, phaseIndex = 3, color = PAUSE_COLOR),
        ),
    )

    private fun prepare(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_BOX_PREPARE,
        title = "Квадратное дыхание: подготовка",
        description = "Спокойное свечение для настройки на ровный ритм",
        type = "BREATHING_BOX",
        durationMs = 10_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 10_000, color = "#37474F", fadeInMs = 3_000, fadeOutMs = 3_000)),
        ),
    )

    private fun inhale(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_BOX_INHALE,
        title = "Квадратное дыхание: вдох",
        description = "Ровное нарастание яркости на вдохе",
        type = "BREATHING_BOX",
        durationMs = PHASE_MS,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = PHASE_MS, color = INHALE_COLOR, fadeInMs = PHASE_MS, fadeOutMs = 0)),
        ),
    )

    private fun hold(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_BOX_HOLD,
        title = "Квадратное дыхание: задержка",
        description = "Устойчивое удержание яркости на задержке",
        type = "BREATHING_BOX",
        durationMs = PHASE_MS,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = PHASE_MS, color = HOLD_COLOR, fadeInMs = 400, fadeOutMs = 400)),
        ),
    )

    private fun exhale(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_BOX_EXHALE,
        title = "Квадратное дыхание: выдох",
        description = "Ровное затухание на выдохе",
        type = "BREATHING_BOX",
        durationMs = PHASE_MS,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = PHASE_MS, color = EXHALE_COLOR, fadeInMs = 0, fadeOutMs = PHASE_MS)),
        ),
    )

    private fun pause(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_BOX_PAUSE,
        title = "Квадратное дыхание: пауза",
        description = "Тихая пауза без вдоха -- свет почти гаснет",
        type = "BREATHING_BOX",
        durationMs = PHASE_MS,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = PHASE_MS, color = PAUSE_COLOR, fadeInMs = 300, fadeOutMs = 3_000)),
        ),
    )

    private fun finish(): Pattern = lightPattern(
        id = PracticePatternIds.BREATHING_BOX_FINISH,
        title = "Квадратное дыхание: завершение",
        description = "Мягкое угасание после практики",
        type = "BREATHING_BOX",
        durationMs = 10_000,
        loop = false,
        tags = listOf("internal_step"),
        tracks = listOf(
            ringTrack(clip(startMs = 0, durationMs = 10_000, color = "#607D8B", fadeInMs = 1_500, fadeOutMs = 5_000)),
        ),
    )

    /** Угловой диод, подсвеченный в течение своей фазы цикла. */
    private fun cornerAccent(ledIndex: Int, phaseIndex: Int, color: String) = ledTrack(
        index = ledIndex,
        clip(
            startMs = phaseIndex * PHASE_MS,
            durationMs = PHASE_MS,
            color = color,
            fadeInMs = 400,
            fadeOutMs = 600,
        ),
        priority = 1,
    )
}
