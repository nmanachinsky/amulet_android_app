package com.example.amulet.shared.domain.practices

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.model.MoodEntry
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.practices.model.MoodSource
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с историей настроения пользователя.
 */
interface MoodRepository {

    /**
     * Логирует событие о настроении пользователя.
     */
    suspend fun logMood(
        userId: UserId,
        mood: MoodKind,
        source: MoodSource,
    ): AppResult<Unit>

    /**
     * Стрим истории настроения пользователя (на будущее для графиков/статистики).
     */
    fun getMoodHistoryStream(userId: UserId): Flow<List<MoodEntry>>
}
