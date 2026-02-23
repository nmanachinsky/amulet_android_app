package com.example.amulet.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patterns",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["public", "hardwareVersion", "kind"])
    ]
)
data class PatternEntity(
    @PrimaryKey val id: String,
    val ownerId: String?,
    val kind: String,
    val hardwareVersion: Int,
    val title: String,
    val description: String?,
    val specJson: String,
    val public: Boolean,
    val reviewStatus: String?,
    val usageCount: Int?,
    val version: Int = 1,
    val createdAt: Long?,
    val updatedAt: Long?,
    val parentPatternId: String? = null,
    val segmentIndex: Int? = null,
    val segmentStartMs: Int? = null,
    val segmentEndMs: Int? = null,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(
    tableName = "pattern_tags",
    primaryKeys = ["patternId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = PatternEntity::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tagId"])]
)
data class PatternTagCrossRef(
    val patternId: String,
    val tagId: String
)

@Entity(
    tableName = "pattern_shares",
    primaryKeys = ["patternId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = PatternEntity::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class PatternShareEntity(
    val patternId: String,
    val userId: String
)

@Entity(
    tableName = "pattern_markers",
    foreignKeys = [
        ForeignKey(
            entity = PatternEntity::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["patternId"], unique = true)]
)
data class PatternMarkersEntity(
    @PrimaryKey val patternId: String,
    val markersJson: String
)
