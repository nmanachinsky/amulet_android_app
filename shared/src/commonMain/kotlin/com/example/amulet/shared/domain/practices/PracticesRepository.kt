package com.example.amulet.shared.domain.practices

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.model.Practice
import com.example.amulet.shared.domain.practices.model.PracticeBadge
import com.example.amulet.shared.domain.practices.model.PracticeCategory
import com.example.amulet.shared.domain.practices.model.PracticeCollection
import com.example.amulet.shared.domain.practices.model.PracticeFilter
import com.example.amulet.shared.domain.practices.model.PracticeGoal
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.ScheduledSession
import com.example.amulet.shared.domain.practices.model.PracticePlan
import com.example.amulet.shared.domain.practices.model.PracticeSchedule
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.practices.model.PracticeSessionSource
import com.example.amulet.shared.domain.practices.model.PracticeSessionId
import com.example.amulet.shared.domain.practices.model.PracticeStatistics
import com.example.amulet.shared.domain.practices.model.PracticeTag
import com.example.amulet.shared.domain.user.model.UserPreferences
import com.example.amulet.shared.domain.practices.model.PracticeAudioMode
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow

interface PracticesRepository {
    fun getPracticesStream(
        userId: UserId,
        filter: PracticeFilter
    ): Flow<List<Practice>>

    fun getPracticeById(userId: UserId, id: PracticeId): Flow<Practice?>

    fun getCategoriesStream(): Flow<List<PracticeCategory>>

    fun getFavoritesStream(userId: UserId): Flow<List<Practice>>

    fun getRecommendationsStream(userId: UserId, limit: Int? = null, contextGoal: PracticeGoal? = null): Flow<List<Practice>>

    suspend fun search(
        userId: UserId,
        query: String,
        filter: PracticeFilter
    ): AppResult<List<Practice>>

    suspend fun upsertPractice(practice: Practice): AppResult<Unit>

    suspend fun refreshCatalog(): AppResult<Unit>

    suspend fun seedLocalData(): AppResult<Unit>

    suspend fun setFavorite(
        userId: UserId,
        practiceId: PracticeId,
        favorite: Boolean
    ): AppResult<Unit>

    fun getActiveSessionStream(userId: UserId): Flow<PracticeSession?>

    fun getSessionsHistoryStream(userId: UserId, limit: Int? = null): Flow<List<PracticeSession>>

    suspend fun startPractice(
        userId: UserId,
        practiceId: PracticeId,
        intensity: Double? = null,
        brightness: Double? = null,
        vibrationLevel: Double? = null,
        audioMode: PracticeAudioMode? = null,
        source: PracticeSessionSource? = PracticeSessionSource.Manual,
    ): AppResult<PracticeSession>

    suspend fun stopSession(
        sessionId: PracticeSessionId,
        completed: Boolean
    ): AppResult<PracticeSession>

    suspend fun updateSessionMoodBefore(
        sessionId: PracticeSessionId,
        moodBefore: MoodKind?,
    ): AppResult<PracticeSession>

    suspend fun updateSessionFeedback(
        sessionId: PracticeSessionId,
        rating: Int?,
        moodAfter: MoodKind?,
        feedbackNote: String?,
    ): AppResult<PracticeSession>

    fun getUserPreferencesStream(userId: UserId): Flow<UserPreferences>
    
    fun getSchedulesStream(userId: UserId): Flow<List<PracticeSchedule>>
    
    fun getScheduleByPracticeId(userId: UserId, practiceId: PracticeId): Flow<PracticeSchedule?>

    suspend fun upsertSchedule(
        userId: UserId,
        schedule: PracticeSchedule
    ): AppResult<Unit>

    suspend fun deleteSchedule(
        scheduleId: String
    ): AppResult<Unit>

    suspend fun updateUserPreferences(
        userId: UserId,
        preferences: UserPreferences
    ): AppResult<Unit>

    suspend fun skipScheduledSession(
        userId: UserId,
        session: ScheduledSession
    ): AppResult<Unit>

    // Plans
    fun getPlansStream(userId: UserId): Flow<List<PracticePlan>>
    fun getPlanById(userId: UserId, id: String): Flow<PracticePlan?>
    fun getSchedulesByPlanStream(planId: String): Flow<List<PracticeSchedule>>
    suspend fun upsertPlan(userId: UserId, plan: PracticePlan): AppResult<Unit>
    suspend fun deletePlan(planId: String): AppResult<Unit>

    // Statistics & badges
    fun getStatisticsStream(userId: UserId): Flow<PracticeStatistics?>
    fun getBadgesStream(userId: UserId): Flow<List<PracticeBadge>>

    // Tags & collections (для дома практик / каталога)
    fun getPracticeTagsStream(): Flow<List<PracticeTag>>
    fun getCollectionsStream(): Flow<List<PracticeCollection>>
}
