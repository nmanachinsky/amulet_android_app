package com.example.amulet.feature.hugs.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.amulet.core.design.components.avatar.AmuletAvatar
import com.example.amulet.core.design.components.avatar.AvatarSize
import com.example.amulet.core.design.components.card.AmuletCard
import com.example.amulet.core.design.scaffold.LocalScaffoldState
import com.example.amulet.feature.hugs.R
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.domain.hugs.model.Hug
import com.example.amulet.shared.domain.hugs.model.GestureType
import com.example.amulet.shared.domain.hugs.model.HugStatus
import com.example.amulet.shared.domain.hugs.model.PairEmotion
import com.example.amulet.shared.domain.hugs.model.PairStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HugsRoute(
    onOpenSettings: () -> Unit = {},
    onOpenEmotions: () -> Unit = {},
    onOpenPairing: () -> Unit = {},
    viewModel: HugsHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scaffoldState = LocalScaffoldState.current

    LaunchedEffect(Unit) {
        viewModel.onIntent(HugsHomeIntent.OnEnter)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HugsHomeEffect.NavigateToSettings -> onOpenSettings()
                HugsHomeEffect.NavigateToEmotions -> onOpenEmotions()
                HugsHomeEffect.NavigateToPairing -> onOpenPairing()
                is HugsHomeEffect.ShowError -> {
                    // TODO: показать ошибку через Snackbar, когда появится инфраструктура
                }
            }
        }
    }

    SideEffect {
        scaffoldState.updateConfig {
            copy(
                topBar = {
                    TopAppBar(
                        title = { Text(text = stringResource(R.string.hugs_home_title), style = MaterialTheme.typography.titleLarge) },
                        actions = {
                            IconButton(onClick = { viewModel.onIntent(HugsHomeIntent.OpenSettings) }) {
                                Icon(imageVector = Icons.Filled.Settings, contentDescription = null)
                            }
                        }
                    )
                },
                floatingActionButton = {}
            )
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing || state.isLoading,
            onRefresh = { viewModel.onIntent(HugsHomeIntent.Refresh) },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            HugsHomeScreen(
                state = state,
                onIntent = viewModel::onIntent,
            )
        }

        if (state.isSyncingOnEnter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (state.isPairEmotionPickerOpen) {
        val mode = state.pairEmotionPickerMode
        val title = when (mode) {
            PairEmotionPickerMode.SendHug -> stringResource(R.string.hugs_home_emotion_picker_title)
            PairEmotionPickerMode.SetDefault -> stringResource(R.string.hugs_home_default_emotion_card_title)
            is PairEmotionPickerMode.SetQuickReply -> stringResource(R.string.hugs_home_quick_reply_picker_title)
        }
        val initialEmotionId = when (mode) {
            PairEmotionPickerMode.SendHug,
            PairEmotionPickerMode.SetDefault -> state.defaultHugEmotionId
            is PairEmotionPickerMode.SetQuickReply -> state.quickReplies
                .firstOrNull { it.gestureType == mode.gestureType }
                ?.emotionId
        }
        val allowNotSet = mode != PairEmotionPickerMode.SendHug

        PairEmotionPickerBottomSheet(
            emotions = state.pairEmotions,
            title = title,
            initialEmotionId = initialEmotionId,
            allowNotSet = allowNotSet,
            onDismiss = { viewModel.onIntent(HugsHomeIntent.ClosePairEmotionPicker) },
            onSelect = { emotionId ->
                viewModel.onIntent(HugsHomeIntent.SelectPairEmotion(emotionId))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultEmotionCard(
    state: HugsHomeState,
    onIntent: (HugsHomeIntent) -> Unit,
) {
    val selectedEmotion = state.defaultHugEmotionId
        ?.let { id -> state.pairEmotions.firstOrNull { it.id == id } }

    val defaultColor = selectedEmotion
        ?.colorHex
        ?.takeIf { it.isNotBlank() }
        ?.let { hex -> runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { null } }

    val containerColor = if (defaultColor != null) {
        defaultColor.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (defaultColor != null && defaultColor.luminance() < 0.4f) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        onClick = { onIntent(HugsHomeIntent.OpenDefaultEmotionPicker) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.hugs_home_default_emotion_card_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (selectedEmotion == null) {
                    stringResource(R.string.hugs_home_default_emotion_card_not_set)
                } else {
                    selectedEmotion.name
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickReplyCard(
    state: HugsHomeState,
    onIntent: (HugsHomeIntent) -> Unit,
) {
    AmuletCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.hugs_home_quick_reply_card_title),
                style = MaterialTheme.typography.titleMedium,
            )

            QuickReplyRow(
                title = stringResource(R.string.hugs_home_quick_reply_double_tap),
                emotionName = resolveQuickReplyEmotionName(state, GestureType.DOUBLE_TAP),
                onClick = { onIntent(HugsHomeIntent.OpenQuickReplyPicker(GestureType.DOUBLE_TAP)) },
            )
        }
    }
}

@Composable
private fun QuickReplyRow(
    title: String,
    emotionName: String,
    onClick: () -> Unit,
) {
    AmuletCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        elevation = com.example.amulet.core.design.components.card.CardElevation.Low,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = emotionName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun resolveQuickReplyEmotionName(state: HugsHomeState, gestureType: GestureType): String {
    val replyEmotionId = state.quickReplies.firstOrNull { it.gestureType == gestureType }?.emotionId
        ?: return "-"
    return state.pairEmotions.firstOrNull { it.id == replyEmotionId }?.name ?: "-"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairEmotionPickerBottomSheet(
    emotions: List<PairEmotion>,
    title: String,
    initialEmotionId: String?,
    allowNotSet: Boolean,
    onDismiss: () -> Unit,
    onSelect: (emotionId: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var query by remember { mutableStateOf("") }
    val filtered = remember(emotions, query) {
        val q = query.trim()
        if (q.isBlank()) {
            emotions
        } else {
            emotions.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider()

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(text = stringResource(R.string.hugs_home_emotion_picker_search_placeholder)) },
            )

            if (allowNotSet) {
                val notSetSelected = initialEmotionId == null
                AmuletCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelect(null) },
                    backgroundColor = if (notSetSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (notSetSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    elevation = com.example.amulet.core.design.components.card.CardElevation.Low,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.hugs_home_quick_reply_not_set),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (notSetSelected) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.hugs_home_emotion_picker_no_patterns),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { emotion ->
                        val selected = initialEmotionId == emotion.id
                        val color = runCatching { Color(android.graphics.Color.parseColor(emotion.colorHex)) }
                            .getOrElse { MaterialTheme.colorScheme.primary }

                        val containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                        val contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        AmuletCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onSelect(emotion.id) },
                            backgroundColor = containerColor,
                            contentColor = contentColor,
                            elevation = com.example.amulet.core.design.components.card.CardElevation.Low,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = emotion.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (selected) {
                                    Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HugsHomeScreen(
    state: HugsHomeState,
    onIntent: (HugsHomeIntent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        state.error?.let { error ->
            item {
                AmuletCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.hugs_home_sync_error_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = when (error) {
                                    is AppError.Timeout -> stringResource(R.string.hugs_home_sync_timeout_message)
                                    else -> stringResource(R.string.hugs_home_sync_error_message)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = { onIntent(HugsHomeIntent.Refresh) }) {
                            Text(text = stringResource(R.string.hugs_home_sync_retry))
                        }
                    }
                }
            }
        }

        item {
            PairHeaderSection(state = state, onIntent = onIntent)
        }

        if (state.activePair != null) {
            item {
                DefaultEmotionCard(state = state, onIntent = onIntent)
            }

            item {
                QuickReplyCard(state = state, onIntent = onIntent)
            }
        }

        item {
            QuickActionsSection(onIntent = onIntent)
        }

        if (state.hugs.isNotEmpty()) {
            item {
                RecentHugsSection(hugs = state.hugs)
            }
        }

        // Карточка управления заблокированной парой
        if (state.activePair?.status == PairStatus.BLOCKED) {
            item {
                AmuletCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hugs_home_blocked_pair_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.hugs_home_blocked_pair_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { onIntent(HugsHomeIntent.UnblockPair) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.hugs_home_unblock_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairHeaderSection(
    state: HugsHomeState,
    onIntent: (HugsHomeIntent) -> Unit,
) {
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
                    text = stringResource(R.string.hugs_home_pair_section_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            val pair = state.activePair
            val hasPair = pair != null

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PairMemberCard(
                    title = state.currentUser?.displayName ?: stringResource(R.string.hugs_home_you),
                    subtitle = stringResource(R.string.hugs_home_you),
                    highlight = true,
                    avatarUrl = state.currentUser?.avatarUrl,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                val partnerSubtitle = when (pair?.status) {
                    PairStatus.ACTIVE -> stringResource(R.string.hugs_home_partner_active)
                    PairStatus.PENDING -> stringResource(R.string.hugs_home_partner_pending)
                    PairStatus.BLOCKED -> stringResource(R.string.hugs_home_partner_blocked)
                    null -> stringResource(R.string.hugs_home_partner_not_configured)
                }

                PairMemberCard(
                    title = state.partnerUser?.displayName
                        ?: if (hasPair) stringResource(R.string.hugs_home_partner_title) else stringResource(R.string.hugs_home_no_partner_title),
                    subtitle = partnerSubtitle,
                    highlight = hasPair,
                    avatarUrl = state.partnerUser?.avatarUrl,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!hasPair) {
                Button(
                    onClick = { onIntent(HugsHomeIntent.OpenPairing) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.hugs_home_setup_pair_button))
                }
            }

            val hasPartner = state.partnerUser?.id != null || pair?.members?.any { member ->
                member.userId != state.currentUser?.id
            } == true

            val canSendHug =
                hasPair &&
                    hasPartner &&
                    state.activePair?.status == PairStatus.ACTIVE &&
                    !state.isSending &&
                    !state.isSyncingOnEnter

            Button(
                onClick = { onIntent(HugsHomeIntent.SendHug) },
                enabled = canSendHug,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (state.isSending) stringResource(R.string.hugs_home_send_in_progress) else stringResource(R.string.hugs_home_send_button),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onIntent: (HugsHomeIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionCard(
                title = stringResource(R.string.hugs_home_quick_actions_emotions_title),
                description = stringResource(R.string.hugs_home_quick_actions_emotions_desc),
                icon = Icons.Filled.Favorite,
                onClick = { onIntent(HugsHomeIntent.OpenEmotions) },
                modifier = Modifier.weight(1f)
            )
        }

    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AmuletCard(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentHugsSection(hugs: List<Hug>) {
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
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.hugs_home_recent_hugs_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (hugs.isEmpty()) {
                Text(
                    text = stringResource(R.string.hugs_home_recent_hugs_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    hugs.forEach { hug ->
                        RecentHugItem(hug = hug)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentHugItem(hug: Hug) {
    val emotionColor = remember(hug.emotion.colorHex) {
        parseEmotionColor(hug.emotion.colorHex)
    }
    val formattedDateTime = remember(hug.createdAt) {
        formatHugDateTime(hug)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(emotionColor)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = when (hug.status) {
                    HugStatus.SENT -> stringResource(R.string.hugs_home_status_sent)
                    HugStatus.DELIVERED -> stringResource(R.string.hugs_home_status_delivered)
                    HugStatus.READ -> stringResource(R.string.hugs_home_status_read)
                    HugStatus.EXPIRED -> stringResource(R.string.hugs_home_status_expired)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formattedDateTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PairMemberCard(
    title: String,
    subtitle: String,
    highlight: Boolean,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (avatarUrl != null && avatarUrl.isNotBlank()) {
            AmuletAvatar(
                imageUrl = avatarUrl,
                initials = title.trim().takeIf { it.isNotBlank() },
                size = if (highlight) AvatarSize.ExtraLarge else AvatarSize.Medium,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(if (highlight) 72.dp else 56.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun parseEmotionColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color.Gray
    return try {
        val clean = hex.removePrefix("#")
        val r = clean.take(2).toInt(16)
        val g = clean.substring(2, 4).toInt(16)
        val b = clean.substring(4, 6).toInt(16)
        Color(r, g, b)
    } catch (_: Exception) {
        Color.Gray
    }
}

private fun formatHugDateTime(hug: Hug): String {
    val dt = hug.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = dt.date.day.toString().padStart(2, '0')
    val month = (dt.date.month.ordinal + 1).toString().padStart(2, '0')
    val hour = dt.hour.toString().padStart(2, '0')
    val minute = dt.minute.toString().padStart(2, '0')
    return "$day.$month $hour:$minute"
}
