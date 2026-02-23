package com.example.amulet_android_app.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.auth.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject



@HiltViewModel
class SessionViewModel @Inject constructor(
    observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    val state: StateFlow<AuthState> = observeAuthStateUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthState.Loading
    )
}
