package com.example.amulet.data.practices.seed

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты сид-практик: все практики уникальны и ни одна не делит обзорный паттерн с другой.
 * Защищает от ситуации "две практики выглядят одинаково".
 */
class PracticeSeedDataTest {

    private val practices = PracticeSeedData.getPractices()

    @Test
    fun `все идентификаторы практик уникальны`() {
        val ids = practices.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Найдены дублирующиеся ID практик")
    }

    @Test
    fun `у каждой практики задан обзорный паттерн`() {
        practices.forEach { practice ->
            assertTrue(
                practice.patternId != null,
                "У практики ${practice.id} не задан patternId",
            )
        }
    }

    @Test
    fun `ни одна практика не делит обзорный паттерн с другой`() {
        val patternIds = practices.mapNotNull { it.patternId?.value }
        assertEquals(
            patternIds.size,
            patternIds.toSet().size,
            "Несколько практик ссылаются на один и тот же обзорный паттерн",
        )
    }
}
