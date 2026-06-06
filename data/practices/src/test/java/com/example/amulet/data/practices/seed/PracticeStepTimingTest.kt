package com.example.amulet.data.practices.seed

import com.example.amulet.data.practices.mapper.toDomain
import com.example.amulet.data.practices.mapper.toEntity
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Регрессия: у практик без пофазного скрипта (в т.ч. медитативного типа) шаги авто-скрипта
 * должны иметь ненулевую длительность. Иначе логика перехода между шагами прыгает сразу на
 * последний шаг и замирает.
 */
class PracticeStepTimingTest {

    // Медитативные практики без детального скрипта (играют обзорный паттерн, шаги -- текст).
    private val scriptlessMeditationIds = listOf(
        "morning",
        "stressrelief",
        "anxiety",
        "mood",
        "deepmeditation",
    )

    private val practices = PracticeSeedData.getPractices().associateBy { it.id }

    @Test
    fun `у медитаций без скрипта шаги имеют положительную длительность`() {
        scriptlessMeditationIds.forEach { id ->
            // Полный путь сидирования: domain -> seed -> entity -> domain (как в приложении).
            val domain = practices.getValue(id).toSeed().toEntity().toDomain()
            val steps = domain.script?.steps.orEmpty()

            assertTrue(steps.isNotEmpty(), "У практики $id нет шагов скрипта")
            steps.forEach { step ->
                assertTrue(
                    (step.durationSec ?: 0) > 0,
                    "Шаг '${step.description}' практики $id без длительности -- таймлайн застрянет",
                )
            }
        }
    }
}
