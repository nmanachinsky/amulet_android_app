package com.example.amulet.feature.settings.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.domain.user.model.UpdateUserProfileRequest
import com.example.amulet.shared.domain.user.model.User
import com.example.amulet.shared.domain.user.usecase.ObserveCurrentUserUseCase
import com.example.amulet.shared.domain.user.usecase.UpdateLocalUserPreferencesUseCase
import com.example.amulet.shared.domain.user.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val updateLocalUserPreferencesUseCase: UpdateLocalUserPreferencesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSettingsState())
    val state: StateFlow<ProfileSettingsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ProfileSettingsEffect>()
    val effects = _effects.asSharedFlow()

    init {
        observeUser()
    }

    private fun observeUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            observeCurrentUserUseCase()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = _state.value.currentUser,
                )
                .collect { user ->
                    _state.update { current ->
                        val newDisplayName =
                            if (current.displayNameInput.isEmpty()) user?.displayName.orEmpty() else current.displayNameInput
                        val newAvatarUrl =
                            if (current.avatarUrlInput.isEmpty()) user?.avatarUrl.orEmpty() else current.avatarUrlInput
                        val newTimezone =
                            if (current.timezoneInput.isEmpty()) user?.timezone.orEmpty() else current.timezoneInput
                        val newLanguage =
                            if (current.languageInput.isEmpty()) user?.language.orEmpty() else current.languageInput

                        current.copy(
                            isLoading = false,
                            currentUser = user,
                            displayNameInput = newDisplayName,
                            avatarUrlInput = newAvatarUrl,
                            timezoneInput = newTimezone,
                            languageInput = newLanguage,
                        )
                    }
                }
        }
    }

    fun onIntent(intent: ProfileSettingsIntent) {
        when (intent) {
            ProfileSettingsIntent.NavigateBack -> emitEffect(ProfileSettingsEffect.NavigateBack)

            is ProfileSettingsIntent.DisplayNameChanged ->
                _state.update { it.copy(displayNameInput = intent.value) }

            is ProfileSettingsIntent.AvatarChanged ->
                _state.update { it.copy(avatarUrlInput = intent.uri) }

            is ProfileSettingsIntent.TimezoneChanged ->
                _state.update { it.copy(timezoneInput = intent.value) }

            is ProfileSettingsIntent.LanguageChanged ->
                _state.update { it.copy(languageInput = intent.value) }

            ProfileSettingsIntent.SaveClicked -> saveProfile()
            ProfileSettingsIntent.ChangePasswordClicked ->
                emitEffect(ProfileSettingsEffect.NavigateToChangePassword)
        }
    }

    private fun saveProfile() {
        val currentState = _state.value
        val user = currentState.currentUser ?: return

        val trimmedTimezone = currentState.timezoneInput.trim()
        val trimmedLanguage = currentState.languageInput.trim()

        // Серверные поля профиля (имя, аватар) — синхронизируются с backend
        val profileRequest = UpdateUserProfileRequest(
            displayName = currentState.displayNameInput.trim().takeIf { it != (user.displayName ?: "") },
            avatarUrl = currentState.avatarUrlInput.trim().takeIf { it != (user.avatarUrl ?: "") },
        )
        val hasProfileChanges = profileRequest.displayName != null || profileRequest.avatarUrl != null

        // Локальные настройки (часовой пояс, язык) — только на устройстве
        val hasLocalPreferenceChanges =
            trimmedTimezone != (user.timezone ?: "") || trimmedLanguage != (user.language ?: "")

        if (!hasProfileChanges && !hasLocalPreferenceChanges) {
            emitEffect(ProfileSettingsEffect.NavigateBack)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val error = saveChanges(
                user = user,
                profileRequest = profileRequest,
                hasProfileChanges = hasProfileChanges,
                hasLocalPreferenceChanges = hasLocalPreferenceChanges,
                timezone = trimmedTimezone,
                language = trimmedLanguage,
            )

            _state.update { it.copy(isSaving = false) }

            if (error != null) {
                emitEffect(ProfileSettingsEffect.ShowError(error))
            } else {
                emitEffect(ProfileSettingsEffect.NavigateBack)
            }
        }
    }

    /**
     * Сохраняет изменения и возвращает первую возникшую ошибку либо null при успехе.
     *
     * Локальные настройки сохраняем первыми: они не зависят от сети и не должны
     * теряться из-за возможной ошибки серверного обновления профиля.
     */
    private suspend fun saveChanges(
        user: User,
        profileRequest: UpdateUserProfileRequest,
        hasProfileChanges: Boolean,
        hasLocalPreferenceChanges: Boolean,
        timezone: String,
        language: String,
    ): AppError? {
        if (hasLocalPreferenceChanges) {
            val localError = updateLocalUserPreferencesUseCase(user.id, timezone, language).component2()
            if (localError != null) return localError
        }

        if (hasProfileChanges) {
            val profileError = updateUserProfileUseCase(profileRequest).component2()
            if (profileError != null) return profileError
        }

        return null
    }

    private fun emitEffect(effect: ProfileSettingsEffect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }
}
