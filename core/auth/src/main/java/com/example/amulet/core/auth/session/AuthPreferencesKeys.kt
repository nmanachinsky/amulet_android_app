package com.example.amulet.core.auth.session

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

object AuthPreferencesKeys {
    val AUTH_STATE = stringPreferencesKey("auth_state")
    val USER_ID = stringPreferencesKey("user_id")
}
