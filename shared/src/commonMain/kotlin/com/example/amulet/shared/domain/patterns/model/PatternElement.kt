package com.example.amulet.shared.domain.patterns.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class PatternTimeline(
    val durationMs: Int,
    val tracks: List<TimelineTrack>
)

@Serializable
data class TimelineTrack(
    val target: TimelineTarget,
    val priority: Int = 0,
    val mixMode: MixMode = MixMode.OVERRIDE,
    val clips: List<TimelineClip>
)

@Serializable
@JsonClassDiscriminator("type")
sealed class TimelineTarget

@Serializable
@SerialName("led")
data class TargetLed(val index: Int) : TimelineTarget()

@Serializable
@SerialName("group")
data class TargetGroup(val indices: List<Int>) : TimelineTarget()

@Serializable
@SerialName("ring")
data object TargetRing : TimelineTarget()

@Serializable
data class TimelineClip(
    val startMs: Int,
    val durationMs: Int,
    val color: String,
    val fadeInMs: Int = 0,
    val fadeOutMs: Int = 0,
    val easing: Easing = Easing.LINEAR
)

@Serializable
enum class MixMode { OVERRIDE, ADDITIVE }

@Serializable
enum class Easing { LINEAR }
