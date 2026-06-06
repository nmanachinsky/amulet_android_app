package com.example.amulet.feature.hugs.presentation.settings

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.domain.hugs.model.Pair
import com.example.amulet.shared.domain.user.model.User

data class HugsSettingsState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    val currentUser: User? = null,
    val activePair: Pair? = null,
    val globalDndEnabled: Boolean = false,
    val isMuted: Boolean = false,
    val quietHoursStartMinutes: Int? = null,
    val quietHoursEndMinutes: Int? = null,
    val quietHoursStartText: String = "",
    val quietHoursEndText: String = "",
    val maxHugsPerHourText: String = "",
    // Поля ввода инициализируем сохранёнными настройками один раз, далее их владелец — пользователь
    val hasInitializedInputs: Boolean = false,
)

sealed class HugsSettingsIntent {
    object Refresh : HugsSettingsIntent()
    data class ToggleGlobalDnd(val enabled: Boolean) : HugsSettingsIntent()
    data class ChangeQuietStartText(val value: String) : HugsSettingsIntent()
    data class ChangeQuietEndText(val value: String) : HugsSettingsIntent()
    data class ChangeMaxHugsPerHour(val value: String) : HugsSettingsIntent()
    data class ToggleMuted(val enabled: Boolean) : HugsSettingsIntent()
    object SavePairSettings : HugsSettingsIntent()
    object DisconnectPair : HugsSettingsIntent()
    object UnblockPair : HugsSettingsIntent()
    object DeletePair : HugsSettingsIntent()
}

sealed class HugsSettingsEffect {
    data class ShowError(val error: AppError) : HugsSettingsEffect()
    object Close : HugsSettingsEffect()
}
