package com.example.amulet.shared.domain.auth.repository

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.auth.model.UserCredentials
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun signUp(credentials: UserCredentials): AppResult<UserId>
    suspend fun signIn(credentials: UserCredentials): AppResult<UserId>
    suspend fun signInWithGoogle(idToken: String, rawNonce: String?): AppResult<UserId>
    suspend fun signOut(): AppResult<Unit>

    suspend fun restoreSession(userId: UserId): AppResult<Unit>
    suspend fun startGuestSession(): AppResult<Unit>
    suspend fun clearSession(): AppResult<Unit>

    fun isLoggedIn(): Boolean
    fun isGuest(): Boolean
    fun getCurrentUserId(): UserId?
    fun hasActiveSession(): Boolean
}
