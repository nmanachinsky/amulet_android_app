package com.example.amulet.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.amulet.core.database.entity.PairEntity
import com.example.amulet.core.database.entity.PairMemberEntity
import com.example.amulet.core.database.entity.PairEmotionEntity
import com.example.amulet.core.database.entity.PairQuickReplyEntity
import com.example.amulet.core.database.relation.PairWithMembers
import com.example.amulet.core.database.relation.PairWithMemberSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface PairDao {

    @Transaction
    @Query("SELECT * FROM pairs WHERE id = :pairId")
    fun observeById(pairId: String): Flow<PairWithMembers?>

    @Transaction
    @Query("SELECT * FROM pairs ORDER BY createdAt DESC")
    fun observeAllWithSettings(): Flow<List<PairWithMemberSettings>>

    @Transaction
    @Query("SELECT * FROM pairs WHERE id = :pairId")
    fun observePairWithSettings(pairId: String): Flow<PairWithMemberSettings?>

    @Transaction
    @Query("SELECT * FROM pairs ORDER BY createdAt DESC")
    fun pagingAll(): PagingSource<Int, PairWithMembers>

    @Transaction
    @Query("SELECT * FROM pairs WHERE status = :status ORDER BY createdAt DESC")
    fun pagingByStatus(status: String): PagingSource<Int, PairWithMembers>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPairIgnore(pair: PairEntity): Long

    @Update
    suspend fun updatePair(pair: PairEntity): Int

    @Transaction
    suspend fun upsertPair(pair: PairEntity) {
        val inserted = insertPairIgnore(pair)
        if (inserted == -1L) {
            updatePair(pair)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<PairMemberEntity>)

    @Query("SELECT * FROM pair_members")
    suspend fun getAllMembers(): List<PairMemberEntity>

    @Query(
        "UPDATE pair_members SET muted = :muted, quietHoursStartMinutes = :quietStart, " +
            "quietHoursEndMinutes = :quietEnd, maxHugsPerHour = :maxHugsPerHour " +
            "WHERE pairId = :pairId AND userId = :userId"
    )
    suspend fun updateMemberSettings(
        pairId: String,
        userId: String,
        muted: Boolean,
        quietStart: Int?,
        quietEnd: Int?,
        maxHugsPerHour: Int?
    )

    @Query("DELETE FROM pair_members WHERE pairId = :pairId")
    suspend fun deleteMembers(pairId: String)

    @Query("DELETE FROM pairs WHERE id = :pairId")
    suspend fun deletePair(pairId: String)

    @Query("DELETE FROM pair_members")
    suspend fun deleteAllMembers()

    @Query("DELETE FROM pairs")
    suspend fun deleteAllPairs()

    @Query("DELETE FROM pair_members WHERE pairId NOT IN (:pairIds)")
    suspend fun deleteMembersNotInPairs(pairIds: List<String>)

    @Query("DELETE FROM pairs WHERE id NOT IN (:pairIds)")
    suspend fun deletePairsNotIn(pairIds: List<String>)

    // Palette (pair_emotions)

    @Query("SELECT * FROM pair_emotions WHERE pairId = :pairId ORDER BY `order` ASC")
    fun observeEmotions(pairId: String): Flow<List<PairEmotionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmotionsIgnore(entities: List<PairEmotionEntity>): List<Long>

    @Update
    suspend fun updateEmotions(entities: List<PairEmotionEntity>): Int

    @Transaction
    suspend fun upsertEmotions(entities: List<PairEmotionEntity>) {
        insertEmotionsIgnore(entities)
        updateEmotions(entities)
    }

    @Query("DELETE FROM pair_emotions WHERE pairId = :pairId")
    suspend fun deleteEmotions(pairId: String)

    @Query("DELETE FROM pair_emotions WHERE pairId = :pairId AND id NOT IN (:emotionIds)")
    suspend fun deleteEmotionsNotIn(pairId: String, emotionIds: List<String>)

    // Quick replies (pair_quick_replies)

    @Query("SELECT * FROM pair_quick_replies WHERE pairId = :pairId AND userId = :userId")
    fun observeQuickReplies(pairId: String, userId: String): Flow<List<PairQuickReplyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuickReplies(entities: List<PairQuickReplyEntity>)

    @Query("DELETE FROM pair_quick_replies WHERE pairId = :pairId AND userId = :userId")
    suspend fun deleteQuickReplies(pairId: String, userId: String)

    @Transaction
    suspend fun upsertPairWithMembers(pair: PairEntity, members: List<PairMemberEntity>) {
        upsertPair(pair)
        deleteMembers(pair.id)
        if (members.isNotEmpty()) {
            insertMembers(members)
        }
    }
}
