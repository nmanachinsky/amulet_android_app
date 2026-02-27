package com.example.amulet.feature.hugs.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.amulet.core.design.components.card.AmuletCard
import com.example.amulet.core.design.components.textfield.AmuletTextField
import com.example.amulet.core.design.scaffold.LocalScaffoldState
import com.example.amulet.feature.hugs.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HugsSettingsRoute(
    onNavigateBack: () -> Unit = {},
    viewModel: HugsSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scaffoldState = LocalScaffoldState.current
    val snackbarHostState = remember { SnackbarHostState() }

    SideEffect {
        scaffoldState.updateConfig {
            copy(
                topBar = {
                    TopAppBar(
                        title = { Text(text = stringResource(R.string.hugs_settings_title)) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors()
                    )
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HugsSettingsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.error.toString(),
                        duration = SnackbarDuration.Long
                    )
                }
                HugsSettingsEffect.Close -> onNavigateBack()
            }
        }
    }

    HugsSettingsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onSaveClick = { viewModel.onIntent(HugsSettingsIntent.SavePairSettings) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HugsSettingsScreen(
    state: HugsSettingsState,
    onIntent: (HugsSettingsIntent) -> Unit,
    onSaveClick: () -> Unit,
) {
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStartTimePickerDialog by remember { mutableStateOf(false) }
    var showEndTimePickerDialog by remember { mutableStateOf(false) }

    val startInitialMinutes = state.quietHoursStartMinutes ?: (22 * 60)
    val endInitialMinutes = state.quietHoursEndMinutes ?: (7 * 60)

    val startTimePickerState = rememberTimePickerState(
        initialHour = startInitialMinutes / 60,
        initialMinute = startInitialMinutes % 60,
        is24Hour = true,
    )

    val endTimePickerState = rememberTimePickerState(
        initialHour = endInitialMinutes / 60,
        initialMinute = endInitialMinutes % 60,
        is24Hour = true,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Секция режимов работы
        item {
            AmuletCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Заголовок секции
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoNotDisturb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.hugs_settings_work_modes_section_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Глобальный DND
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.hugs_settings_global_dnd_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.hugs_settings_global_dnd_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.globalDndEnabled,
                            onCheckedChange = { enabled -> onIntent(HugsSettingsIntent.ToggleGlobalDnd(enabled)) }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Тихие часы
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.hugs_settings_quiet_hours_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = stringResource(R.string.hugs_settings_quiet_hours_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showStartTimePickerDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = state.quietHoursStartText.ifBlank { "--:--" },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            OutlinedButton(
                                onClick = { showEndTimePickerDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = state.quietHoursEndText.ifBlank { "--:--" },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Секция ограничений
        item {
            AmuletCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.hugs_settings_limits_section_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hugs_settings_anti_spam_title),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.hugs_settings_anti_spam_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AmuletTextField(
                            value = state.maxHugsPerHourText,
                            onValueChange = { onIntent(HugsSettingsIntent.ChangeMaxHugsPerHour(it)) },
                            label = stringResource(R.string.hugs_settings_anti_spam_label),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Секция управления парой
        item {
            AmuletCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.hugs_settings_pair_section_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = stringResource(R.string.hugs_settings_pair_mute_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Переключатель заглушки
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                tint = if (state.isMuted) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (state.isMuted) stringResource(R.string.hugs_settings_pair_muted) else stringResource(R.string.hugs_settings_pair_active),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = state.isMuted,
                            onCheckedChange = { enabled -> onIntent(HugsSettingsIntent.ToggleMuted(enabled)) }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    val pairStatus = state.activePair?.status

                    // Кнопка разрыва связи (только если пара не заблокирована)
                    Button(
                        onClick = { showDisconnectDialog = true },
                        enabled = state.activePair != null && pairStatus != com.example.amulet.shared.domain.hugs.model.PairStatus.BLOCKED,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LinkOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.hugs_settings_disconnect_button))
                    }

                    // Кнопка разблокировки пары (если пара заблокирована)
                    if (pairStatus == com.example.amulet.shared.domain.hugs.model.PairStatus.BLOCKED) {
                        OutlinedButton(
                            onClick = { onIntent(HugsSettingsIntent.UnblockPair) },
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.hugs_settings_unblock_button))
                        }
                    }

                    // Кнопка полного удаления пары (доступна когда пара заблокирована)
                    if (pairStatus == com.example.amulet.shared.domain.hugs.model.PairStatus.BLOCKED) {
                        Text(
                            text = stringResource(R.string.hugs_settings_delete_pair_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showDeleteDialog = true },
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(text = stringResource(R.string.hugs_settings_delete_pair_button))
                        }
                    }
                }
            }
        }

        // Кнопка сохранения
        if (state.activePair != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSaveClick,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.hugs_settings_save_button))
                }
            }
        }
    }

    // Диалог подтверждения разрыва связи
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(text = stringResource(R.string.hugs_settings_disconnect_dialog_title)) },
            text = {
                Text(
                    text = stringResource(R.string.hugs_settings_disconnect_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        onIntent(HugsSettingsIntent.DisconnectPair)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = stringResource(R.string.hugs_settings_disconnect_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(text = stringResource(R.string.hugs_settings_cancel))
                }
            }
        )
    }

    // Диалог подтверждения полного удаления пары
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(text = stringResource(R.string.hugs_settings_delete_pair_dialog_title)) },
            text = {
                Text(
                    text = stringResource(R.string.hugs_settings_delete_pair_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onIntent(HugsSettingsIntent.DeletePair)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = stringResource(R.string.hugs_settings_delete_pair_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(R.string.hugs_settings_cancel))
                }
            }
        )
    }

    if (showStartTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { showStartTimePickerDialog = false },
            title = { Text(text = stringResource(R.string.hugs_settings_quiet_hours_start_label)) },
            text = {
                TimePicker(state = startTimePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartTimePickerDialog = false
                        val hour = startTimePickerState.hour
                        val minute = startTimePickerState.minute
                        val text = "%02d:%02d".format(hour, minute)
                        onIntent(HugsSettingsIntent.ChangeQuietStartText(text))
                    }
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePickerDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    if (showEndTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { showEndTimePickerDialog = false },
            title = { Text(text = stringResource(R.string.hugs_settings_quiet_hours_end_label)) },
            text = {
                TimePicker(state = endTimePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndTimePickerDialog = false
                        val hour = endTimePickerState.hour
                        val minute = endTimePickerState.minute
                        val text = "%02d:%02d".format(hour, minute)
                        onIntent(HugsSettingsIntent.ChangeQuietEndText(text))
                    }
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePickerDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}
