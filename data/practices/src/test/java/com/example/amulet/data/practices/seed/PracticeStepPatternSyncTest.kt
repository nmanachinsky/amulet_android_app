package com.example.amulet.data.practices.seed

import com.example.amulet.data.patterns.seed.PracticePatternSeeds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Главный инвариант синхронизации устройства и телефона.
 *
 * На устройство шаг скрипта передаётся только как patternId (без durationSec), поэтому
 * длительность шага на амулете задаётся длительностью самого паттерна. Телефон же двигается
 * по step.durationSec. Чтобы экран и свет шли в ногу, для каждого шага должно выполняться
 * pattern.durationMs == step.durationSec * 1000, а паттерн шага -- одноразовый (loop=false).
 */
class PracticeStepPatternSyncTest {

    private val patternsById = PracticePatternSeeds.getPatterns().associateBy { it.id.value }
    private val practices = PracticeSeedData.getPractices()

    @Test
    fun `длительность паттерна каждого шага равна длительности шага`() {
        forEachScriptedStep { practiceId, step ->
            val pattern = patternsById[step.patternId]
            assertNotNull(pattern, "Шаг '${step.title}' практики $practiceId ссылается на несуществующий паттерн ${step.patternId}")

            val stepMs = (step.durationSec ?: 0) * 1000
            assertEquals(
                pattern!!.spec.durationMs,
                stepMs,
                "Рассинхрон: шаг '${step.title}' ($practiceId) длится $stepMs мс, а паттерн ${step.patternId} -- ${pattern.spec.durationMs} мс",
            )
        }
    }

    @Test
    fun `паттерны шагов скриптов одноразовые`() {
        forEachScriptedStep { practiceId, step ->
            val pattern = patternsById.getValue(step.patternId!!)
            assertFalse(
                pattern.spec.loop,
                "Паттерн шага '${step.title}' ($practiceId) зациклен -- устройство не перейдёт к следующему шагу",
            )
        }
    }

    private fun forEachScriptedStep(action: (practiceId: String, step: com.example.amulet.shared.domain.practices.model.PracticeStep) -> Unit) {
        practices.forEach { practice ->
            val script = PracticeScriptSeedData.getScriptForPractice(practice.id) ?: return@forEach
            script.steps.forEach { step ->
                if (step.patternId != null) action(practice.id, step)
            }
        }
    }
}
