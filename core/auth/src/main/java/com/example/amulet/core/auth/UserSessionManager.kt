package com.example.amulet.core.auth

import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserSessionManager {
    val authState: StateFlow<AuthState>

    suspend fun setLoggedIn(userId: UserId)
    suspend fun setGuest(userId: UserId)
    suspend fun setLoggedOut()
    suspend fun clear()

    fun isLoggedIn(): Boolean
    fun isGuest(): Boolean
    fun getCurrentUserId(): UserId?
    fun hasActiveSession(): Boolean
}
