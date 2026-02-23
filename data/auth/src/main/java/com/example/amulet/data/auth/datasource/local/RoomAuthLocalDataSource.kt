package com.example.amulet.data.auth.datasource.local

import com.example.amulet.core.database.AmuletDatabase
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RoomAuthLocalDataSource @Inject constructor(
    private val database: AmuletDatabase
) : AuthLocalDataSource {

    override suspend fun clearAll(): AppResult<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            Logger.d("clearAll: starting", TAG)
            database.clearAllTables()
        }
    }.fold(
        onSuccess = {
            Logger.i("clearAll: success", TAG)
            Ok(Unit)
        },
        onFailure = { throwable ->
            Logger.e("clearAll: failed", throwable, TAG)
            Err(AppError.DatabaseError)
        }
    )

    override suspend fun deleteByUserId(userId: String): AppResult<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            Logger.d("deleteByUserId: starting for userId=$userId", TAG)
            database.userDao().deleteById(userId)
        }
    }.fold(
        onSuccess = {
            Logger.i("deleteByUserId: success for userId=$userId", TAG)
            Ok(Unit)
        },
        onFailure = { throwable ->
            Logger.e("deleteByUserId: failed for userId=$userId", throwable, TAG)
            Err(AppError.DatabaseError)
        }
    )

    private companion object {
        const val TAG = "RoomAuthLocalDataSource"
    }
}
