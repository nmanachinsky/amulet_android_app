package com.example.amulet.core.auth

import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.StateFlow

interface UserSessionManager {
    val authState: StateFlow<AuthState>
    val currentUserId: StateFlow<UserId?>
    val isLoggedIn: StateFlow<Boolean>
    val isGuest: StateFlow<Boolean>

    suspend fun setLoggedIn(userId: UserId)
    suspend fun setGuest()
    suspend fun setLoggedOut()
    suspend fun clear()
}
