package com.example.amulet.data.practices.seed

import com.example.amulet.data.practices.seed.script.BreathingScripts
import com.example.amulet.data.practices.seed.script.MeditationScripts
import com.example.amulet.shared.domain.practices.model.PracticeScript

/**
 * Диспетчер скриптов практик: сопоставляет ID практики с её детальным пофазным скриптом.
 * Сами скрипты вынесены по категориям в подпакет [com.example.amulet.data.practices.seed.script].
 */
object PracticeScriptSeedData {

    /**
     * Возвращает скрипт практики по её ID либо null, если у практики нет детального скрипта.
     * Длительность скрипта детерминирована (фиксированное число циклов/фаз).
     */
    fun getScriptForPractice(practiceId: String): PracticeScript? =
        when (practiceId) {
            "breathing478" -> BreathingScripts.breathing478()
            "boxbreathing" -> BreathingScripts.boxBreathing()
            "energizing" -> BreathingScripts.energizingBreathing()
            "mindfulness" -> MeditationScripts.mindfulness()
            "bodyscan" -> MeditationScripts.bodyScan()
            "sleep" -> MeditationScripts.sleep()
            "focus" -> MeditationScripts.focus()
            else -> null
        }
}
