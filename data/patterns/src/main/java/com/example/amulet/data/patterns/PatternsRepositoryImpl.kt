package com.example.amulet.data.patterns

import com.example.amulet.core.database.entity.OutboxActionEntity
import com.example.amulet.core.database.entity.OutboxActionStatus
import com.example.amulet.core.database.entity.OutboxActionType
import com.example.amulet.core.sync.scheduler.OutboxScheduler
import com.example.amulet.data.patterns.datasource.LocalPatternDataSource
import com.example.amulet.data.patterns.datasource.RemotePatternDataSource
import com.example.amulet.data.patterns.mapper.toDomain
import com.example.amulet.data.patterns.mapper.toDtoForSegments
import com.example.amulet.data.patterns.mapper.toEntity
import com.example.amulet.data.patterns.mapper.toTagEntities
import com.example.amulet.data.patterns.mapper.toTagNames
import com.example.amulet.data.patterns.mapper.toUserIds
import com.example.amulet.data.patterns.seed.PresetPatternSeeds
import com.example.amulet.data.patterns.seed.PracticePatternSeeds
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.auth.UserSessionContext
import com.example.amulet.shared.core.auth.UserSessionProvider
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.Pattern
import com.example.amulet.shared.domain.patterns.model.PatternDraft
import com.example.amulet.shared.domain.patterns.model.PatternFilter
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.model.PatternMarkers
import com.example.amulet.shared.domain.patterns.model.PatternSpec
import com.example.amulet.shared.domain.patterns.model.PatternUpdate
import com.example.amulet.shared.domain.patterns.model.PublishMetadata
import com.example.amulet.shared.domain.patterns.model.SyncResult
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория паттернов.
 * Использует datasource паттерн для разделения источников данных.
 * Команды отправляются через Outbox для гарантированной доставки.
 */
@Singleton
class PatternsRepositoryImpl @Inject constructor(
    private val localDataSource: LocalPatternDataSource,
    private val remoteDataSource: RemotePatternDataSource,
    private val sessionProvider: UserSessionProvider,
    private val outboxScheduler: OutboxScheduler,
    private val json: Json
) : PatternsRepository {
    
    private val currentUserId: String
        get() {
            val context = sessionProvider.currentContext
            return when (context) {
                is UserSessionContext.LoggedIn -> context.userId.value
                else -> throw IllegalStateException("User not authenticated")
            }
        }
    
    override fun getPatternsStream(filter: PatternFilter): Flow<List<Pattern>> {
        val baseFlow = when {
            filter.presetsOnly -> localDataSource.observePresets()
            filter.publicOnly -> localDataSource.observePublic()
            else -> localDataSource.observeByOwner(currentUserId)
        }
        
        return baseFlow.map { entities ->
            entities.map { entity ->
                val tags = localDataSource.getTagsForPattern(entity.id).toTagNames()
                val sharedWith = localDataSource.getSharesForPattern(entity.id).toUserIds()
                entity.toDomain(tags, sharedWith)
            }.filter { pattern ->
                // Применяем фильтры
                (filter.kind == null || pattern.kind == filter.kind) &&
                (filter.hardwareVersion == null || pattern.hardwareVersion == filter.hardwareVersion) &&
                (filter.tags.isEmpty() || filter.tags.any { it in pattern.tags }) &&
                (filter.query == null || pattern.title.contains(filter.query ?: "", ignoreCase = true))
            }
        }
    }
    
    override fun getPatternById(id: PatternId): Flow<Pattern?> {
        return localDataSource.observeById(id.value).map { entity ->
            entity?.let {
                val tags = localDataSource.getTagsForPattern(it.id).toTagNames()
                val sharedWith = localDataSource.getSharesForPattern(it.id).toUserIds()
                it.toDomain(tags, sharedWith)
            }
        }
    }
    
    override fun getMyPatternsStream(): Flow<List<Pattern>> {
        return localDataSource.observeByOwner(currentUserId).map { entities ->
            entities.map { entity ->
                val tags = localDataSource.getTagsForPattern(entity.id).toTagNames()
                val sharedWith = localDataSource.getSharesForPattern(entity.id).toUserIds()
                entity.toDomain(tags, sharedWith)
            }
        }
    }
    
    override suspend fun syncWithCloud(): Result<SyncResult, AppError> {
        Logger.d("Начало синхронизации паттернов с облаком", "PatternsRepositoryImpl")
        return try {
            // Получаем все паттерны с сервера
            val remotePatterns = remoteDataSource.getOwnPatterns().getOrElse { error ->
                Logger.e("Ошибка получения паттернов с сервера: $error", throwable = Exception(error.toString()), tag = "PatternsRepositoryImpl")
                val result = SyncResult(
                    patternsAdded = 0,
                    patternsUpdated = 0,
                    patternsDeleted = 0
                )
                Logger.d("Синхронизация завершена с ошибкой: $result", "PatternsRepositoryImpl")
                return Ok(result)
            }

            Logger.d("Получено паттернов с сервера: ${remotePatterns.size}", "PatternsRepositoryImpl")
            
            // Сохраняем только пользовательские паттерны с сервера
            remotePatterns.forEach { dto ->
                val tags = dto.tags?.toTagEntities() ?: emptyList()
                val tagIds = tags.map { it.id }
                val sharedWith = dto.sharedWith ?: emptyList()
                
                val entity = dto.toEntity()
                // Сохраняем только если есть ownerId (пользовательский паттерн)
                if (entity.ownerId != null) {
                    localDataSource.upsertPatternWithRelations(
                        pattern = entity,
                        tags = tags,
                        tagIds = tagIds,
                        sharedUserIds = sharedWith
                    )
                }
            }
            
            val result = SyncResult(
                patternsAdded = remotePatterns.size,
                patternsUpdated = 0,
                patternsDeleted = 0
            )
            Logger.d("Синхронизация завершена: $result", "PatternsRepositoryImpl")
            Ok(result)
        } catch (e: Exception) {
            Logger.e("Ошибка синхронизации паттернов: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.Unknown)
        }
    }
    
    override suspend fun seedLocalData(): AppResult<Unit> {
        Logger.d("Начало локального сидирования паттернов-пресетов", "PatternsRepositoryImpl")
        return try {
            val presetPatterns = PresetPatternSeeds.getPresetPatterns()
            val practicePatterns = PracticePatternSeeds.getPatterns()

            val allPatterns: List<Pattern> = presetPatterns + practicePatterns
            localDataSource.seedPresets(allPatterns)
            Logger.d("Локальное сидирование паттернов-пресетов завершено: ${allPatterns.size} паттернов", "PatternsRepositoryImpl")
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка локального сидирования паттернов-пресетов: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.Unknown)
        }
    }
    
    override suspend fun createPattern(draft: PatternDraft): AppResult<Pattern> {
        Logger.d("Создание паттерна: ${draft.title}, тип: ${draft.kind}", "PatternsRepositoryImpl")
        return try {
            val patternId = UUID.randomUUID().toString()
            val nowMillis = System.currentTimeMillis()
            
            // Создаем локальную сущность
            val pattern = Pattern(
                id = PatternId(patternId),
                version = 1,
                ownerId = UserId(currentUserId),
                kind = draft.kind,
                spec = draft.spec,
                public = false,
                reviewStatus = null,
                hardwareVersion = draft.hardwareVersion,
                title = draft.title,
                description = draft.description,
                tags = emptyList(),
                usageCount = null,
                sharedWith = emptyList(),
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
            
            // Транзакционно: сохраняем в БД и добавляем в Outbox
            localDataSource.withPatternTransaction {
                Logger.d("Сохранение паттерна в БД: $patternId", "PatternsRepositoryImpl")
                localDataSource.upsertPattern(pattern.toEntity())
                
                val payloadObject = buildJsonObject {
                    put("kind", draft.kind.name.lowercase())
                    put("spec", json.encodeToJsonElement(PatternSpec.serializer(), draft.spec))
                    put("title", draft.title)
                    draft.description?.let { put("description", it) } ?: put("description", JsonNull)
                    put("hardwareVersion", draft.hardwareVersion)
                }
                val payload = json.encodeToString(payloadObject)
                
                localDataSource.enqueueOutboxAction(
                    OutboxActionEntity(
                        id = UUID.randomUUID().toString(),
                        type = OutboxActionType.PATTERN_CREATE,
                        payloadJson = payload,
                        status = OutboxActionStatus.PENDING,
                        retryCount = 0,
                        lastError = null,
                        idempotencyKey = "pattern_create_$patternId",
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        availableAt = nowMillis,
                        priority = 1,
                        targetEntityId = patternId
                    )
                )
            }
            
            // Планируем синхронизацию
            outboxScheduler.scheduleSync()
            
            Logger.d("Паттерн создан успешно: $patternId", "PatternsRepositoryImpl")
            Ok(pattern)
        } catch (e: Exception) {
            Logger.e("Ошибка создания паттерна: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun updatePattern(
        id: PatternId,
        version: Int,
        updates: PatternUpdate
    ): AppResult<Pattern> {
        Logger.d("Обновление паттерна: ${id.value}, версия: $version", "PatternsRepositoryImpl")
        return try {
            val entity = localDataSource.observeById(id.value).first()
                ?: return Err(AppError.NotFound)
            
            val tags = localDataSource.getTagsForPattern(id.value).toTagNames()
            val sharedWith = localDataSource.getSharesForPattern(id.value).toUserIds()
            val currentPattern = entity.toDomain(tags, sharedWith)
            
            // Применяем обновления
            val updatedPattern = currentPattern.copy(
                title = updates.title ?: currentPattern.title,
                description = updates.description ?: currentPattern.description,
                spec = updates.spec ?: currentPattern.spec,
                tags = updates.tags ?: currentPattern.tags,
                version = version + 1,
                updatedAt = System.currentTimeMillis()
            )
            
            val nowMillis = System.currentTimeMillis()
            
            // Транзакционно: обновляем в БД и добавляем в Outbox
            localDataSource.withPatternTransaction {
                Logger.d("Обновление паттерна в БД: ${id.value}", "PatternsRepositoryImpl")
                localDataSource.upsertPattern(updatedPattern.toEntity())
                
                val payloadObject = buildJsonObject {
                    put("version", version)
                    updates.title?.let { put("title", it) } ?: put("title", JsonNull)
                    updates.description?.let { put("description", it) } ?: put("description", JsonNull)
                    val specJson = updates.spec?.let { json.encodeToString(PatternSpec.serializer(), it) }
                    specJson?.let { put("spec", it) } ?: put("spec", JsonNull)
                    updates.tags?.let { tagsList ->
                        put("tags", buildJsonArray { tagsList.forEach { add(JsonPrimitive(it)) } })
                    } ?: put("tags", JsonNull)
                }
                val payload = json.encodeToString(payloadObject)
                
                localDataSource.enqueueOutboxAction(
                    OutboxActionEntity(
                        id = UUID.randomUUID().toString(),
                        type = OutboxActionType.PATTERN_UPDATE,
                        payloadJson = payload,
                        status = OutboxActionStatus.PENDING,
                        retryCount = 0,
                        lastError = null,
                        idempotencyKey = "pattern_update_${id.value}_$version",
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        availableAt = nowMillis,
                        priority = 1,
                        targetEntityId = id.value
                    )
                )
            }
            
            outboxScheduler.scheduleSync()
            
            Logger.d("Паттерн обновлен успешно: ${id.value}", "PatternsRepositoryImpl")
            Ok(updatedPattern)
        } catch (e: Exception) {
            Logger.e("Ошибка обновления паттерна: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun deletePattern(id: PatternId): AppResult<Unit> {
        Logger.d("Удаление паттерна: ${id.value}", "PatternsRepositoryImpl")
        return try {
            val nowMillis = System.currentTimeMillis()
            
            // Транзакционно: удаляем из БД и добавляем в Outbox
            localDataSource.withPatternTransaction {
                Logger.d("Удаление паттерна из БД: ${id.value}", "PatternsRepositoryImpl")
                localDataSource.deletePattern(id.value)
                
                localDataSource.enqueueOutboxAction(
                    OutboxActionEntity(
                        id = UUID.randomUUID().toString(),
                        type = OutboxActionType.PATTERN_DELETE,
                        payloadJson = json.encodeToString(mapOf("patternId" to id.value)),
                        status = OutboxActionStatus.PENDING,
                        retryCount = 0,
                        lastError = null,
                        idempotencyKey = "pattern_delete_${id.value}",
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        availableAt = nowMillis,
                        priority = 1,
                        targetEntityId = id.value
                    )
                )
            }
            
            outboxScheduler.scheduleSync()
            
            Logger.d("Паттерн удален успешно: ${id.value}", "PatternsRepositoryImpl")
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка удаления паттерна: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun publishPattern(
        id: PatternId,
        metadata: PublishMetadata
    ): AppResult<Pattern> {
        Logger.d("Публикация паттерна: ${id.value}, заголовок: ${metadata.title}", "PatternsRepositoryImpl")
        return try {
            val entity = localDataSource.observeById(id.value).first()
                ?: return Err(AppError.NotFound)
            
            val tags = localDataSource.getTagsForPattern(id.value).toTagNames()
            val sharedWith = localDataSource.getSharesForPattern(id.value).toUserIds()
            val currentPattern = entity.toDomain(tags, sharedWith)
            
            val updatedPattern = currentPattern.copy(
                public = true,
                title = metadata.title,
                description = metadata.description,
                tags = metadata.tags,
                updatedAt = System.currentTimeMillis()
            )
            
            val nowMillis = System.currentTimeMillis()
            
            localDataSource.withPatternTransaction {
                Logger.d("Публикация паттерна в БД: ${id.value}", "PatternsRepositoryImpl")
                localDataSource.upsertPattern(updatedPattern.toEntity())
                
                val payloadObject = buildJsonObject {
                    put("version", currentPattern.version)
                    put("public", true)
                    put("title", metadata.title)
                    metadata.description?.let { put("description", it) } ?: put("description", JsonNull)
                    put("tags", buildJsonArray { metadata.tags.forEach { add(JsonPrimitive(it)) } })
                }
                val payload = json.encodeToString(payloadObject)
                
                localDataSource.enqueueOutboxAction(
                    OutboxActionEntity(
                        id = UUID.randomUUID().toString(),
                        type = OutboxActionType.PATTERN_UPDATE,
                        payloadJson = payload,
                        status = OutboxActionStatus.PENDING,
                        retryCount = 0,
                        lastError = null,
                        idempotencyKey = "pattern_publish_${id.value}",
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        availableAt = nowMillis,
                        priority = 1,
                        targetEntityId = id.value
                    )
                )
            }
            
            outboxScheduler.scheduleSync()
            
            Logger.d("Паттерн опубликован успешно: ${id.value}", "PatternsRepositoryImpl")
            Ok(updatedPattern)
        } catch (e: Exception) {
            Logger.e("Ошибка публикации паттерна: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun sharePattern(id: PatternId, userIds: List<UserId>): AppResult<Unit> {
        Logger.d("Шаринг паттерна: ${id.value}, пользователям: ${userIds.size}", "PatternsRepositoryImpl")
        return try {
            val nowMillis = System.currentTimeMillis()
            
            userIds.forEach { userId ->
                localDataSource.withPatternTransaction {
                    Logger.d("Добавление шаринга паттерна: ${id.value} для пользователя: ${userId.value}", "PatternsRepositoryImpl")
                    val payload = json.encodeToString(
                        mapOf(
                            "patternId" to id.value,
                            "toUserId" to userId.value
                        )
                    )
                    
                    localDataSource.enqueueOutboxAction(
                        OutboxActionEntity(
                            id = UUID.randomUUID().toString(),
                            type = OutboxActionType.PATTERN_SHARE,
                            payloadJson = payload,
                            status = OutboxActionStatus.PENDING,
                            retryCount = 0,
                            lastError = null,
                            idempotencyKey = "pattern_share_${id.value}_${userId.value}",
                            createdAt = nowMillis,
                            updatedAt = nowMillis,
                            availableAt = nowMillis,
                            priority = 1,
                            targetEntityId = id.value
                        )
                    )
                }
            }
            
            outboxScheduler.scheduleSync()
            
            Logger.d("Шаринг паттерна завершен: ${id.value}", "PatternsRepositoryImpl")
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка шаринга паттерна: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun addTag(patternId: PatternId, tag: String): AppResult<Unit> {
        Logger.d("Добавление тега к паттерну: ${patternId.value}, тег: $tag", "PatternsRepositoryImpl")
        return try {
            val entity = localDataSource.observeById(patternId.value).first()
                ?: return Err(AppError.NotFound)
            
            val currentTags = localDataSource.getTagsForPattern(patternId.value).toTagNames()
            if (tag in currentTags) {
                Logger.d("Тег уже существует: $tag", "PatternsRepositoryImpl")
                return Ok(Unit) // Тег уже есть
            }
            
            val updatedTags = currentTags + tag
            val tagEntities = updatedTags.toTagEntities()
            
            Logger.d("Обновление тегов паттерна: ${patternId.value}", "PatternsRepositoryImpl")

            val nowMillis = System.currentTimeMillis()
            localDataSource.withPatternTransaction {
                // Обновляем связи тегов локально.
                localDataSource.upsertPatternWithRelations(
                    pattern = entity,
                    tags = tagEntities,
                    tagIds = tagEntities.map { it.id },
                    sharedUserIds = localDataSource.getSharesForPattern(patternId.value).toUserIds()
                )

                // Ставим Outbox-задачу PATTERN_UPDATE для синхронизации тегов на сервере.
                val payloadObject = buildJsonObject {
                    put("version", entity.version)
                    put("title", JsonNull)
                    put("description", JsonNull)
                    put("spec", JsonNull)
                    put("tags", buildJsonArray { updatedTags.forEach { add(JsonPrimitive(it)) } })
                }
                val payloadJson = json.encodeToString(payloadObject)

                localDataSource.enqueueOutboxAction(
                    OutboxActionEntity(
                        id = UUID.randomUUID().toString(),
                        type = OutboxActionType.PATTERN_UPDATE,
                        payloadJson = payloadJson,
                        status = OutboxActionStatus.PENDING,
                        retryCount = 0,
                        lastError = null,
                        idempotencyKey = "pattern_tags_${patternId.value}_$nowMillis",
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        availableAt = nowMillis,
                        priority = 1,
                        targetEntityId = patternId.value
                    )
                )
            }

            outboxScheduler.scheduleSync()

            Logger.d("Тег добавлен успешно: $tag", "PatternsRepositoryImpl")
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка добавления тега: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun removeTag(patternId: PatternId, tag: String): AppResult<Unit> {
        Logger.d("Удаление тега из паттерна: ${patternId.value}, тег: $tag", "PatternsRepositoryImpl")
        return try {
            val entity = localDataSource.observeById(patternId.value).first()
                ?: return Err(AppError.NotFound)
            
            val currentTags = localDataSource.getTagsForPattern(patternId.value).toTagNames()
            val updatedTags = currentTags.filter { it != tag }
            val tagEntities = updatedTags.toTagEntities()
            
            Logger.d("Обновление тегов паттерна: ${patternId.value}", "PatternsRepositoryImpl")

            val nowMillis = System.currentTimeMillis()
            localDataSource.withPatternTransaction {
                // Обновляем связи тегов локально.
                localDataSource.upsertPatternWithRelations(
                    pattern = entity,
                    tags = tagEntities,
                    tagIds = tagEntities.map { it.id },
                    sharedUserIds = localDataSource.getSharesForPattern(patternId.value).toUserIds()
                )

                // Ставим Outbox-задачу PATTERN_UPDATE для синхронизации тегов на сервере.
                val payloadObject = buildJsonObject {
                    put("version", entity.version)
                    put("title", JsonNull)
                    put("description", JsonNull)
                    put("spec", JsonNull)
                    put("tags", buildJsonArray { updatedTags.forEach { add(JsonPrimitive(it)) } })
                }
                val payloadJson = json.encodeToString(payloadObject)

                localDataSource.enqueueOutboxAction(
                    OutboxActionEntity(
                        id = UUID.randomUUID().toString(),
                        type = OutboxActionType.PATTERN_UPDATE,
                        payloadJson = payloadJson,
                        status = OutboxActionStatus.PENDING,
                        retryCount = 0,
                        lastError = null,
                        idempotencyKey = "pattern_tags_${patternId.value}_$nowMillis",
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        availableAt = nowMillis,
                        priority = 1,
                        targetEntityId = patternId.value
                    )
                )
            }

            outboxScheduler.scheduleSync()

            Logger.d("Тег удален успешно: $tag", "PatternsRepositoryImpl")
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка удаления тега: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun getAllTags(): AppResult<List<String>> {
        return try {
            val tags = localDataSource.getAllTags().toTagNames()
            Ok(tags)
        } catch (e: Exception) {
            Logger.e("Ошибка получения всех тегов: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun searchTags(query: String): AppResult<List<String>> {
        return try {
            val tags = localDataSource.searchTags(query).toTagNames()
            Ok(tags)
        } catch (e: Exception) {
            Logger.e("Ошибка поиска тегов: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun createTags(names: List<String>): AppResult<Unit> {
        Logger.d("Создание тегов в БД: ${names.size}", "PatternsRepositoryImpl")
        return try {
            if (names.isEmpty()) return Ok(Unit)
            val entities = names.toTagEntities()
            localDataSource.insertTags(entities)
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка создания тегов: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun setPatternTags(patternId: PatternId, tags: List<String>): AppResult<Unit> {
        Logger.d("Перезапись тегов для паттерна: ${patternId.value}, всего: ${tags.size}", "PatternsRepositoryImpl")
        return try {
            val entity = localDataSource.observeById(patternId.value).first()
                ?: return Err(AppError.NotFound)

            // Существующие и новые теги
            val existing = localDataSource.getTagsByNames(tags)
            val existingNames = existing.map { it.name }.toSet()
            val newNames = tags.filter { it !in existingNames }
            val newEntities = newNames.toTagEntities()
            val combined = existing + newEntities
            val tagIds = combined.map { it.id }

            Logger.d("Обновление связей тегов для паттерна: ${patternId.value}", "PatternsRepositoryImpl")

            val nowMillis = System.currentTimeMillis()
            localDataSource.withPatternTransaction {
                // Обновляем связи тегов локально.
                localDataSource.upsertPatternWithRelations(
                    pattern = entity,
                    tags = combined, // insertTags(IGNORE) создаст новые, существующие проигнорируются
                    tagIds = tagIds, // использовать реальные IDs (для новых сгенерированы нами)
                    sharedUserIds = localDataSource.getSharesForPattern(patternId.value).toUserIds()
                )

                // Ставим Outbox-задачу PATTERN_UPDATE для синхронизации тегов на сервере.
                val payloadObject = buildJsonObject {
                    put("version", entity.version)
                    put("title", JsonNull)
                    put("description", JsonNull)
                    put("spec", JsonNull)
                    put("tags", buildJsonArray { tags.forEach { add(JsonPrimitive(it)) } })
                }
                val payloadJson = json.encodeToString(payloadObject)

                localDataSource.enqueueOutboxAction(
                    OutboxActionEntity(
                        id = UUID.randomUUID().toString(),
                        type = OutboxActionType.PATTERN_UPDATE,
                        payloadJson = payloadJson,
                        status = OutboxActionStatus.PENDING,
                        retryCount = 0,
                        lastError = null,
                        idempotencyKey = "pattern_tags_${patternId.value}_$nowMillis",
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        availableAt = nowMillis,
                        priority = 1,
                        targetEntityId = patternId.value
                    )
                )
            }

            outboxScheduler.scheduleSync()

            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка перезаписи тегов: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun deleteTags(names: List<String>): AppResult<Unit> {
        Logger.d("Удаление тегов из БД: ${names.size}", "PatternsRepositoryImpl")
        return try {
            if (names.isEmpty()) return Ok(Unit)
            localDataSource.deleteTagsByNames(names)
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка удаления тегов: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun ensurePatternLoaded(id: PatternId): AppResult<Unit> {
        Logger.d("ensurePatternLoaded: start patternId=${id.value}", "PatternsRepositoryImpl")
        return try {
            // Если паттерн уже есть локально, ничего не делаем.
            val existing = localDataSource.observeById(id.value).first()
            if (existing != null) {
                Logger.d("ensurePatternLoaded: already in local DB patternId=${id.value}", "PatternsRepositoryImpl")
                return Ok(Unit)
            }

            // Пытаемся получить паттерн с сервера.
            Logger.d("ensurePatternLoaded: fetch from remote patternId=${id.value}", "PatternsRepositoryImpl")
            val remoteResult = remoteDataSource.getPattern(id.value)
            val dto = remoteResult.component1()
            if (dto != null) {
                val tags = dto.tags?.toTagEntities() ?: emptyList()
                val tagIds = tags.map { it.id }
                val sharedWith = dto.sharedWith ?: emptyList()

                val entity = dto.toEntity().copy(ownerId = currentUserId)
                Logger.d("ensurePatternLoaded: upsert local patternId=${id.value}", "PatternsRepositoryImpl")
                localDataSource.upsertPatternWithRelations(
                    pattern = entity,
                    tags = tags,
                    tagIds = tagIds,
                    sharedUserIds = sharedWith
                )

                val inserted = localDataSource.observeById(id.value).first()
                if (inserted == null) {
                    Logger.e(
                        "ensurePatternLoaded: local upsert finished but pattern still missing in DB patternId=${id.value}",
                        throwable = IllegalStateException("Pattern not found after upsert: ${id.value}"),
                        tag = "PatternsRepositoryImpl"
                    )
                    return Err(AppError.DatabaseError)
                }

                // Дополнительно подтягиваем сегменты и маркеры таймлайна с сервера.
                runCatching {
                    val segmentsResult = remoteDataSource.getPatternSegments(id.value)
                    val segmentDtos = segmentsResult.component1().orEmpty()
                    if (segmentDtos.isNotEmpty()) {
                        // Перезаписываем локальные сегменты без постановки задач в Outbox.
                        localDataSource.deleteSegmentsForPattern(id.value)
                        segmentDtos.forEach { segmentDto ->
                            val segmentEntity = segmentDto.toEntity().copy(ownerId = currentUserId)
                            val segmentTags = segmentDto.tags?.toTagEntities() ?: emptyList()
                            val segmentTagIds = segmentTags.map { it.id }
                            localDataSource.upsertPatternWithRelations(
                                pattern = segmentEntity,
                                tags = segmentTags,
                                tagIds = segmentTagIds,
                                sharedUserIds = emptyList()
                            )
                        }
                    }
                }

                runCatching {
                    val markersResult = remoteDataSource.getPatternMarkers(id.value)
                    val markersDto = markersResult.component1()
                    if (markersDto != null) {
                        val markersJson: String = json.encodeToString(markersDto.markersMs)
                        val entity = com.example.amulet.core.database.entity.PatternMarkersEntity(
                            patternId = markersDto.patternId,
                            markersJson = markersJson
                        )
                        localDataSource.upsertPatternMarkers(entity)
                    }
                }

                Logger.d("ensurePatternLoaded: done patternId=${id.value}", "PatternsRepositoryImpl")
                Ok(Unit)
            } else {
                val error = remoteResult.component2() ?: AppError.Unknown
                Logger.e(
                    "ensurePatternLoaded: remote returned null dto patternId=${id.value} error=$error",
                    throwable = Exception(error.toString()),
                    tag = "PatternsRepositoryImpl"
                )
                Err(error)
            }
        } catch (e: Exception) {
            Logger.e("Ошибка ensurePatternLoaded: $e", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.Unknown)
        }
    }

    override suspend fun getSegmentsForPattern(parentId: PatternId): AppResult<List<Pattern>> {
        return try {
            val entities = localDataSource.getSegmentsForPattern(parentId.value)
            val patterns = entities.map { entity ->
                val tags = localDataSource.getTagsForPattern(entity.id).toTagNames()
                val sharedWith = localDataSource.getSharesForPattern(entity.id).toUserIds()
                entity.toDomain(tags, sharedWith)
            }
            Ok(patterns)
        } catch (e: Exception) {
            Logger.e("Ошибка чтения сегментов паттерна: $parentId", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun deleteSegmentsForPattern(parentId: PatternId): AppResult<Unit> {
        return try {
            Logger.d("Удаление сегментов паттерна (локально): $parentId", "PatternsRepositoryImpl")
            localDataSource.deleteSegmentsForPattern(parentId.value)
            // Ставим задачу в Outbox для синхронизации удаления сегментов на сервере.
            val nowMillis = System.currentTimeMillis()
            val payloadObject = buildJsonObject {
                put("patternId", parentId.value)
            }
            val payloadJson = json.encodeToString(payloadObject)

            val action = OutboxActionEntity(
                id = UUID.randomUUID().toString(),
                type = OutboxActionType.PATTERN_SEGMENTS_UPDATE,
                payloadJson = payloadJson,
                status = OutboxActionStatus.PENDING,
                retryCount = 0,
                lastError = null,
                idempotencyKey = "pattern_segments_${parentId.value}_$nowMillis",
                createdAt = nowMillis,
                updatedAt = nowMillis,
                availableAt = nowMillis,
                priority = 1,
                targetEntityId = parentId.value
            )

            Logger.d(
                "Постановка Outbox-задачи PATTERN_SEGMENTS_UPDATE (удаление) id=${action.id} patternId=${parentId.value}",
                "PatternsRepositoryImpl"
            )
            localDataSource.enqueueOutboxAction(action)

            outboxScheduler.scheduleSync()

            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка удаления сегментов паттерна: $parentId", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun upsertSegmentsForPattern(parentId: PatternId, segments: List<Pattern>): AppResult<Unit> {
        Logger.d("Пересохранение сегментов паттерна: $parentId, всего: ${segments.size}", "PatternsRepositoryImpl")
        return try {
            localDataSource.withPatternTransaction {
                // Удаляем старые сегменты
                localDataSource.deleteSegmentsForPattern(parentId.value)

                // Для каждого сегмента создаём/обновляем записи и связи с тегами
                segments.forEach { segment ->
                    val entity = segment.toEntity()
                    val tagNames = segment.tags
                    val existing = localDataSource.getTagsByNames(tagNames)
                    val existingNames = existing.map { it.name }.toSet()
                    val newNames = tagNames.filter { it !in existingNames }
                    val newEntities = newNames.toTagEntities()
                    val combined = existing + newEntities
                    val tagIds = combined.map { it.id }

                    localDataSource.upsertPatternWithRelations(
                        pattern = entity,
                        tags = combined,
                        tagIds = tagIds,
                        sharedUserIds = emptyList()
                    )
                }
            }
            // Ставим задачу в Outbox для синхронизации сегментов на сервере.
            val nowMillis = System.currentTimeMillis()
            val payloadObject = buildJsonObject {
                put("patternId", parentId.value)
            }
            val payloadJson = json.encodeToString(payloadObject)

            val action = OutboxActionEntity(
                id = UUID.randomUUID().toString(),
                type = OutboxActionType.PATTERN_SEGMENTS_UPDATE,
                payloadJson = payloadJson,
                status = OutboxActionStatus.PENDING,
                retryCount = 0,
                lastError = null,
                idempotencyKey = "pattern_segments_${parentId.value}_$nowMillis",
                createdAt = nowMillis,
                updatedAt = nowMillis,
                availableAt = nowMillis,
                priority = 1,
                targetEntityId = parentId.value
            )

            Logger.d(
                "Постановка Outbox-задачи PATTERN_SEGMENTS_UPDATE (upsert) id=${action.id} patternId=${parentId.value} segmentsCount=${segments.size}",
                "PatternsRepositoryImpl"
            )
            localDataSource.enqueueOutboxAction(action)

            outboxScheduler.scheduleSync()

            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка пересохранения сегментов паттерна: $parentId", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun getPatternMarkers(patternId: PatternId): AppResult<PatternMarkers?> {
        return try {
            val entity = localDataSource.getPatternMarkers(patternId.value)
            if (entity == null) {
                Ok(null)
            } else {
                val markers: List<Int> = json.decodeFromString(entity.markersJson)
                Ok(PatternMarkers(patternId = patternId, markersMs = markers))
            }
        } catch (e: Exception) {
            Logger.e("Ошибка чтения маркеров паттерна: $patternId", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun upsertPatternMarkers(markers: PatternMarkers): AppResult<Unit> {
        return try {
            Logger.d(
                "Обновление маркеров паттерна: ${markers.patternId}, всего маркеров=${markers.markersMs.size}",
                "PatternsRepositoryImpl"
            )
            val markersJson: String = json.encodeToString(markers.markersMs)
            val entity = com.example.amulet.core.database.entity.PatternMarkersEntity(
                patternId = markers.patternId.value,
                markersJson = markersJson
            )
            localDataSource.upsertPatternMarkers(entity)
            // Ставим задачу в Outbox для синхронизации маркеров на сервере.
            val nowMillis = System.currentTimeMillis()
            val payloadObject = buildJsonObject {
                put("patternId", markers.patternId.value)
                put(
                    "markersMs",
                    buildJsonArray {
                        markers.markersMs.forEach { value ->
                            add(JsonPrimitive(value))
                        }
                    }
                )
            }
            val payloadJson = json.encodeToString(payloadObject)

            val action = OutboxActionEntity(
                id = UUID.randomUUID().toString(),
                type = OutboxActionType.PATTERN_MARKERS_UPDATE,
                payloadJson = payloadJson,
                status = OutboxActionStatus.PENDING,
                retryCount = 0,
                lastError = null,
                idempotencyKey = "pattern_markers_${markers.patternId.value}_$nowMillis",
                createdAt = nowMillis,
                updatedAt = nowMillis,
                availableAt = nowMillis,
                priority = 1,
                targetEntityId = markers.patternId.value
            )

            Logger.d(
                "Постановка Outbox-задачи PATTERN_MARKERS_UPDATE (upsert) id=${action.id} patternId=${markers.patternId.value} markersCount=${markers.markersMs.size}",
                "PatternsRepositoryImpl"
            )
            localDataSource.enqueueOutboxAction(action)

            outboxScheduler.scheduleSync()

            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка сохранения маркеров паттерна: ${markers.patternId}", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }

    override suspend fun deletePatternMarkers(patternId: PatternId): AppResult<Unit> {
        return try {
            localDataSource.deletePatternMarkers(patternId.value)
            // Ставим задачу в Outbox для удаления маркеров на сервере.
            val nowMillis = System.currentTimeMillis()
            val payloadObject = buildJsonObject {
                put("patternId", patternId.value)
                put("markersMs", buildJsonArray { })
            }
            val payloadJson = json.encodeToString(payloadObject)

            val action = OutboxActionEntity(
                id = UUID.randomUUID().toString(),
                type = OutboxActionType.PATTERN_MARKERS_UPDATE,
                payloadJson = payloadJson,
                status = OutboxActionStatus.PENDING,
                retryCount = 0,
                lastError = null,
                idempotencyKey = "pattern_markers_${patternId.value}_$nowMillis",
                createdAt = nowMillis,
                updatedAt = nowMillis,
                availableAt = nowMillis,
                priority = 1,
                targetEntityId = patternId.value
            )

            Logger.d(
                "Постановка Outbox-задачи PATTERN_MARKERS_UPDATE (delete) id=${action.id} patternId=${patternId.value}",
                "PatternsRepositoryImpl"
            )
            localDataSource.enqueueOutboxAction(action)
            
            outboxScheduler.scheduleSync()

            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка удаления маркеров паттерна: $patternId", throwable = e, tag = "PatternsRepositoryImpl")
            Err(AppError.DatabaseError)
        }
    }
}
