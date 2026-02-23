package com.example.amulet.data.user.datasource.local

import com.example.amulet.core.database.dao.UserDao
import com.example.amulet.core.database.entity.UserEntity
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.privacy.model.UserConsents
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface UserLocalDataSource {
    suspend fun upsert(user: UserEntity): AppResult<Unit>
    suspend fun findById(userId: String): AppResult<UserEntity?>
    fun observeById(userId: String): Flow<UserEntity?>
    fun observeConsents(userId: String): Flow<UserConsents>
    suspend fun updateConsents(userId: String, consents: UserConsents): AppResult<Unit>
}

@Singleton
class UserLocalDataSourceImpl @Inject constructor(
    private val userDao: UserDao
) : UserLocalDataSource {

    override suspend fun upsert(user: UserEntity): AppResult<Unit> = runCatching {
        Logger.d("upsert: userId=${user.id}", TAG)
        userDao.upsert(user)
    }.fold(
        onSuccess = {
            Logger.i("upsert: success userId=${user.id}", TAG)
            Ok(Unit)
        },
        onFailure = { throwable ->
            Logger.e("upsert: failed", throwable, TAG)
            Err(AppError.DatabaseError)
        }
    )

    override suspend fun findById(userId: String): AppResult<UserEntity?> = runCatching {
        userDao.getById(userId)
    }.fold(
        onSuccess = { Ok(it) },
        onFailure = { throwable ->
            Logger.e("findById: failed", throwable, TAG)
            Err(AppError.DatabaseError)
        }
    )

    override fun observeById(userId: String): Flow<UserEntity?> =
        userDao.observeById(userId)

    override fun observeConsents(userId: String): Flow<UserConsents> =
        userDao.observeById(userId).map { entity ->
            entity?.consents ?: UserConsents()
        }

    override suspend fun updateConsents(userId: String, consents: UserConsents): AppResult<Unit> =
        runCatching {
            val entity = userDao.getById(userId)
            if (entity != null) {
                userDao.upsert(entity.copy(consents = consents))
            }
        }.fold(
            onSuccess = {
                Logger.i("updateConsents: success userId=$userId", TAG)
                Ok(Unit)
            },
            onFailure = { throwable ->
                Logger.e("updateConsents: failed", throwable, TAG)
                Err(AppError.DatabaseError)
            }
        )

    private companion object {
        const val TAG = "UserLocalDataSource"
    }
}
