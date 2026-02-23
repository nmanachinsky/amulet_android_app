package com.example.amulet.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hugs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromUserId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["toUserId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PatternEntity::class,
            parentColumns = ["id"],
            childColumns = ["emotionPatternId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            value = ["createdAt"],
            orders = [Index.Order.DESC]
        ),
        Index(
            value = ["fromUserId", "createdAt"],
            orders = [Index.Order.ASC, Index.Order.DESC]
        ),
        Index(
            value = ["toUserId", "createdAt"],
            orders = [Index.Order.ASC, Index.Order.DESC]
        ),
        Index(value = ["emotionPatternId"]),
        Index(
            value = ["pairId", "createdAt"],
            orders = [Index.Order.ASC, Index.Order.DESC]
        ),
        Index(value = ["status"]) 
    ]
)
data class HugEntity(
    @PrimaryKey val id: String,
    val fromUserId: String?,
    val toUserId: String?,
    val pairId: String?,
    val emotionColor: String?,
    val emotionPatternId: String?,
    val payloadJson: String?,
    val inReplyToHugId: String?,
    val deliveredAt: Long?,
    @ColumnInfo(defaultValue = "SENT") val status: String,
    @ColumnInfo(defaultValue = "0") val createdAt: Long
)
