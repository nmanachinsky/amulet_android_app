package com.example.amulet.feature.auth.presentation

import com.example.amulet.shared.core.AppError

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val authMode: AuthMode = AuthMode.SignIn,
    val isSubmitting: Boolean = false,
    val error: AppError? = null
)

enum class AuthMode {
    SignIn,
    SignUp
}

sealed interface AuthUiEvent {
    data class EmailChanged(val value: String) : AuthUiEvent
    data class PasswordChanged(val value: String) : AuthUiEvent
    data class ConfirmPasswordChanged(val value: String) : AuthUiEvent
    object Submit : AuthUiEvent
    object ErrorConsumed : AuthUiEvent
    object AuthModeSwitchRequested : AuthUiEvent
    object GoogleSignInRequested : AuthUiEvent
    data class GoogleIdTokenReceived(val idToken: String, val rawNonce: String?) : AuthUiEvent
    object GoogleSignInCancelled : AuthUiEvent
    data class GoogleSignInError(val throwable: Throwable?) : AuthUiEvent
    object GuestModeRequested : AuthUiEvent
}

sealed interface AuthSideEffect {
    object SignInSuccess : AuthSideEffect
    object LaunchGoogleSignIn : AuthSideEffect
}
