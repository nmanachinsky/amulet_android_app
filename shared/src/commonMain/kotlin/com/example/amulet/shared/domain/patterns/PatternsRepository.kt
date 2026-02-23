package com.example.amulet.shared.domain.patterns

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternDraft
import com.example.amulet.shared.domain.patterns.model.PatternFilter
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.model.PatternMarkers
import com.example.amulet.shared.domain.patterns.model.PatternUpdate
import com.example.amulet.shared.domain.patterns.model.PublishMetadata
import com.example.amulet.shared.domain.patterns.model.SyncResult
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с паттернами.
 * Предоставляет методы для чтения, создания, обновления и удаления паттернов.
 */
interface PatternsRepository {
    // Потоки данных (только из локальной БД)
    fun getPatternsStream(
        filter: PatternFilter,
        userId: UserId
    ): Flow<List<Pattern>>
    
    fun getPatternById(id: PatternId): Flow<Pattern?>
    
    fun getMyPatternsStream(userId: UserId): Flow<List<Pattern>>
    
    // Синхронизация (ручная, по запросу пользователя)
    suspend fun syncWithCloud(): AppResult<SyncResult>

    // Локальное сидирование данных
    suspend fun seedLocalData(): AppResult<Unit>
    
    // Команды
    suspend fun createPattern(
        draft: PatternDraft,
        userId: UserId
    ): AppResult<Pattern>
    
    suspend fun updatePattern(
        id: PatternId,
        version: Int,
        updates: PatternUpdate,
        userId: UserId
    ): AppResult<Pattern>
    
    suspend fun deletePattern(
        id: PatternId,
        userId: UserId
    ): AppResult<Unit>
    
    suspend fun publishPattern(
        id: PatternId,
        metadata: PublishMetadata,
        userId: UserId
    ): AppResult<Pattern>
    
    suspend fun sharePattern(
        id: PatternId,
        userIds: List<UserId>,
        userId: UserId
    ): AppResult<Unit>
    
    // Теги
    suspend fun addTag(
        patternId: PatternId,
        tag: String,
        userId: UserId
    ): AppResult<Unit>
    
    suspend fun removeTag(
        patternId: PatternId,
        tag: String,
        userId: UserId
    ): AppResult<Unit>

    // Справочник тегов
    suspend fun getAllTags(): AppResult<List<String>>

    suspend fun searchTags(query: String): AppResult<List<String>>

    // Создание тегов (без привязки к паттерну)
    suspend fun createTags(names: List<String>): AppResult<Unit>

    // Массовые операции
    suspend fun setPatternTags(
        patternId: PatternId,
        tags: List<String>,
        userId: UserId
    ): AppResult<Unit>

    suspend fun deleteTags(names: List<String>): AppResult<Unit>

    /**
     * Гарантировать наличие паттерна локально.
     * Если паттерна нет в БД, пробуем загрузить его с сервера по id и сохранить.
     */
    suspend fun ensurePatternLoaded(id: PatternId, userId: UserId): AppResult<Unit>

    /**
     * Получить сегменты паттерна по parentPatternId (локально).
     */
    suspend fun getSegmentsForPattern(parentId: PatternId, userId: UserId): AppResult<List<Pattern>>

    /**
     * Удалить все сегменты паттерна по parentPatternId (локально).
     */
    suspend fun deleteSegmentsForPattern(parentId: PatternId, userId: UserId): AppResult<Unit>

    /**
     * Пересохранить сегменты паттерна: удалить старые и записать новые (локально).
     */
    suspend fun upsertSegmentsForPattern(parentId: PatternId, segments: List<Pattern>, userId: UserId): AppResult<Unit>

    /**
     * Получить маркеры таймлайна для паттерна.
     */
    suspend fun getPatternMarkers(patternId: PatternId): AppResult<PatternMarkers?>

    /**
     * Сохранить или обновить маркеры таймлайна для паттерна.
     */
    suspend fun upsertPatternMarkers(markers: PatternMarkers, userId: UserId): AppResult<Unit>

    /**
     * Удалить маркеры таймлайна для паттерна.
     */
    suspend fun deletePatternMarkers(patternId: PatternId, userId: UserId): AppResult<Unit>
}
