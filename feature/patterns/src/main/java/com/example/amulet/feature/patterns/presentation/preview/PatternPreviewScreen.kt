package com.example.amulet.feature.patterns.presentation.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.amulet.core.design.scaffold.LocalScaffoldState
import com.example.amulet.feature.patterns.R
import com.example.amulet.feature.patterns.presentation.components.AmuletAvatar2D
import com.example.amulet.feature.patterns.presentation.components.DeviceConnectionCard
import com.example.amulet.feature.patterns.presentation.components.PatternInfoCard
import com.example.amulet.feature.patterns.presentation.components.PlaybackControls
import com.example.amulet.shared.domain.playback.PlaybackState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PatternPreviewRoute(
    viewModel: PatternPreviewViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEditor: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is PatternPreviewSideEffect.ShowSnackbar -> {
                    // Handle snackbar
                }
                is PatternPreviewSideEffect.ShowDeviceRequired -> {
                    // Handle device required dialog
                }
                is PatternPreviewSideEffect.ShowBleConnectionError -> {
                    // Handle BLE error
                }
                is PatternPreviewSideEffect.NavigateToDeviceSelection -> {
                    // Navigate to device selection
                }
            }
        }
    }

    PatternPreviewScreen(
        state = uiState,
        onEvent = viewModel::handleEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToEditor = onNavigateToEditor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternPreviewScreen(
    state: PatternPreviewState,
    onEvent: (PatternPreviewEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: () -> Unit = {}
) {
    val scaffoldState = LocalScaffoldState.current

    SideEffect {
        scaffoldState.updateConfig {
            copy(
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                text = state.pattern?.title ?: stringResource(R.string.pattern_preview_title)
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.Default.ArrowBack, 
                                    contentDescription = stringResource(R.string.cd_navigate_back)
                                )
                            }
                        },
                        actions = {
                            // Share button
                            IconButton(onClick = { /* TODO: Share pattern */ }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(R.string.pattern_preview_share)
                                )
                            }
                            // Edit button
                            IconButton(onClick = onNavigateToEditor) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.pattern_preview_edit)
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {}
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
        return
    }

    if (state.pattern == null && state.spec == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.error_pattern_load_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    // Определяем поведение зацикливания: если в паттерне/спеке loop=true, отображаем как включённое и блокируем переключатель
    val patternLoop = remember(state.pattern, state.spec) {
        state.pattern?.spec?.loop ?: state.spec?.loop ?: false
    }
    val visualIsLooping = patternLoop || state.isLooping
    val loopToggleEnabled = !patternLoop

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AmuletAvatar2D - анимированный 2D аватар амулета (без карточки)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            AmuletAvatar2D(
                spec = state.spec,
                isPlaying = state.isPlaying,
                modifier = Modifier.padding(16.dp),
                size = 200.dp
            )
        }

        // PlaybackControls - элементы управления воспроизведением
        PlaybackControls(
            isPlaying = state.isPlaying,
            isLooping = visualIsLooping,
            onPlayPause = { onEvent(PatternPreviewEvent.PlayPause) },
            onRestart = { onEvent(PatternPreviewEvent.Restart) },
            onLoopToggle = { onEvent(PatternPreviewEvent.UpdateLoop(it)) },
            loopEnabled = loopToggleEnabled
        )

        // DeviceConnectionCard - карточка подключенного устройства
        state.selectedDevice?.let { device ->
            DeviceConnectionCard(
                device = device,
                isSending = state.isSendingToDevice,
                onSendToDevice = { onEvent(PatternPreviewEvent.SendToDevice) },
                isConnectedOverride = state.isDeviceConnected,
                batteryOverride = state.batteryLevel
            )
        }

        // Прогресс отправки на устройство
        if (state.playbackState != PlaybackState.IDLE) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        when (state.playbackState) {
                            PlaybackState.COMPILING -> stringResource(R.string.loading_pattern)
                            PlaybackState.UPLOADING -> stringResource(R.string.sending_to_device)
                            PlaybackState.PLAYING -> stringResource(R.string.pattern_preview_play)
                            PlaybackState.ERROR -> stringResource(R.string.error_device_send_failed)
                            PlaybackState.IDLE -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (state.playbackState == PlaybackState.UPLOADING) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = ProgressIndicatorDefaults.linearColor,
                            trackColor = ProgressIndicatorDefaults.linearTrackColor,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        )
                    }
                }
            }
        }

        // PatternInfoCard - информация о паттерне
        if (state.pattern != null) {
            PatternInfoCard(pattern = state.pattern)
        }
    }
}
