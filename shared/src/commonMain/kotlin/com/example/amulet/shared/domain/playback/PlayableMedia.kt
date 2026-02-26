package com.example.amulet.shared.domain.playback

import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.model.PatternSpec
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticeStep

sealed interface PlayableMedia {
    data class Preview(
        val spec: PatternSpec,
        val intensity: Double
    ) : PlayableMedia

    data class SinglePattern(
        val patternId: PatternId,
        val intensity: Double
    ) : PlayableMedia

    data class PracticeScript(
        val practiceId: PracticeId,
        val steps: List<PracticeStep>,
        val intensity: Double
    ) : PlayableMedia
}
