package com.example.amulet.data.hugs.datasource.local

import com.example.amulet.core.database.TransactionRunner
import com.example.amulet.core.database.dao.OutboxActionDao
import com.example.amulet.core.database.dao.PairDao
import com.example.amulet.core.database.entity.OutboxActionEntity
import com.example.amulet.core.database.entity.PairEmotionEntity
import com.example.amulet.core.database.entity.PairEntity
import com.example.amulet.core.database.entity.PairMemberEntity
import com.example.amulet.core.database.entity.PairQuickReplyEntity
import com.example.amulet.core.database.relation.PairWithMemberSettings
import com.example.amulet.shared.core.logging.Logger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PairsLocalDataSourceImpl @Inject constructor(
    private val pairDao: PairDao,
    private val outboxDao: OutboxActionDao,
    private val transactionRunner: TransactionRunner,
) : PairsLocalDataSource {

    override fun observeAllWithSettings(): Flow<List<PairWithMemberSettings>> =
        pairDao.observeAllWithSettings()

    override fun observePairWithSettings(pairId: String): Flow<PairWithMemberSettings?> =
        pairDao.observePairWithSettings(pairId)

    override fun observeEmotions(pairId: String): Flow<List<PairEmotionEntity>> =
        pairDao.observeEmotions(pairId)

    override suspend fun upsertEmotions(entities: List<PairEmotionEntity>) {
        pairDao.upsertEmotions(entities)
    }

    override suspend fun deleteEmotions(pairId: String) {
        pairDao.deleteEmotions(pairId)
    }

    override suspend fun deleteEmotionsNotIn(pairId: String, emotionIds: List<String>) {
        pairDao.deleteEmotionsNotIn(pairId, emotionIds)
    }

    override suspend fun updateMemberSettings(
        pairId: String,
        userId: String,
        muted: Boolean,
        quietStart: Int?,
        quietEnd: Int?,
        maxHugsPerHour: Int?
    ) {
        pairDao.updateMemberSettings(pairId, userId, muted, quietStart, quietEnd, maxHugsPerHour)
    }

    override fun observeQuickReplies(pairId: String, userId: String): Flow<List<PairQuickReplyEntity>> =
        pairDao.observeQuickReplies(pairId, userId)

    override suspend fun upsertQuickReplies(entities: List<PairQuickReplyEntity>) {
        pairDao.upsertQuickReplies(entities)
    }

    override suspend fun deleteQuickReplies(pairId: String, userId: String) {
        pairDao.deleteQuickReplies(pairId, userId)
    }

    override suspend fun enqueueOutboxAction(action: OutboxActionEntity) {
        outboxDao.upsert(action)
    }

    override suspend fun <R> withPairTransaction(block: suspend () -> R): R {
        return transactionRunner.runInTransaction(block)
    }

    override suspend fun replaceAllPairs(pairs: List<PairEntity>, members: List<PairMemberEntity>) {
        transactionRunner.runInTransaction {
            Logger.d(
                "PairsLocalDataSourceImpl.replaceAllPairs: start pairsCount=${pairs.size} membersCount=${members.size}",
                "PairsLocalDataSourceImpl"
            )
            if (pairs.isEmpty()) {
                Logger.e(
                    "PairsLocalDataSourceImpl.replaceAllPairs: pairs is empty -> deleting all pairs/members (will cascade)",
                    throwable = IllegalStateException("pairs is empty"),
                    tag = "PairsLocalDataSourceImpl"
                )
                pairDao.deleteAllMembers()
                pairDao.deleteAllPairs()
                return@runInTransaction
            }

            val pairIds = pairs.map { it.id }
            Logger.d(
                "PairsLocalDataSourceImpl.replaceAllPairs: applying pairIds=${pairIds.joinToString()}",
                "PairsLocalDataSourceImpl"
            )

            // Удаляем только пары/мемберы, которых больше нет на сервере.
            // Это важно, чтобы не удалять каскадно pair_emotions для существующих пар.
            pairDao.deleteMembersNotInPairs(pairIds)
            pairDao.deletePairsNotIn(pairIds)

            val membersByPairId: Map<String, List<PairMemberEntity>> =
                members.groupBy { it.pairId }

            for (pair in pairs) {
                val pairMembers = membersByPairId[pair.id].orEmpty()
                pairDao.upsertPairWithMembers(pair, pairMembers)
            }

            Logger.d(
                "PairsLocalDataSourceImpl.replaceAllPairs: done pairsCount=${pairs.size} membersCount=${members.size}",
                "PairsLocalDataSourceImpl"
            )
        }
    }

    override suspend fun getAllMembers(): List<PairMemberEntity> =
        pairDao.getAllMembers()

    override suspend fun deletePair(pairId: String) {
        transactionRunner.runInTransaction {
            pairDao.deleteEmotions(pairId)
            pairDao.deleteMembers(pairId)
            pairDao.deletePair(pairId)
        }
    }
}
