package com.example.amulet.data.patterns.seed

import com.example.amulet.shared.domain.patterns.model.TargetGroup
import com.example.amulet.shared.domain.patterns.model.TargetLed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты сид-паттернов практик: уникальность, отсутствие визуальных дублей и валидность
 * таймлайнов. Прямо защищают от исходной жалобы "спокойное дыхание == энергичное".
 */
class PracticePatternSeedsTest {

    private val patterns = PracticePatternSeeds.getPatterns()
    private val hexColor = Regex("^#[0-9A-Fa-f]{6}$")

    @Test
    fun `все идентификаторы паттернов уникальны`() {
        val ids = patterns.map { it.id.value }
        assertEquals(ids.size, ids.toSet().size, "Найдены дублирующиеся ID паттернов")
    }

    @Test
    fun `нет двух паттернов с идентичной анимацией`() {
        // spec -- data class, структурное равенство ловит визуально одинаковые паттерны.
        val specs = patterns.map { it.spec }
        assertEquals(
            specs.size,
            specs.toSet().size,
            "Есть паттерны с идентичным spec -- они будут выглядеть одинаково",
        )
    }

    @Test
    fun `длительность паттерна совпадает с длительностью его таймлайна`() {
        patterns.forEach { pattern ->
            assertEquals(
                pattern.spec.durationMs,
                pattern.spec.timeline.durationMs,
                "Рассинхрон durationMs у паттерна ${pattern.id.value}",
            )
        }
    }

    @Test
    fun `каждый трек содержит хотя бы один клип`() {
        patterns.forEach { pattern ->
            pattern.spec.timeline.tracks.forEach { track ->
                assertTrue(
                    track.clips.isNotEmpty(),
                    "Пустой трек в паттерне ${pattern.id.value}",
                )
            }
        }
    }

    @Test
    fun `все клипы укладываются в длительность и имеют корректные затухания`() {
        patterns.forEach { pattern ->
            val total = pattern.spec.timeline.durationMs
            pattern.spec.timeline.tracks.forEach { track ->
                track.clips.forEach { clip ->
                    assertTrue(clip.durationMs > 0, "Нулевой клип в ${pattern.id.value}")
                    assertTrue(clip.startMs >= 0, "Отрицательный старт в ${pattern.id.value}")
                    assertTrue(
                        clip.startMs + clip.durationMs <= total,
                        "Клип выходит за длительность в ${pattern.id.value}",
                    )
                    assertTrue(
                        clip.fadeInMs >= 0 && clip.fadeOutMs >= 0,
                        "Отрицательное затухание в ${pattern.id.value}",
                    )
                    assertTrue(
                        clip.fadeInMs + clip.fadeOutMs <= clip.durationMs,
                        "Затухания длиннее клипа в ${pattern.id.value}",
                    )
                }
            }
        }
    }

    @Test
    fun `все цвета заданы валидным hex`() {
        patterns.forEach { pattern ->
            pattern.spec.timeline.tracks.forEach { track ->
                track.clips.forEach { clip ->
                    assertTrue(
                        hexColor.matches(clip.color),
                        "Невалидный цвет ${clip.color} в ${pattern.id.value}",
                    )
                }
            }
        }
    }

    @Test
    fun `индексы светодиодов в пределах кольца`() {
        patterns.forEach { pattern ->
            pattern.spec.timeline.tracks.forEach { track ->
                when (val target = track.target) {
                    is TargetLed -> assertTrue(
                        target.index in 0 until RING_LED_COUNT,
                        "Индекс диода вне кольца в ${pattern.id.value}",
                    )
                    is TargetGroup -> target.indices.forEach { index ->
                        assertTrue(
                            index in 0 until RING_LED_COUNT,
                            "Индекс группы вне кольца в ${pattern.id.value}",
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}
