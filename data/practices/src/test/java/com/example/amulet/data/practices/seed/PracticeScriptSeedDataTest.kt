package com.example.amulet.data.practices.seed

import com.example.amulet.shared.domain.practices.model.PracticeScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты скриптов практик: дыхательные фазы пофазно различимы, медитации многофазны,
 * а длительности шагов согласованы с длительностью практики.
 */
class PracticeScriptSeedDataTest {

    private val practices = PracticeSeedData.getPractices().associateBy { it.id }

    private val breathingIds = listOf(
        "breathing478",
        "boxbreathing",
        "energizing",
    )

    private val meditationIds = listOf(
        "mindfulness",
        "bodyscan",
        "sleep",
        "focus",
    )

    private val breathKeywords = listOf("вдох", "выдох", "задержка")

    @Test
    fun `каждая дыхательная практика имеет скрипт`() {
        breathingIds.forEach { id ->
            assertTrue(scriptFor(id) != null, "Нет скрипта для $id")
        }
    }

    @Test
    fun `у дыхательных практик соседние шаги используют разные паттерны`() {
        breathingIds.forEach { id ->
            val steps = scriptFor(id)!!.steps
            steps.zipWithNext().forEach { (prev, next) ->
                assertTrue(
                    prev.patternId != next.patternId,
                    "Соседние шаги делят паттерн в $id: ${prev.title} -> ${next.title}",
                )
            }
        }
    }

    @Test
    fun `порядок шагов строго возрастает`() {
        (breathingIds + meditationIds).forEach { id ->
            val orders = scriptFor(id)!!.steps.map { it.order }
            assertEquals(orders.sortedBy { it }, orders, "Порядок шагов нарушен в $id")
            assertEquals(orders.size, orders.toSet().size, "Дубли order в $id")
        }
    }

    @Test
    fun `длительность дыхательного скрипта точно равна длительности практики`() {
        breathingIds.forEach { id ->
            val target = practices.getValue(id).durationSec!!
            val sum = scriptFor(id)!!.steps.sumOf { it.durationSec ?: 0 }
            assertEquals(target, sum, "Длительность скрипта $id ($sum c) не равна практике ($target c)")
        }
    }

    @Test
    fun `медитации многофазны и покрывают длительность практики`() {
        meditationIds.forEach { id ->
            val script = scriptFor(id)!!
            val distinctPatterns = script.steps.mapNotNull { it.patternId }.toSet()
            assertTrue(distinctPatterns.size >= 3, "Медитация $id недостаточно многофазна")

            val sum = script.steps.sumOf { it.durationSec ?: 0 }
            assertEquals(
                practices.getValue(id).durationSec,
                sum,
                "Сумма фаз медитации $id не совпадает с её длительностью",
            )
        }
    }

    @Test
    fun `названия фаз медитаций не содержат дыхательных ключевых слов`() {
        // Иначе экранный визуализатор ошибочно переключится в дыхательный режим.
        meditationIds.forEach { id ->
            scriptFor(id)!!.steps.forEach { step ->
                val title = step.title?.lowercase().orEmpty()
                breathKeywords.forEach { keyword ->
                    assertTrue(
                        !title.contains(keyword),
                        "Фаза медитации $id содержит дыхательное слово '$keyword': ${step.title}",
                    )
                }
            }
        }
    }

    private fun scriptFor(id: String): PracticeScript? =
        PracticeScriptSeedData.getScriptForPractice(id)
}
