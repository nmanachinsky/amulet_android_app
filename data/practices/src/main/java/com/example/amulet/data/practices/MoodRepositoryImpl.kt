package com.example.amulet.data.practices

import com.example.amulet.data.practices.datasource.LocalPracticesDataSource
import com.example.amulet.data.practices.mapper.toDomain
import com.example.amulet.data.practices.mapper.toEntity
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.practices.MoodRepository
import com.example.amulet.shared.domain.practices.model.MoodEntry
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.practices.model.MoodSource
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoodRepositoryImpl @Inject constructor(
    private val local: LocalPracticesDataSource,
    private val sessionProvider: UserSessionProvider,
) : MoodRepository {

    private val currentUserId: String
        get() = when (val c = sessionProvider.currentContext) {
            is UserSessionContext.LoggedIn -> c.userId.value
            else -> throw IllegalStateException("User not authenticated")
        }

    override suspend fun logMood(
        mood: MoodKind,
        source: MoodSource,
    ): AppResult<Unit> {
        return try {
            val entry = MoodEntry(
                id = java.util.UUID.randomUUID().toString(),
                userId = UserId(currentUserId),
                mood = mood,
                source = source,
                createdAt = System.currentTimeMillis(),
            )
            local.upsertMoodEntry(entry.toEntity())
            Ok(Unit)
        } catch (e: Exception) {
            Err(AppError.Unknown)
        }
    }

    override fun getMoodHistoryStream(): Flow<List<MoodEntry>> =
        local.observeMoodEntries(currentUserId).map { list ->
            list.mapNotNull { it.toDomain() }
        }
}
