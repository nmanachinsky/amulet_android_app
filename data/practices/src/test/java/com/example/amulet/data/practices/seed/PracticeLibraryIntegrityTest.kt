package com.example.amulet.data.practices.seed

import com.example.amulet.data.patterns.seed.PracticePatternSeeds
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Межмодульная проверка целостности библиотеки практик.
 *
 * Модули data:patterns и data:practices связаны лишь строковыми ID паттернов. Тест
 * гарантирует, что каждый паттерн, на который ссылается практика или её скрипт, реально
 * определён в [PracticePatternSeeds]. data:patterns подключён только как testImplementation.
 */
class PracticeLibraryIntegrityTest {

    private val definedPatternIds = PracticePatternSeeds.getPatterns().map { it.id.value }.toSet()
    private val practices = PracticeSeedData.getPractices()

    @Test
    fun `обзорные паттерны всех практик существуют`() {
        practices.forEach { practice ->
            val patternId = practice.patternId?.value
            assertTrue(
                patternId != null && patternId in definedPatternIds,
                "Практика ${practice.id} ссылается на несуществующий паттерн $patternId",
            )
        }
    }

    @Test
    fun `все паттерны шагов скриптов существуют`() {
        practices.forEach { practice ->
            val script = PracticeScriptSeedData.getScriptForPractice(practice.id)
                ?: return@forEach
            script.steps.forEach { step ->
                val patternId = step.patternId ?: return@forEach
                assertTrue(
                    patternId in definedPatternIds,
                    "Шаг '${step.title}' практики ${practice.id} ссылается на несуществующий паттерн $patternId",
                )
            }
        }
    }
}
