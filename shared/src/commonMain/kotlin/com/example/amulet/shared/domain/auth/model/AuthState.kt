package com.example.amulet.shared.domain.auth.model

import com.example.amulet.shared.domain.user.model.UserId

sealed interface AuthState {
    object Loading : AuthState
    data class Guest(val userId: UserId): AuthState
    object LoggedOut : AuthState
    data class LoggedIn(val userId: UserId) : AuthState
}