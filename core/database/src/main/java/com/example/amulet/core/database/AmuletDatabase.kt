package com.example.amulet.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.amulet.core.database.converter.InstantConverters
import com.example.amulet.core.database.converter.UserConverters
import com.example.amulet.core.database.dao.DeviceDao
import com.example.amulet.core.database.dao.FirmwareInfoDao
import com.example.amulet.core.database.dao.HugDao
import com.example.amulet.core.database.dao.OutboxActionDao
import com.example.amulet.core.database.dao.PairDao
import com.example.amulet.core.database.dao.PatternDao
import com.example.amulet.core.database.dao.PracticeDao
import com.example.amulet.core.database.dao.PracticeExtrasDao
import com.example.amulet.core.database.dao.PrivacyJobDao
import com.example.amulet.core.database.dao.CourseDao
import com.example.amulet.core.database.dao.RemoteKeyDao
import com.example.amulet.core.database.dao.RuleDao
import com.example.amulet.core.database.dao.TelemetryDao
import com.example.amulet.core.database.dao.UserDao
import com.example.amulet.core.database.entity.DeviceEntity
import com.example.amulet.core.database.entity.FirmwareInfoEntity
import com.example.amulet.core.database.entity.HugEntity
import com.example.amulet.core.database.entity.OutboxActionEntity
import com.example.amulet.core.database.entity.PairEntity
import com.example.amulet.core.database.entity.PairEmotionEntity
import com.example.amulet.core.database.entity.PairMemberEntity
import com.example.amulet.core.database.entity.PairQuickReplyEntity
import com.example.amulet.core.database.entity.PatternEntity
import com.example.amulet.core.database.entity.PatternShareEntity
import com.example.amulet.core.database.entity.PatternTagCrossRef
import com.example.amulet.core.database.entity.PatternMarkersEntity
import com.example.amulet.core.database.entity.PracticeEntity
import com.example.amulet.core.database.entity.PracticeSessionEntity
import com.example.amulet.core.database.entity.PracticeCategoryEntity
import com.example.amulet.core.database.entity.PracticeFavoriteEntity
import com.example.amulet.core.database.entity.PracticeScheduleEntity
import com.example.amulet.core.database.entity.UserPreferencesEntity
import com.example.amulet.core.database.entity.PrivacyJobEntity
import com.example.amulet.core.database.entity.RemoteKeyEntity
import com.example.amulet.core.database.entity.RuleEntity
import com.example.amulet.core.database.entity.TagEntity
import com.example.amulet.core.database.entity.TelemetryEventEntity
import com.example.amulet.core.database.entity.UserEntity
import com.example.amulet.core.database.entity.CollectionEntity
import com.example.amulet.core.database.entity.CollectionItemEntity
import com.example.amulet.core.database.entity.CourseEntity
import com.example.amulet.core.database.entity.CourseItemEntity
import com.example.amulet.core.database.entity.CourseModuleEntity
import com.example.amulet.core.database.entity.CourseProgressEntity
import com.example.amulet.core.database.entity.PlanEntity
import com.example.amulet.core.database.entity.PracticeTagCrossRef
import com.example.amulet.core.database.entity.PracticeTagEntity
import com.example.amulet.core.database.entity.UserBadgeEntity
import com.example.amulet.core.database.entity.UserMoodEntryEntity
import com.example.amulet.core.database.entity.UserPracticeStatsEntity

@Database(
    entities = [
        UserEntity::class,
        DeviceEntity::class,
        HugEntity::class,
        PatternEntity::class,
        TagEntity::class,
        PatternTagCrossRef::class,
        PatternShareEntity::class,
        PatternMarkersEntity::class,
        PairEntity::class,
        PairMemberEntity::class,
        PairEmotionEntity::class,
        PairQuickReplyEntity::class,
        PracticeEntity::class,
        PracticeSessionEntity::class,
        PracticeCategoryEntity::class,
        PracticeFavoriteEntity::class,
        PracticeScheduleEntity::class,
        UserPreferencesEntity::class,
        PlanEntity::class,
        UserPracticeStatsEntity::class,
        UserBadgeEntity::class,
        PracticeTagEntity::class,
        PracticeTagCrossRef::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        CourseEntity::class,
        CourseItemEntity::class,
        CourseModuleEntity::class,
        CourseProgressEntity::class,
        RuleEntity::class,
        TelemetryEventEntity::class,
        PrivacyJobEntity::class,
        FirmwareInfoEntity::class,
        OutboxActionEntity::class,
        RemoteKeyEntity::class,
        UserMoodEntryEntity::class
    ],
    version = 16,
    exportSchema = true
)
@TypeConverters(DatabaseTypeConverters::class, UserConverters::class, InstantConverters::class)
abstract class AmuletDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun hugDao(): HugDao
    abstract fun patternDao(): PatternDao
    abstract fun pairDao(): PairDao
    abstract fun practiceDao(): PracticeDao
    abstract fun practiceExtrasDao(): PracticeExtrasDao
    abstract fun courseDao(): CourseDao
    abstract fun ruleDao(): RuleDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun privacyJobDao(): PrivacyJobDao
    abstract fun firmwareInfoDao(): FirmwareInfoDao
    abstract fun outboxActionDao(): OutboxActionDao
    abstract fun remoteKeyDao(): RemoteKeyDao
}

