package com.example.amulet.data.practices.seed.script

import com.example.amulet.shared.domain.patterns.model.PracticePatternIds
import com.example.amulet.shared.domain.practices.model.PracticeScript
import com.example.amulet.shared.domain.practices.model.PracticeStep
import com.example.amulet.shared.domain.practices.model.PracticeStepType

/**
 * Скрипты дыхательных практик с пофазными паттернами.
 *
 * Каждая фаза цикла (вдох/задержка/выдох/пауза/активное/отдых) ссылается на собственный
 * световой паттерн, поэтому амулет визуально различает фазы дыхания.
 */
internal object BreathingScripts {

    /**
     * "Дыхание 4-7-8": вдох 4 сек, задержка 7 сек, выдох 8 сек. Один цикл -- 19 секунд.
     * 15 циклов + подготовка 5 c + завершение 10 c = ровно 5 минут.
     */
    fun breathing478(): PracticeScript {
        val cycles = 15
        val steps = mutableListOf<PracticeStep>()
        steps.add(
            textStep(
                order = 0,
                title = "Подготовка",
                description = "Устройтесь удобно, закройте глаза. Следуйте за светом амулета.",
                durationSec = 5,
                patternId = PracticePatternIds.BREATHING_478_PREPARE,
            ),
        )
        repeat(cycles) { cycleIndex ->
            val baseOrder = 1 + cycleIndex * 3
            steps.add(breathStep(baseOrder, "Вдох", "Медленно вдохните через нос", 4, PracticePatternIds.BREATHING_478_INHALE))
            steps.add(breathStep(baseOrder + 1, "Задержка", "Задержите дыхание", 7, PracticePatternIds.BREATHING_478_HOLD))
            steps.add(breathStep(baseOrder + 2, "Выдох", "Медленно выдохните через рот", 8, PracticePatternIds.BREATHING_478_EXHALE))
        }
        steps.add(
            textStep(
                order = steps.size,
                title = "Завершение",
                description = "Отлично! Сделайте несколько свободных вдохов.",
                durationSec = 10,
                patternId = PracticePatternIds.BREATHING_478_FINISH,
            ),
        )
        return PracticeScript(steps = steps)
    }

    /**
     * "Квадратное дыхание": четыре фазы по 4 секунды (вдох, задержка, выдох, пауза).
     * Один цикл -- 16 секунд; 25 циклов + подготовка 10 c + завершение 10 c = ровно 7 минут.
     */
    fun boxBreathing(cycles: Int = 25): PracticeScript {
        val steps = mutableListOf<PracticeStep>()
        steps.add(
            textStep(
                order = 0,
                title = "Подготовка",
                description = "Устройтесь удобно. Каждая фаза длится 4 секунды.",
                durationSec = 10,
                patternId = PracticePatternIds.BREATHING_BOX_PREPARE,
            ),
        )
        repeat(cycles) { cycleIndex ->
            val baseOrder = 1 + cycleIndex * 4
            steps.add(breathStep(baseOrder, "Вдох", "Плавный вдох через нос", 4, PracticePatternIds.BREATHING_BOX_INHALE))
            steps.add(breathStep(baseOrder + 1, "Задержка", "Задержите дыхание", 4, PracticePatternIds.BREATHING_BOX_HOLD))
            steps.add(breathStep(baseOrder + 2, "Выдох", "Выдохните через нос или рот", 4, PracticePatternIds.BREATHING_BOX_EXHALE))
            steps.add(breathStep(baseOrder + 3, "Пауза", "Пауза без вдоха", 4, PracticePatternIds.BREATHING_BOX_PAUSE))
        }
        steps.add(
            textStep(
                order = steps.size,
                title = "Завершение",
                description = "Практика завершена. Отличная работа!",
                durationSec = 10,
                patternId = PracticePatternIds.BREATHING_BOX_FINISH,
            ),
        )
        return PracticeScript(steps = steps)
    }

    /**
     * "Бодрящее дыхание": раунды активного дыхания (20 сек) с отдыхом (10 сек).
     * 5 раундов + подготовка 15 c + завершение 15 c = ровно 3 минуты.
     */
    fun energizingBreathing(rounds: Int = 5): PracticeScript {
        val steps = mutableListOf<PracticeStep>()
        steps.add(
            textStep(
                order = 0,
                title = "Подготовка",
                description = "Сядьте с прямой спиной. Приготовьтесь к активному дыханию.",
                durationSec = 15,
                patternId = PracticePatternIds.BREATHING_ENERGIZING_PREPARE,
            ),
        )
        repeat(rounds) { roundIndex ->
            val baseOrder = 1 + roundIndex * 2
            steps.add(
                breathStep(baseOrder, "Активная фаза", "Быстрые короткие вдохи-выдохи через нос", 20, PracticePatternIds.BREATHING_ENERGIZING_ACTIVE),
            )
            steps.add(
                breathStep(baseOrder + 1, "Отдых", "Свободное дыхание, расслабьтесь", 10, PracticePatternIds.BREATHING_ENERGIZING_REST),
            )
        }
        steps.add(
            textStep(
                order = steps.size,
                title = "Завершение",
                description = "Почувствуйте прилив энергии!",
                durationSec = 15,
                patternId = PracticePatternIds.BREATHING_ENERGIZING_FINISH,
            ),
        )
        return PracticeScript(steps = steps)
    }

    private fun breathStep(order: Int, title: String, description: String, durationSec: Int, patternId: String) =
        PracticeStep(
            order = order,
            type = PracticeStepType.BREATH_STEP,
            title = title,
            description = description,
            durationSec = durationSec,
            patternId = patternId,
        )

    private fun textStep(order: Int, title: String, description: String, durationSec: Int, patternId: String) =
        PracticeStep(
            order = order,
            type = PracticeStepType.TEXT_HINT,
            title = title,
            description = description,
            durationSec = durationSec,
            patternId = patternId,
        )
}
