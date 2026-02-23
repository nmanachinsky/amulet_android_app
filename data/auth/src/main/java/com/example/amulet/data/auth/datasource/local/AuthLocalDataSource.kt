package com.example.amulet.data.auth.datasource.local

import com.example.amulet.shared.core.AppResult

interface AuthLocalDataSource {
    suspend fun clearAll(): AppResult<Unit>
    suspend fun deleteByUserId(userId: String): AppResult<Unit>
}
