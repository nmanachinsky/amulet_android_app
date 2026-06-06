package com.example.amulet.data.patterns.seed

import com.example.amulet.data.patterns.seed.breathing.Breathing478Patterns
import com.example.amulet.data.patterns.seed.breathing.BoxBreathingPatterns
import com.example.amulet.data.patterns.seed.breathing.EnergizingBreathingPatterns
import com.example.amulet.data.patterns.seed.meditation.BodyScanPatterns
import com.example.amulet.data.patterns.seed.meditation.FocusMeditationPatterns
import com.example.amulet.data.patterns.seed.meditation.MindfulnessPatterns
import com.example.amulet.data.patterns.seed.meditation.SleepMeditationPatterns
import com.example.amulet.data.patterns.seed.other.MixedPracticePatterns
import com.example.amulet.data.patterns.seed.other.SoundscapePatterns
import com.example.amulet.shared.domain.patterns.model.Pattern

/**
 * Агрегатор системных световых паттернов практик.
 *
 * Каждая практика имеет собственный уникальный набор паттернов; ни один паттерн не делится
 * между практиками. Дыхательные и медитационные практики дополнительно разбиты на пофазные
 * паттерны (см. соответствующие объекты в подпакетах breathing/meditation/other).
 */
object PracticePatternSeeds {

    fun getPatterns(): List<Pattern> = buildList {
        // Дыхание -- полные пофазные паттерны.
        addAll(Breathing478Patterns.all())
        addAll(BoxBreathingPatterns.all())
        addAll(EnergizingBreathingPatterns.all())

        // Медитации -- многофазные паттерны.
        addAll(MindfulnessPatterns.all())
        addAll(BodyScanPatterns.all())
        addAll(SleepMeditationPatterns.all())
        addAll(FocusMeditationPatterns.all())

        // Звуки, смешанные и продвинутые -- по одному уникальному паттерну на практику.
        addAll(SoundscapePatterns.all())
        addAll(MixedPracticePatterns.all())
    }
}
