package com.example.amulet.shared.domain.patterns.model

/**
 * Единый источник истины для строковых идентификаторов световых паттернов практик.
 *
 * Определения паттернов (модуль data:patterns) и скрипты практик (модуль data:practices)
 * ссылаются на одни и те же ID. Хранение их здесь, в общем модуле, исключает класс багов
 * «опечатка в строке -> практика ссылается на несуществующий паттерн».
 *
 * Соглашение об именовании: префикс `light_` (световой паттерн), затем полное имя практики,
 * затем фаза: `light_<practice>` для обзорного паттерна и `light_<practice>_<phase>` для фаз.
 */
object PracticePatternIds {

    // === Дыхание 4-7-8 (эталон) ===
    const val BREATHING_478 = "light_breathing478"
    const val BREATHING_478_PREPARE = "light_breathing478_prepare"
    const val BREATHING_478_INHALE = "light_breathing478_inhale"
    const val BREATHING_478_HOLD = "light_breathing478_hold"
    const val BREATHING_478_EXHALE = "light_breathing478_exhale"
    const val BREATHING_478_FINISH = "light_breathing478_finish"

    // === Квадратное дыхание 4-4-4-4 ===
    const val BREATHING_BOX = "light_boxbreathing"
    const val BREATHING_BOX_PREPARE = "light_boxbreathing_prepare"
    const val BREATHING_BOX_INHALE = "light_boxbreathing_inhale"
    const val BREATHING_BOX_HOLD = "light_boxbreathing_hold"
    const val BREATHING_BOX_EXHALE = "light_boxbreathing_exhale"
    const val BREATHING_BOX_PAUSE = "light_boxbreathing_pause"
    const val BREATHING_BOX_FINISH = "light_boxbreathing_finish"

    // === Бодрящее дыхание ===
    const val BREATHING_ENERGIZING = "light_energizing"
    const val BREATHING_ENERGIZING_PREPARE = "light_energizing_prepare"
    const val BREATHING_ENERGIZING_ACTIVE = "light_energizing_active"
    const val BREATHING_ENERGIZING_REST = "light_energizing_rest"
    const val BREATHING_ENERGIZING_FINISH = "light_energizing_finish"

    // === Медитация: осознанность ===
    const val MEDITATION_MINDFULNESS = "light_mindfulness"
    const val MEDITATION_MINDFULNESS_SETTLE = "light_mindfulness_settle"
    const val MEDITATION_MINDFULNESS_ANCHOR = "light_mindfulness_anchor"
    const val MEDITATION_MINDFULNESS_OBSERVE = "light_mindfulness_observe"
    const val MEDITATION_MINDFULNESS_RETURN = "light_mindfulness_return"

    // === Медитация: сканирование тела ===
    const val MEDITATION_BODY_SCAN = "light_bodyscan"
    const val MEDITATION_BODY_SCAN_SETTLE = "light_bodyscan_settle"
    const val MEDITATION_BODY_SCAN_LOWER = "light_bodyscan_lower"
    const val MEDITATION_BODY_SCAN_TORSO = "light_bodyscan_torso"
    const val MEDITATION_BODY_SCAN_UPPER = "light_bodyscan_upper"
    const val MEDITATION_BODY_SCAN_WHOLE = "light_bodyscan_whole"

    // === Медитация: перед сном ===
    const val MEDITATION_SLEEP = "light_sleep"
    const val MEDITATION_SLEEP_SETTLE = "light_sleep_settle"
    const val MEDITATION_SLEEP_WAVE = "light_sleep_wave"
    const val MEDITATION_SLEEP_IMAGERY = "light_sleep_imagery"
    const val MEDITATION_SLEEP_DISSOLVE = "light_sleep_dissolve"

    // === Медитация: фокус ===
    const val MEDITATION_FOCUS = "light_focus"
    const val MEDITATION_FOCUS_CHOOSE = "light_focus_choose"
    const val MEDITATION_FOCUS_STABILIZE = "light_focus_stabilize"
    const val MEDITATION_FOCUS_SUSTAIN = "light_focus_sustain"
    const val MEDITATION_FOCUS_RETURN = "light_focus_return"

    // === Прочие практики (один уникальный паттерн на практику) ===
    const val SOUNDSCAPE_OCEAN = "light_ocean"
    const val SOUNDSCAPE_FOREST = "light_forest"
    const val MIXED_MORNING = "light_morning"
    const val MIXED_STRESS_RELIEF = "light_stressrelief"
    const val MIXED_ANXIETY = "light_anxiety"
    const val MIXED_MOOD = "light_mood"
    const val ADVANCED_DEEP_MEDITATION = "light_deepmeditation"
}
