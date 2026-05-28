package com.example.amulet.feature.devices.presentation.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Refresh
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.amulet.core.design.components.card.AmuletCard
import com.example.amulet.core.design.components.textfield.AmuletTextField
import com.example.amulet.core.design.scaffold.LocalScaffoldState
import com.example.amulet.feature.devices.R
import com.example.amulet.feature.devices.presentation.components.DeviceStatusChip
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DeviceDetailsRoute(
    deviceId: String,
    viewModel: DeviceDetailsViewModel = hiltViewModel(),
    onNavigateToOta: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is DeviceDetailsSideEffect.NavigateToOta -> {
                    onNavigateToOta()
                }
                is DeviceDetailsSideEffect.DeviceUnclaimedNavigateBack -> {
                    onNavigateBack()
                }
                is DeviceDetailsSideEffect.SettingsSavedNavigateBack -> {
                    // Toast переживает навигацию и виден уже на дашборде после возврата
                    Toast.makeText(
                        context,
                        context.getString(R.string.device_details_save_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    onNavigateBack()
                }
                is DeviceDetailsSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.device_details_save_error),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    DeviceDetailsScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::handleEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    state: DeviceDetailsState,
    snackbarHostState: SnackbarHostState,
    onEvent: (DeviceDetailsEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scaffoldState = LocalScaffoldState.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deviceName by remember(state.device?.id) { mutableStateOf(state.device?.name ?: "") }

    // Устанавливаем topBar и FAB
    SideEffect {
        scaffoldState.updateConfig {
            copy(
                topBar = {
                    TopAppBar(
                        title = { Text(state.device?.name ?: stringResource(R.string.device_details_default_title)) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                            }
                        },
                        actions = {
                            IconButton(onClick = { onEvent(DeviceDetailsEvent.Reconnect) }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.device_details_delete_button))
                            }
                        }
                    )
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                floatingActionButton = {
                    if (!state.isLoading && state.device != null) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                // Игнорируем повторные нажатия во время сохранения
                                if (!state.isSaving) {
                                    onEvent(DeviceDetailsEvent.SaveSettings(deviceName))
                                }
                            }
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = LocalContentColor.current
                                )
                            } else {
                                Text(stringResource(id = R.string.device_details_save_button))
                            }
                        }
                    }
                }
            )
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        state.device?.let { device ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                    // Основная информация
                    AmuletCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.device_details_info_title),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.device_details_firmware_label))
                                Text(device.firmwareVersion, style = MaterialTheme.typography.bodyMedium)
                            }

                            val batteryToShow: Int? = state.batteryLevel

                            batteryToShow?.let { level ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(R.string.device_details_battery_label))
                                    Text("${level}%", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.device_details_status_label))
                                val statusForChip = if (state.isDeviceOnline) {
                                    com.example.amulet.shared.domain.devices.model.DeviceStatus.ONLINE
                                } else {
                                    com.example.amulet.shared.domain.devices.model.DeviceStatus.OFFLINE
                                }
                                DeviceStatusChip(status = statusForChip)
                            }
                        }
                    }

                    // OTA обновление
                    state.firmwareUpdate?.takeIf { it.updateAvailable }?.let { update ->
                        AmuletCard(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.device_details_update_available),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.device_details_update_version, update.version),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onEvent(DeviceDetailsEvent.NavigateToOta) }
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.device_details_update_button))
                                }
                            }
                        }
                    }

                    // Настройки
                    AmuletCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.device_details_settings_title),
                                style = MaterialTheme.typography.titleMedium
                            )

                            // Имя устройства
                            AmuletTextField(
                                value = deviceName,
                                onValueChange = { deviceName = it },
                                label =stringResource(R.string.device_details_name_label),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = Icons.Default.Abc

                            )

                            // Яркость
                            Column {
                                Text(stringResource(R.string.device_details_brightness_label))
                                Slider(
                                    value = device.settings.brightness.toFloat(),
                                    onValueChange = { 
                                        onEvent(DeviceDetailsEvent.UpdateBrightness(it.toDouble()))
                                    },
                                    valueRange = 0f..1f
                                )
                            }

                            // Вибрация
                            Column {
                                Text(stringResource(R.string.device_details_haptics_label))
                                Slider(
                                    value = device.settings.haptics.toFloat(),
                                    onValueChange = { 
                                        onEvent(DeviceDetailsEvent.UpdateHaptics(it.toDouble()))
                                    },
                                    valueRange = 0f..1f
                                )
                            }
                        }
                    }
                }
            }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.device_details_unclaim_title)) },
            text = { Text(stringResource(R.string.device_details_unclaim_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(DeviceDetailsEvent.UnclaimDevice)
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.device_details_unclaim_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
