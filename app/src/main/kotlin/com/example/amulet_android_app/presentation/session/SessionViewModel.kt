package com.example.amulet_android_app.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data object Guest : AuthState
    data object LoggedIn : AuthState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionProvider: UserSessionProvider
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionProvider.sessionContext.collectLatest { context ->
                _state.value = when (context) {
                    UserSessionContext.Loading -> AuthState.Loading
                    UserSessionContext.LoggedOut -> AuthState.LoggedOut
                    is UserSessionContext.Guest -> AuthState.Guest
                    is UserSessionContext.LoggedIn -> AuthState.LoggedIn
                }
            }
        }
    }
}
