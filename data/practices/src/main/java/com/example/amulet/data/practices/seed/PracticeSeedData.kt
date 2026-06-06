package com.example.amulet.data.practices.seed

import com.example.amulet.data.practices.seed.script.BreathingScripts
import com.example.amulet.data.practices.seed.script.MeditationScripts
import com.example.amulet.shared.domain.practices.model.Practice
import com.example.amulet.shared.domain.practices.model.PracticeGoal
import com.example.amulet.shared.domain.practices.model.PracticeLevel
import com.example.amulet.shared.domain.practices.model.PracticeType
import com.example.amulet.shared.domain.practices.model.totalDurationSec
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.model.PracticePatternIds

/**
 * Seed данные практик с привязкой к паттернам.
 *
 * Длительность практик со скриптом берётся из самого скрипта ([totalDurationSec]) -- единый
 * источник истины: показанная длительность всегда равна фактической длине скрипта.
 */
object PracticeSeedData {

    // Длительности практик со скриптом = сумма шагов соответствующего скрипта.
    private val breathing478Sec = BreathingScripts.breathing478().totalDurationSec()
    private val boxBreathingSec = BreathingScripts.boxBreathing().totalDurationSec()
    private val energizingSec = BreathingScripts.energizingBreathing().totalDurationSec()
    private val mindfulnessSec = MeditationScripts.mindfulness().totalDurationSec()
    private val bodyScanSec = MeditationScripts.bodyScan().totalDurationSec()
    private val sleepMeditationSec = MeditationScripts.sleep().totalDurationSec()
    private val focusMeditationSec = MeditationScripts.focus().totalDurationSec()

    fun getPractices(): List<Practice> = listOf(
        // === ДЫХАТЕЛЬНЫЕ ПРАКТИКИ ===

        Practice(
            id = "breathing478",
            type = PracticeType.BREATH,
            title = "Дыхание 4-7-8",
            description = "Классическая техника для быстрого засыпания. Вдох на 4 счета, задержка на 7, выдох на 8. Помогает успокоить нервную систему.",
            durationSec = breathing478Sec,
            level = PracticeLevel.BEGINNER,
            goal = PracticeGoal.SLEEP,
            tags = listOf("сон", "успокоение", "вечер"),
            contraindications = listOf("Астма в острой фазе", "ХОБЛ"),
            patternId = PatternId(PracticePatternIds.BREATHING_478),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Подготовка: сядьте удобно и сделайте спокойный выдох через рот",
                "Вдох через нос на 4 счёта",
                "Задержка дыхания на 7 счётов",
                "Выдох через рот на 8 счётов",
                "Повторите цикл 15 раз, следуя за светом амулета"
            ),
            safetyNotes = listOf(
                "Лучше выполнять сидя или лёжа в безопасной обстановке",
                "Не выполнять во время вождения и работы с механизмами"
            )
        ),
        
        Practice(
            id = "boxbreathing",
            type = PracticeType.BREATH,
            title = "Квадратное дыхание",
            description = "Техника Navy SEAL для снятия стресса. Равные фазы по 4 секунды: вдох, задержка, выдох, задержка. Идеально для быстрого восстановления спокойствия.",
            durationSec = boxBreathingSec,
            level = PracticeLevel.BEGINNER,
            goal = PracticeGoal.STRESS,
            tags = listOf("стресс", "концентрация", "баланс"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.BREATHING_BOX),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Подготовка: каждая фаза длится 4 счёта",
                "Вдох через нос на 4 счёта",
                "Задержка дыхания на 4 счёта",
                "Выдох через нос или рот на 4 счёта",
                "Пауза без вдоха на 4 счёта, затем повторите цикл (25 раз)"
            ),
            safetyNotes = listOf(
                "Выполняйте сидя с опорой для спины",
                "Остановитесь при сильном головокружении или дискомфорте"
            )
        ),
        
        Practice(
            id = "energizing",
            type = PracticeType.BREATH,
            title = "Бодрящее дыхание",
            description = "Активная дыхательная практика для пробуждения. Быстрые вдохи-выдохи повышают уровень энергии и ясность ума.",
            durationSec = energizingSec,
            level = PracticeLevel.INTERMEDIATE,
            goal = PracticeGoal.ENERGY,
            tags = listOf("энергия", "утро", "бодрость"),
            contraindications = listOf("Гипертония", "Беременность", "Головокружения"),
            patternId = PatternId(PracticePatternIds.BREATHING_ENERGIZING),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Подготовка: сядьте с прямой спиной, сделайте несколько спокойных вдохов",
                "Активная фаза: 20 секунд быстрых коротких вдохов-выдохов через нос",
                "Отдых: 10 секунд свободного спокойного дыхания",
                "Повторите 5 раундов активной фазы и отдыха",
                "Завершите практику одним глубоким очищающим выдохом"
            ),
            safetyNotes = listOf(
                "Выполняйте только сидя или стоя, не лёжа",
                "Не выполнять при выраженном недомогании, болях в груди или головокружении"
            )
        ),
        
        // === МЕДИТАЦИИ ===
        
        Practice(
            id = "mindfulness",
            type = PracticeType.MEDITATION,
            title = "Осознанность для начинающих",
            description = "Базовая практика майндфулнесс. Фокус на дыхании и наблюдение за мыслями без суждения. Идеально для первого знакомства с медитацией.",
            durationSec = mindfulnessSec,
            level = PracticeLevel.BEGINNER,
            goal = PracticeGoal.RELAXATION,
            tags = listOf("осознанность", "начинающие", "базовая"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.MEDITATION_MINDFULNESS),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Настройка: устройтесь удобно сидя, выровняйте спину, мягко прикройте глаза",
                "Дыхание-якорь: перенесите внимание на вдохи и выдохи в теле",
                "Наблюдение: замечайте мысли и звуки, не вовлекаясь, и возвращайтесь к дыханию",
                "Возврат: сделайте несколько более глубоких вдохов и мягко откройте глаза"
            ),
            safetyNotes = listOf(
                "Лучше выполнять сидя с устойчивой опорой",
                "При повышенной сонливости можно выполнять с открытыми глазами"
            )
        ),
        
        Practice(
            id = "bodyscan",
            type = PracticeType.MEDITATION,
            title = "Сканирование тела",
            description = "Постепенное расслабление каждой части тела от головы до пят. Снимает физическое и эмоциональное напряжение.",
            durationSec = bodyScanSec,
            level = PracticeLevel.BEGINNER,
            goal = PracticeGoal.RELAXATION,
            tags = listOf("расслабление", "тело", "напряжение"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.MEDITATION_BODY_SCAN),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Настройка: лягте или сядьте удобно, дайте телу опереться на поверхность",
                "Стопы и ноги: перенесите внимание вниз и постепенно поднимайтесь к бёдрам",
                "Корпус: расслабьте таз, живот, грудь и спину, отмечая ощущения",
                "Руки, шея и лицо: поднимитесь вниманием к верхней части тела",
                "Всё тело: охватите вниманием тело целиком и поблагодарите себя за практику"
            ),
            safetyNotes = listOf(
                "Оптимально выполнять лёжа или полулёжа в спокойной обстановке",
                "Не выполнять за рулём и при необходимости сохранять активное внимание"
            )
        ),
        
        Practice(
            id = "sleep",
            type = PracticeType.MEDITATION,
            title = "Медитация перед сном",
            description = "Специальная практика для глубокого засыпания. Визуализации и расслабляющие техники помогают отпустить день и погрузиться в сон.",
            durationSec = sleepMeditationSec,
            level = PracticeLevel.INTERMEDIATE,
            goal = PracticeGoal.SLEEP,
            tags = listOf("сон", "вечер", "визуализация"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.MEDITATION_SLEEP),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Успокоение: устройтесь в позе для сна и отпускайте события дня на выдохе",
                "Волна расслабления: представьте мягкую волну, идущую от стоп к голове",
                "Образ покоя: создайте спокойный образ — безопасное место, тёплый свет или природу",
                "Растворение: позвольте вниманию раствориться в ощущении покоя и тяжести тела"
            ),
            safetyNotes = listOf(
                "Выполнять только перед сном или в условиях, где можно заснуть",
                "Не использовать в ситуациях, требующих концентрации (вождение, работа)"
            )
        ),
        
        Practice(
            id = "focus",
            type = PracticeType.MEDITATION,
            title = "Медитация для фокуса",
            description = "Тренировка концентрации через фокус на одной точке. Улучшает способность к длительной сосредоточенности.",
            durationSec = focusMeditationSec,
            level = PracticeLevel.INTERMEDIATE,
            goal = PracticeGoal.FOCUS,
            tags = listOf("концентрация", "фокус", "работа"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.MEDITATION_FOCUS),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Выбор объекта: выберите точку, дыхание или внутренний образ",
                "Стабилизация: удерживайте мягкое устойчивое внимание на объекте",
                "Удержание: при отвлечении спокойно возвращайте внимание обратно",
                "Возврат: сделайте несколько глубоких вдохов и перенесите фокус в комнату"
            ),
            safetyNotes = listOf(
                "Лучше выполнять сидя за столом или в рабочей обстановке",
                "Избегать выполнения при сильной усталости или сонливости"
            )
        ),
        
        // === ЗВУКОВЫЕ ЛАНДШАФТЫ ===
        
        Practice(
            id = "ocean",
            type = PracticeType.SOUND,
            title = "Звуки океана",
            description = "Погружение в атмосферу морского прибоя. Ритм волн естественным образом синхронизируется с дыханием, создавая глубокое расслабление.",
            durationSec = 1800, // 30 минут
            level = PracticeLevel.BEGINNER,
            goal = PracticeGoal.RELAXATION,
            tags = listOf("природа", "океан", "расслабление"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.SOUNDSCAPE_OCEAN),
            audioUrl = "soundscapes/ocean.mp3",
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Устройтесь удобно, закройте глаза или смягчите взгляд",
                "Начните слушать звуки океана, отмечая ритм волн и паузы между ними",
                "Синхронизируйте дыхание с волнами: мягкий вдох на подъёме звука, выдох на спаде",
                "Позвольте мыслям свободно приходить и уходить, удерживая внимание на звуках",
                "В конце практики сделайте несколько осознанных вдохов и вернитесь к окружающему пространству"
            ),
            safetyNotes = listOf(
                "Можно выполнять сидя или лёжа, лучше в наушниках",
                "Не рекомендуется использовать во время управления транспортом"
            )
        ),
        
        Practice(
            id = "forest",
            type = PracticeType.SOUND,
            title = "Лесные звуки",
            description = "Пение птиц, шелест листвы, журчание ручья. Создает ощущение присутствия в лесу, снимает городскую усталость.",
            durationSec = 1800, // 30 минут
            level = PracticeLevel.BEGINNER,
            goal = PracticeGoal.STRESS,
            tags = listOf("природа", "лес", "птицы"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.SOUNDSCAPE_FOREST),
            audioUrl = "soundscapes/forest.mp3",
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Сядьте или лягте комфортно, почувствуйте опору под телом",
                "Начните прислушиваться ко всем слоям звука: птицы, листва, вода",
                "Выберите один звук и некоторое время удерживайте на нём внимание",
                "Расширьте фокус, охватывая сразу весь звуковой фон как единое целое",
                "Отметьте, как меняется состояние тела и ума по мере погружения в звук"
            ),
            safetyNotes = listOf(
                "Рекомендуется выполнять в спокойной, безопасной обстановке",
                "При повышенной тревожности можно оставить немного света или выполнять с открытыми глазами"
            )
        ),
        
        // === СМЕШАННЫЕ ===
        
        Practice(
            id = "morning",
            type = PracticeType.MEDITATION,
            title = "Утренний ритуал",
            description = "Комбинация бодрящего дыхания, легкой медитации и аффирмаций. Идеальное начало продуктивного дня.",
            durationSec = 600, // 10 минут
            level = PracticeLevel.BEGINNER,
            goal = PracticeGoal.ENERGY,
            tags = listOf("утро", "энергия", "ритуал"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.MIXED_MORNING),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Сделайте несколько бодрящих вдохов и мягких потягиваний тела",
                "Выполните короткий цикл активного дыхания для пробуждения",
                "Перейдите к нескольким минутам спокойного наблюдения за дыханием",
                "Произнесите про себя или вслух 1–3 поддерживающие аффирмации на день",
                "Закончите практику, наметив один маленький осознанный шаг на ближайший час"
            ),
            safetyNotes = listOf(
                "Лучше выполнять утром, до кофе и плотного приёма пищи",
                "При проблемах с давлением делать дыхательные части практики мягче"
            )
        ),
        
        Practice(
            id = "stressrelief",
            type = PracticeType.MEDITATION,
            title = "Экспресс-снятие стресса",
            description = "Быстрая комбинация дыхания и точечного расслабления. Эффективно снимает острый стресс за 5 минут.",
            durationSec = 300, // 5 минут
            level = PracticeLevel.BEGINNER,
            goal = PracticeGoal.STRESS,
            tags = listOf("стресс", "быстро", "работа"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.MIXED_STRESS_RELIEF),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Сделайте несколько глубоких выдохов, отпуская напряжение плеч и шеи",
                "Выполните короткий цикл ровного дыхания (например, 4–4–4–4)",
                "Перенесите внимание на одну напряжённую область тела и мягко её расслабьте",
                "Сделайте ещё один цикл дыхания, наблюдая, как снижается эмоциональное напряжение",
                "Сформулируйте для себя один небольшой поддерживающий настрой на ближайшее время"
            ),
            safetyNotes = listOf(
                "Оптимально выполнять сидя в относительно спокойной обстановке (кабинет, переговорная)",
                "Можно использовать в коротких перерывах между задачами"
            )
        ),
        
        Practice(
            id = "anxiety",
            type = PracticeType.MEDITATION,
            title = "Работа с тревожностью",
            description = "Специализированная практика для снижения тревоги. Сочетает успокаивающее дыхание, заземление и когнитивные техники.",
            durationSec = 720, // 12 минут
            level = PracticeLevel.INTERMEDIATE,
            goal = PracticeGoal.ANXIETY,
            tags = listOf("тревога", "заземление", "спокойствие"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.MIXED_ANXIETY),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Заметьте уровень тревоги по субъективной шкале от 0 до 10",
                "Сделайте несколько циклов успокаивающего дыхания с удлинённым выдохом",
                "Переведите внимание на опору: стопы, поверхность стула или кровати",
                "Назовите про себя 3–5 объектов вокруг и 3–5 ощущений в теле, закрепляя себя в настоящем моменте",
                "Снова оцените уровень тревоги и отметьте, что изменилось"
            ),
            safetyNotes = listOf(
                "Лучше выполнять сидя, с возможностью опереться спиной и ногами",
                "При очень высокой тревоге можно сократить длительность и повторять чаще в течение дня"
            )
        ),
        
        Practice(
            id = "mood",
            type = PracticeType.MEDITATION,
            title = "Улучшение настроения",
            description = "Позитивная практика для поднятия духа. Визуализации, дыхание и приятные звуки помогают выйти из подавленного состояния.",
            durationSec = 600, // 10 минут
            level = PracticeLevel.BEGINNER,
            goal = PracticeGoal.MOOD,
            tags = listOf("настроение", "позитив", "радость"),
            contraindications = listOf(),
            patternId = PatternId(PracticePatternIds.MIXED_MOOD),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Сделайте несколько мягких вдохов и выдохов, замечая текущее состояние",
                "Вспомните один приятный момент из недавнего прошлого и погрузитесь в его детали",
                "Отметьте в теле любые даже самые небольшие приятные ощущения",
                "Представьте светлый, поддерживающий образ или цвет, заполняющий пространство вокруг",
                "В завершение сформулируйте одно маленькое действие, которое может поддержать ваше настроение сегодня"
            ),
            safetyNotes = listOf(
                "Можно выполнять сидя или лёжа, в безопасной и спокойной обстановке",
                "Не использовать как замену профессиональной помощи при выраженных депрессивных состояниях"
            )
        ),
        
        // === ПРОДВИНУТЫЕ ===
        
        Practice(
            id = "deepmeditation",
            type = PracticeType.MEDITATION,
            title = "Глубокая медитация",
            description = "Продолжительная практика для опытных медитирующих. Глубокие состояния сознания и внутреннее исследование.",
            durationSec = 1800, // 30 минут
            level = PracticeLevel.ADVANCED,
            goal = PracticeGoal.FOCUS,
            tags = listOf("глубокая", "продвинутая", "исследование"),
            contraindications = listOf("Психические расстройства без контроля специалиста"),
            patternId = PatternId(PracticePatternIds.ADVANCED_DEEP_MEDITATION),
            audioUrl = null,
            isFavorite = false,
            usageCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = listOf(
                "Создайте устойчивые внешние условия: тишина, удобная поза, отсутствие отвлечений",
                "Проведите несколько минут в стабилизирующей практике дыхания или лёгкого сканирования тела",
                "Перейдите к устойчивому фокусу (объект медитации) и удерживайте внимание в выбранной точке",
                "Позвольте вниманию мягко углубляться, наблюдая возникающие состояния без оценки и анализа",
                "Завершите практику постепенным возвращением к ощущениям в теле и пространству комнаты"
            ),
            safetyNotes = listOf(
                "Рекомендуется опытным практикам, в условиях, где можно позволить себе длительное погружение",
                "При обострении психических состояний практику нужно согласовывать со специалистом"
            )
        )
    )
}
