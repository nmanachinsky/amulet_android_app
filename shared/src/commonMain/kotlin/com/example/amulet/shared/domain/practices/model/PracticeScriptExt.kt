package com.example.amulet.shared.domain.practices.model

/**
 * Суммарная длительность скрипта в секундах -- сумма длительностей всех шагов.
 *
 * Используется как единый источник истины для [Practice.durationSec]: показанная
 * длительность практики всегда равна реальной длине её скрипта.
 */
fun PracticeScript.totalDurationSec(): Int = steps.sumOf { it.durationSec ?: 0 }
