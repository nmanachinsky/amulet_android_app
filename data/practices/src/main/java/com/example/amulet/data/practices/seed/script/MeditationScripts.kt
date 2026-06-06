package com.example.amulet.data.practices.seed.script

import com.example.amulet.shared.domain.patterns.model.PracticePatternIds
import com.example.amulet.shared.domain.practices.model.PracticeScript
import com.example.amulet.shared.domain.practices.model.PracticeStep
import com.example.amulet.shared.domain.practices.model.PracticeStepType

/**
 * Многофазные скрипты медитаций.
 *
 * Каждая медитация ведёт через осмысленные фазы (настройка -> углубление -> ядро -> возврат),
 * и у каждой фазы свой световой паттерн. Названия фаз намеренно не содержат слов
 * "вдох"/"выдох"/"задержка", чтобы экранный визуализатор не переключался в дыхательный режим.
 */
internal object MeditationScripts {

    /** "Осознанность для начинающих" -- 10 минут. */
    fun mindfulness(): PracticeScript = scriptOf(
        phase("Настройка", "Устройтесь удобно, выровняйте спину, мягко прикройте глаза.", 90, PracticePatternIds.MEDITATION_MINDFULNESS_SETTLE),
        phase("Дыхание-якорь", "Перенесите внимание на дыхание: ощущайте вдохи и выдохи в теле.", 180, PracticePatternIds.MEDITATION_MINDFULNESS_ANCHOR),
        phase("Наблюдение", "Замечайте мысли и звуки, не вовлекаясь, мягко возвращайтесь к дыханию.", 240, PracticePatternIds.MEDITATION_MINDFULNESS_OBSERVE),
        phase("Возврат", "Сделайте несколько более глубоких вдохов и мягко откройте глаза.", 90, PracticePatternIds.MEDITATION_MINDFULNESS_RETURN),
    )

    /** "Сканирование тела" -- 15 минут. */
    fun bodyScan(): PracticeScript = scriptOf(
        phase("Настройка", "Лягте или сядьте удобно, дайте телу опереться на поверхность.", 120, PracticePatternIds.MEDITATION_BODY_SCAN_SETTLE),
        phase("Стопы и ноги", "Перенесите внимание на стопы и постепенно поднимайтесь к бёдрам.", 180, PracticePatternIds.MEDITATION_BODY_SCAN_LOWER, PracticeStepType.BODY_SCAN),
        phase("Корпус", "Расслабьте таз, живот, грудь и спину, отмечая ощущения.", 180, PracticePatternIds.MEDITATION_BODY_SCAN_TORSO, PracticeStepType.BODY_SCAN),
        phase("Руки, шея и лицо", "Поднимитесь вниманием к рукам, плечам, шее и лицу.", 180, PracticePatternIds.MEDITATION_BODY_SCAN_UPPER, PracticeStepType.BODY_SCAN),
        phase("Всё тело", "Охватите вниманием всё тело целиком и поблагодарите себя за практику.", 240, PracticePatternIds.MEDITATION_BODY_SCAN_WHOLE),
    )

    /** "Медитация перед сном" -- 20 минут. */
    fun sleep(): PracticeScript = scriptOf(
        phase("Успокоение", "Устройтесь в позе для сна и отпускайте события дня.", 240, PracticePatternIds.MEDITATION_SLEEP_SETTLE),
        phase("Волна расслабления", "Представьте мягкую волну расслабления от стоп к голове.", 360, PracticePatternIds.MEDITATION_SLEEP_WAVE),
        phase("Образ покоя", "Создайте спокойный образ: безопасное место, тёплый свет или природу.", 360, PracticePatternIds.MEDITATION_SLEEP_IMAGERY),
        phase("Растворение", "Позвольте вниманию раствориться в ощущении покоя и тяжести тела.", 240, PracticePatternIds.MEDITATION_SLEEP_DISSOLVE),
    )

    /** "Медитация для фокуса" -- 15 минут. */
    fun focus(): PracticeScript = scriptOf(
        phase("Выбор объекта", "Выберите объект внимания: точку, дыхание или внутренний образ.", 120, PracticePatternIds.MEDITATION_FOCUS_CHOOSE),
        phase("Стабилизация", "Удерживайте мягкое устойчивое внимание на выбранном объекте.", 300, PracticePatternIds.MEDITATION_FOCUS_STABILIZE),
        phase("Удержание", "Когда замечаете отвлечение, спокойно возвращайте внимание обратно.", 360, PracticePatternIds.MEDITATION_FOCUS_SUSTAIN),
        phase("Возврат", "Сделайте несколько глубоких вдохов и перенесите фокус в комнату.", 120, PracticePatternIds.MEDITATION_FOCUS_RETURN),
    )

    private data class Phase(
        val title: String,
        val description: String,
        val durationSec: Int,
        val patternId: String,
        val type: PracticeStepType,
    )

    private fun phase(
        title: String,
        description: String,
        durationSec: Int,
        patternId: String,
        type: PracticeStepType = PracticeStepType.TEXT_HINT,
    ) = Phase(title, description, durationSec, patternId, type)

    private fun scriptOf(vararg phases: Phase): PracticeScript = PracticeScript(
        steps = phases.mapIndexed { index, phase ->
            PracticeStep(
                order = index,
                type = phase.type,
                title = phase.title,
                description = phase.description,
                durationSec = phase.durationSec,
                patternId = phase.patternId,
            )
        },
    )
}
