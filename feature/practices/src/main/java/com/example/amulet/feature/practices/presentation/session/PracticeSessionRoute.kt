package com.example.amulet.feature.practices.presentation.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.amulet.core.design.foundation.theme.AmuletTheme
import com.example.amulet.core.design.scaffold.LocalScaffoldState
import com.example.amulet.feature.practices.R
import com.example.amulet.shared.domain.practices.model.PracticeSessionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSessionRoute(
    practiceId: String,
    onNavigateBack: () -> Unit,
    viewModel: PracticeSessionViewModel = hiltViewModel(),
    onOpenSchedule: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val scaffoldState = LocalScaffoldState.current

    SideEffect {
        val status = state.session?.status
        val isActive = status == PracticeSessionStatus.ACTIVE
        val isCompleted = status == PracticeSessionStatus.COMPLETED

        scaffoldState.updateConfig {
            copy(
                topBar = {
                    TopAppBar(
                        title = {
                            val titleText = state.title ?: stringResource(id = R.string.practice_session_default_title)
                            Text(titleText)
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                onNavigateBack()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    // Применяем тот же градиент, что и на основном экране
                    Surface(
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        color = Color.Transparent,
                        modifier = Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = if (isCompleted) Arrangement.spacedBy(8.dp) else Arrangement.Start,
                        ) {
                            if (isCompleted) {
                                Button(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmuletTheme.colors.success
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = stringResource(id = R.string.practice_session_back_home),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                Button(
                                    onClick = { viewModel.handleIntent(PracticeSessionIntent.Start) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = stringResource(id = R.string.practice_session_action_start))
                                }
                            } else {
                                val isLoadingDevice = state.isPatternPreloading
                                Button(
                                    onClick = {
                                        if (!isLoadingDevice) {
                                            if (isActive) {
                                                viewModel.handleIntent(PracticeSessionIntent.Stop(completed = false))
                                            } else {
                                                viewModel.handleIntent(PracticeSessionIntent.Start)
                                            }
                                        }
                                    },
                                    enabled = !isLoadingDevice,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    val label = when {
                                        isLoadingDevice -> stringResource(id = R.string.practice_session_action_loading_device)
                                        isActive -> stringResource(id = R.string.practice_session_action_finish)
                                        else -> stringResource(id = R.string.practice_session_action_start)
                                    }
                                    Text(text = label)
                                }
                            }
                        }
                    }
                },
            )
        }
    }

    LaunchedEffect(practiceId) {
        viewModel.setPracticeIdIfEmpty(practiceId)
    }

    PracticeSessionScreen(
        state = state,
        onIntent = viewModel::handleIntent,
        onNavigateBack = onNavigateBack,
    )
}
