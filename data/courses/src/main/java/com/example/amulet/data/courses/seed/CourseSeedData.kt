package com.example.amulet.data.courses.seed

import com.example.amulet.shared.domain.courses.model.CourseItemType
import com.example.amulet.shared.domain.courses.model.CourseRhythm
import com.example.amulet.shared.domain.courses.model.UnlockCondition
import com.example.amulet.shared.domain.practices.model.PracticeGoal
import com.example.amulet.shared.domain.practices.model.PracticeLevel

/**
 * Seed данные курсов с привязкой к практикам
 */
object CourseSeedData {
    
    fun getCourses(): List<CourseSeed> = listOf(
        // === КУРС 1: Сон за 7 дней ===
        CourseSeed(
            id = "course_sleep_7days",
            title = "Сон за 7 дней",
            description = "Программа для восстановления здорового сна. За неделю вы освоите техники быстрого засыпания и глубокого сна.",
            goal = PracticeGoal.SLEEP,
            level = PracticeLevel.BEGINNER,
            rhythm = CourseRhythm.DAILY,
            tags = listOf("сон", "начинающие", "вечер"),
            totalDurationSec = 7200, // ~2 часа total
            modulesCount = 7,
            recommendedDays = 7,
            difficulty = "easy",
            coverUrl = "course_sleep_7days"
        ),
        
        // === КУРС 2: Снижение стресса ===
        CourseSeed(
            id = "course_stress_relief",
            title = "Снижение стресса",
            description = "10-дневная программа для развития стрессоустойчивости. Научитесь быстро восстанавливаться и сохранять спокойствие.",
            goal = PracticeGoal.STRESS,
            level = PracticeLevel.BEGINNER,
            rhythm = CourseRhythm.DAILY,
            tags = listOf("стресс", "работа", "баланс"),
            totalDurationSec = 6000, // ~1.7 часа total
            modulesCount = 10,
            recommendedDays = 10,
            difficulty = "easy",
            coverUrl = "course_stress_relief"
        ),
        
        // === КУРС 3: Фокус и концентрация ===
        CourseSeed(
            id = "course_focus_concentration",
            title = "Фокус и концентрация",
            description = "Тренировка внимания и способности к глубокой концентрации. Идеально для работы и учебы в две недели.",
            goal = PracticeGoal.FOCUS,
            level = PracticeLevel.INTERMEDIATE,
            rhythm = CourseRhythm.THREE_TIMES_WEEK,
            tags = listOf("фокус", "работа", "продуктивность"),
            totalDurationSec = 5400, // ~1.5 часа total
            modulesCount = 6,
            recommendedDays = 14,
            difficulty = "medium",
            coverUrl = "course_focus_concentration"
        ),
        
        // === КУРС 4: Энергия на весь день ===
        CourseSeed(
            id = "course_energy_boost",
            title = "Энергия на весь день",
            description = "5-дневный курс по повышению жизненной энергии. Утренние и дневные практики для бодрости без кофеина.",
            goal = PracticeGoal.ENERGY,
            level = PracticeLevel.BEGINNER,
            rhythm = CourseRhythm.DAILY,
            tags = listOf("энергия", "утро", "бодрость"),
            totalDurationSec = 2700, // ~45 минут total
            modulesCount = 5,
            recommendedDays = 5,
            difficulty = "easy",
            coverUrl = "course_energy_boost"
        ),
        
        // === КУРС 5: Работа с тревожностью ===
        CourseSeed(
            id = "course_anxiety_management",
            title = "Работа с тревожностью",
            description = "Комплексная программа для снижения тревоги и обретения внутреннего спокойствия. Гибкий график прохождения.",
            goal = PracticeGoal.ANXIETY,
            level = PracticeLevel.INTERMEDIATE,
            rhythm = CourseRhythm.FLEXIBLE,
            tags = listOf("тревога", "спокойствие", "баланс"),
            totalDurationSec = 7200, // ~2 часа total
            modulesCount = 8,
            recommendedDays = null,
            difficulty = "medium",
            coverUrl = "course_anxiety_management"
        )
    )
    
    fun getCourseModules(): Map<String, List<CourseModuleSeed>> = mapOf(
        // === Modules for "Сон за 7 дней" ===
        "course_sleep_7days" to listOf(
            CourseModuleSeed(
                id = "course_sleep_7days_day1",
                courseId = "course_sleep_7days",
                order = 1,
                title = "День 1: Знакомство с дыханием 4-7-8"
            ),
            CourseModuleSeed(
                id = "course_sleep_7days_day2",
                courseId = "course_sleep_7days",
                order = 2,
                title = "День 2: Углубление практики"
            ),
            CourseModuleSeed(
                id = "course_sleep_7days_day3",
                courseId = "course_sleep_7days",
                order = 3,
                title = "День 3: Сканирование тела"
            ),
            CourseModuleSeed(
                id = "course_sleep_7days_day4",
                courseId = "course_sleep_7days",
                order = 4,
                title = "День 4: Звуки океана"
            ),
            CourseModuleSeed(
                id = "course_sleep_7days_day5",
                courseId = "course_sleep_7days",
                order = 5,
                title = "День 5: Медитация перед сном"
            ),
            CourseModuleSeed(
                id = "course_sleep_7days_day6",
                courseId = "course_sleep_7days",
                order = 6,
                title = "День 6: Комбинация техник"
            ),
            CourseModuleSeed(
                id = "course_sleep_7days_day7",
                courseId = "course_sleep_7days",
                order = 7,
                title = "День 7: Финальная практика"
            )
        ),
        // === Modules for "Снижение стресса" ===
        "course_stress_relief" to (1..10).map { day ->
            CourseModuleSeed(
                id = "course_stress_day$day",
                courseId = "course_stress_relief",
                order = day,
                title = "День $day"
            )
        },
        // === Modules for "Фокус и концентрация" ===
        "course_focus_concentration" to (1..6).map { index ->
            CourseModuleSeed(
                id = "course_focus_session$index",
                courseId = "course_focus_concentration",
                order = index,
                title = "Сессия $index"
            )
        },
        // === Modules for "Энергия на весь день" ===
        "course_energy_boost" to (1..5).map { day ->
            CourseModuleSeed(
                id = "course_energy_day$day",
                courseId = "course_energy_boost",
                order = day,
                title = "День $day"
            )
        },
        // === Modules for "Работа с тревожностью" ===
        "course_anxiety_management" to (1..8).map { index ->
            CourseModuleSeed(
                id = "course_anxiety_module$index",
                courseId = "course_anxiety_management",
                order = index,
                title = "Модуль $index"
            )
        }
    )

    fun getCourseItems(): Map<String, List<CourseItemSeed>> = mapOf(
        // === Items для "Сон за 7 дней" ===
        "course_sleep_7days" to listOf(
            CourseItemSeed(
                id = "course_sleep_7days_day1",
                courseId = "course_sleep_7days",
                order = 1,
                type = CourseItemType.PRACTICE,
                practiceId = "breathing478",
                title = "День 1: Знакомство с дыханием 4-7-8",
                description = "Базовая техника для засыпания",
                mandatory = true,
                minDurationSec = 300,
                moduleId = "course_sleep_7days_day1"
            ),
            CourseItemSeed(
                id = "course_sleep_7days_day2",
                courseId = "course_sleep_7days",
                order = 2,
                type = CourseItemType.PRACTICE,
                practiceId = "breathing478",
                title = "День 2: Углубление практики",
                description = "Второй день с дыханием 4-7-8",
                mandatory = true,
                minDurationSec = 300,
                moduleId = "course_sleep_7days_day2",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_sleep_7days_day3",
                courseId = "course_sleep_7days",
                order = 3,
                type = CourseItemType.PRACTICE,
                practiceId = "bodyscan",
                title = "День 3: Сканирование тела",
                description = "Расслабление каждой части тела",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_sleep_7days_day3",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_sleep_7days_day4",
                courseId = "course_sleep_7days",
                order = 4,
                type = CourseItemType.PRACTICE,
                practiceId = "ocean",
                title = "День 4: Звуки океана",
                description = "Погружение в звуки природы",
                mandatory = true,
                minDurationSec = 1800,
                moduleId = "course_sleep_7days_day4",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_sleep_7days_day5",
                courseId = "course_sleep_7days",
                order = 5,
                type = CourseItemType.PRACTICE,
                practiceId = "sleep",
                title = "День 5: Медитация перед сном",
                description = "Специализированная практика для сна",
                mandatory = true,
                minDurationSec = 1200,
                moduleId = "course_sleep_7days_day5",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_sleep_7days_day6",
                courseId = "course_sleep_7days",
                order = 6,
                type = CourseItemType.PRACTICE,
                practiceId = "bodyscan",
                title = "День 6: Комбинация техник",
                description = "Сканирование и дыхание",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_sleep_7days_day6",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_sleep_7days_day7",
                courseId = "course_sleep_7days",
                order = 7,
                type = CourseItemType.PRACTICE,
                practiceId = "sleep",
                title = "День 7: Финальная практика",
                description = "Закрепление всех техник",
                mandatory = true,
                minDurationSec = 1200,
                moduleId = "course_sleep_7days_day7",
                unlockCondition = UnlockCondition.And(
                    listOf(
                        UnlockCondition.CompletePreviousItem,
                        UnlockCondition.MinimumProgress(85)
                    )
                )
            )
        ),
        
        // === Items для "Снижение стресса" ===
        "course_stress_relief" to listOf(
            CourseItemSeed(
                id = "course_stress_day1",
                courseId = "course_stress_relief",
                order = 1,
                type = CourseItemType.PRACTICE,
                practiceId = "boxbreathing",
                title = "День 1: Квадратное дыхание",
                description = "Основа стрессоустойчивости",
                mandatory = true,
                minDurationSec = 420,
                moduleId = "course_stress_day1"
            ),
            CourseItemSeed(
                id = "course_stress_day2",
                courseId = "course_stress_relief",
                order = 2,
                type = CourseItemType.PRACTICE,
                practiceId = "stressrelief",
                title = "День 2: Экспресс-техника",
                description = "Быстрое снятие стресса",
                mandatory = true,
                minDurationSec = 300,
                moduleId = "course_stress_day2"
            ),
            CourseItemSeed(
                id = "course_stress_day3",
                courseId = "course_stress_relief",
                order = 3,
                type = CourseItemType.PRACTICE,
                practiceId = "mindfulness",
                title = "День 3: Осознанность",
                description = "Наблюдение за стрессом без реакции",
                mandatory = true,
                minDurationSec = 600,
                moduleId = "course_stress_day3"
            ),
            CourseItemSeed(
                id = "course_stress_day4",
                courseId = "course_stress_relief",
                order = 4,
                type = CourseItemType.PRACTICE,
                practiceId = "forest",
                title = "День 4: Лесные звуки",
                description = "Природная терапия",
                mandatory = false,
                minDurationSec = 1800,
                moduleId = "course_stress_day4"
            ),
            CourseItemSeed(
                id = "course_stress_day5",  
                courseId = "course_stress_relief",
                order = 5,
                type = CourseItemType.PRACTICE,
                practiceId = "boxbreathing",
                title = "День 5: Углубление дыхания",
                description = "Продвинутая работа с дыханием",
                mandatory = true,
                minDurationSec = 420,
                moduleId = "course_stress_day5"
            ),
            CourseItemSeed(
                id = "course_stress_day6",
                courseId = "course_stress_relief",
                order = 6,
                type = CourseItemType.PRACTICE,
                practiceId = "bodyscan",
                title = "День 6: Расслабление тела",
                description = "Глубокое сканирование тела для снятия напряжения",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_stress_day6"
            ),
            CourseItemSeed(
                id = "course_stress_day7",
                courseId = "course_stress_relief",
                order = 7,
                type = CourseItemType.PRACTICE,
                practiceId = "mindfulness",
                title = "День 7: Осознанность в действии",
                description = "Практика наблюдения за мыслями и эмоциями",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_stress_day7"
            ),
            CourseItemSeed(
                id = "course_stress_day8",
                courseId = "course_stress_relief",
                order = 8,
                type = CourseItemType.PRACTICE,
                practiceId = "forest",
                title = "День 8: Длительное восстановление",
                description = "Расширенная сессия с природными звуками",
                mandatory = false,
                minDurationSec = 1800,
                moduleId = "course_stress_day8"
            ),
            CourseItemSeed(
                id = "course_stress_day9",
                courseId = "course_stress_relief",
                order = 9,
                type = CourseItemType.PRACTICE,
                practiceId = "stressrelief",
                title = "День 9: Комбинированная практика",
                description = "Сочетание дыхания и осознанности",
                mandatory = true,
                minDurationSec = 600,
                moduleId = "course_stress_day9"
            ),
            CourseItemSeed(
                id = "course_stress_day10",
                courseId = "course_stress_relief",
                order = 10,
                type = CourseItemType.PRACTICE,
                practiceId = "mindfulness",
                title = "День 10: Итоговая сессия",
                description = "Закрепление навыков стрессоустойчивости",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_stress_day10"
            )
        ),
        
        // === Items для "Фокус и концентрация" ===
        "course_focus_concentration" to listOf(
            CourseItemSeed(
                id = "course_focus_session1",
                courseId = "course_focus_concentration",
                order = 1,
                type = CourseItemType.PRACTICE,
                practiceId = "focus",
                title = "Сессия 1: Базовый фокус",
                description = "Тренировка концентрации",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_focus_session1"
            ),
            CourseItemSeed(
                id = "course_focus_session2",
                courseId = "course_focus_concentration",
                order = 2,
                type = CourseItemType.PRACTICE,
                practiceId = "focus",
                title = "Сессия 2: Углубление",
                description = "Более длительная концентрация",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_focus_session2",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_focus_session3",
                courseId = "course_focus_concentration",
                order = 3,
                type = CourseItemType.PRACTICE,
                practiceId = "focus",
                title = "Сессия 3: Фокус на задаче",
                description = "Отработка удержания внимания на одном объекте",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_focus_session3",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_focus_session4",
                courseId = "course_focus_concentration",
                order = 4,
                type = CourseItemType.PRACTICE,
                practiceId = "focus",
                title = "Сессия 4: Глубокая концентрация",
                description = "Увеличение продолжительности непрерывной работы ума",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_focus_session4",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_focus_session5",
                courseId = "course_focus_concentration",
                order = 5,
                type = CourseItemType.PRACTICE,
                practiceId = "mindfulness",
                title = "Сессия 5: Осознанная продуктивность",
                description = "Связь фокуса и отдыха",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_focus_session5",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_focus_session6",
                courseId = "course_focus_concentration",
                order = 6,
                type = CourseItemType.PRACTICE,
                practiceId = "focus",
                title = "Сессия 6: Итоговая тренировка",
                description = "Закрепление навыков глубокой концентрации",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_focus_session6",
                unlockCondition = UnlockCondition.CompletePreviousItem
            )
        ),
        
        // === Items для "Энергия на весь день" ===
        "course_energy_boost" to listOf(
            CourseItemSeed(
                id = "course_energy_day1",
                courseId = "course_energy_boost",
                order = 1,
                type = CourseItemType.PRACTICE,
                practiceId = "energizing",
                title = "День 1: Бодрящее дыхание",
                description = "Пробуждение энергии",
                mandatory = true,
                minDurationSec = 180,
                moduleId = "course_energy_day1"
            ),
            CourseItemSeed(
                id = "course_energy_day2",
                courseId = "course_energy_boost",
                order = 2,
                type = CourseItemType.PRACTICE,
                practiceId = "morning",
                title = "День 2: Утренний ритуал",
                description = "Комплексная практика",
                mandatory = true,
                minDurationSec = 600,
                moduleId = "course_energy_day2"
            ),
            CourseItemSeed(
                id = "course_energy_day3",
                courseId = "course_energy_boost",
                order = 3,
                type = CourseItemType.PRACTICE,
                practiceId = "energizing",
                title = "День 3: Быстрое восстановление",
                description = "Короткая сессия для перезагрузки днём",
                mandatory = true,
                minDurationSec = 300,
                moduleId = "course_energy_day3"
            ),
            CourseItemSeed(
                id = "course_energy_day4",
                courseId = "course_energy_boost",
                order = 4,
                type = CourseItemType.PRACTICE,
                practiceId = "morning",
                title = "День 4: Энергия без кофе",
                description = "Комбинированная практика для бодрости",
                mandatory = true,
                minDurationSec = 600,
                moduleId = "course_energy_day4"
            ),
            CourseItemSeed(
                id = "course_energy_day5",
                courseId = "course_energy_boost",
                order = 5,
                type = CourseItemType.PRACTICE,
                practiceId = "morning",
                title = "День 5: Закрепление ритуала",
                description = "Формирование устойчивой утренней привычки",
                mandatory = true,
                minDurationSec = 600,
                moduleId = "course_energy_day5"
            )
        ),
        
        // === Items для "Работа с тревожностью" ===
        "course_anxiety_management" to listOf(
            CourseItemSeed(
                id = "course_anxiety_module1",
                courseId = "course_anxiety_management",
                order = 1,
                type = CourseItemType.PRACTICE,
                practiceId = "anxiety",
                title = "Модуль 1: Основы",
                description = "Знакомство с тревогой",
                mandatory = true,
                minDurationSec = 720,
                moduleId = "course_anxiety_module1"
            ),
            CourseItemSeed(
                id = "course_anxiety_module2",
                courseId = "course_anxiety_management",
                order = 2,
                type = CourseItemType.PRACTICE,
                practiceId = "boxbreathing",
                title = "Модуль 2: Дыхание",
                description = "Дыхательные техники для тревоги",
                mandatory = true,
                minDurationSec = 420,
                moduleId = "course_anxiety_module2",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_anxiety_module3",
                courseId = "course_anxiety_management",
                order = 3,
                type = CourseItemType.PRACTICE,
                practiceId = "mindfulness",
                title = "Модуль 3: Работа с мыслями",
                description = "Осознанное наблюдение за тревожными мыслями",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_anxiety_module3",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_anxiety_module4",
                courseId = "course_anxiety_management",
                order = 4,
                type = CourseItemType.PRACTICE,
                practiceId = "bodyscan",
                title = "Модуль 4: Тело и тревога",
                description = "Расслабление мышечного напряжения",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_anxiety_module4",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_anxiety_module5",
                courseId = "course_anxiety_management",
                order = 5,
                type = CourseItemType.PRACTICE,
                practiceId = "forest",
                title = "Модуль 5: Безопасное пространство",
                description = "Создание чувства опоры через звуки природы",
                mandatory = false,
                minDurationSec = 1800,
                moduleId = "course_anxiety_module5",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_anxiety_module6",
                courseId = "course_anxiety_management",
                order = 6,
                type = CourseItemType.PRACTICE,
                practiceId = "anxiety",
                title = "Модуль 6: Комбинированная практика",
                description = "Сочетание дыхания, осознанности и звука",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_anxiety_module6",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_anxiety_module7",
                courseId = "course_anxiety_management",
                order = 7,
                type = CourseItemType.PRACTICE,
                practiceId = "mindfulness",
                title = "Модуль 7: Принятие и отпускание",
                description = "Практика принятия тревоги без сопротивления",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_anxiety_module7",
                unlockCondition = UnlockCondition.CompletePreviousItem
            ),
            CourseItemSeed(
                id = "course_anxiety_module8",
                courseId = "course_anxiety_management",
                order = 8,
                type = CourseItemType.PRACTICE,
                practiceId = "anxiety",
                title = "Модуль 8: Закрепление результата",
                description = "Финальная сессия для интеграции навыков",
                mandatory = true,
                minDurationSec = 900,
                moduleId = "course_anxiety_module8",
                unlockCondition = UnlockCondition.CompletePreviousItem
            )
        )
    )
}

