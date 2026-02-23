package com.example.amulet.core.auth.session

import androidx.datastore.preferences.core.Preferences
import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.user.model.UserId

class AuthStateMapper {

    fun map(prefs: Preferences): AuthState {
        val state = prefs[AuthPreferencesKeys.AUTH_STATE]
        val userId = prefs[AuthPreferencesKeys.USER_ID]

        return when (state) {
            STATE_LOGGED_IN -> if (userId != null) AuthState.LoggedIn(UserId(userId)) else AuthState.LoggedOut
            STATE_GUEST -> if (userId != null) AuthState.Guest(UserId(userId)) else AuthState.LoggedOut
            STATE_LOGGED_OUT -> AuthState.LoggedOut
            else -> AuthState.Loading
        }
    }

    private companion object {
        const val STATE_LOGGED_IN = "logged_in"
        const val STATE_GUEST = "guest"
        const val STATE_LOGGED_OUT = "logged_out"
    }
}
