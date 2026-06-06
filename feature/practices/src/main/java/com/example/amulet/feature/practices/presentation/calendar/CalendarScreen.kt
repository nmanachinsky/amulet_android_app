package com.example.amulet.feature.practices.presentation.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.amulet.core.design.scaffold.LocalScaffoldState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.amulet.feature.practices.R
import com.example.amulet.core.design.components.card.AmuletCard
import com.example.amulet.shared.domain.practices.model.ScheduledSession
import com.example.amulet.shared.domain.practices.model.ScheduledSessionStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit
) {
    val scaffoldState = LocalScaffoldState.current
    SideEffect {
        scaffoldState.updateConfig {
            copy(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.calendar_title)) },
                        navigationIcon = {
                            IconButton(onClick = { onIntent(CalendarIntent.NavigateBack) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.calendar_back)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                {onIntent(CalendarIntent.OpenPlannerGlobal) }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.calendar_back)
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {}
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tab selector
        ViewModeTabs(
            currentMode = state.viewMode,
            onModeChange = { onIntent(CalendarIntent.ChangeViewMode(it)) }
        )

        // Content based on view mode with crossfade animation
        Crossfade(targetState = state.viewMode, label = "schedule_view_mode") { mode ->
            when (mode) {
                ScheduleViewMode.CALENDAR -> {
                    CalendarViewContent(
                        state = state,
                        onIntent = onIntent
                    )
                }
                ScheduleViewMode.LIST -> {
                    ScheduleListView(
                        state = state,
                        onIntent = onIntent
                    )
                }
            }
        }
    }

    if (state.isPlannerOpen) {
        PlannerBottomSheet(
            state = state,
            onIntent = onIntent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerBottomSheet(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(CalendarIntent.ClosePlanner) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.plannerPracticeId == null) {
                Text(
                    text = stringResource(R.string.schedule_select_practice_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.plannerAvailablePractices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.schedule_no_practices_for_planning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.plannerAvailablePractices) { practice ->
                            AmuletCard(
                                modifier = Modifier
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onIntent(CalendarIntent.PlannerSelectPractice(practice.id))
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = practice.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )

                                    practice.durationSec?.let { durationSec ->
                                        val minutes = (durationSec / 60).coerceAtLeast(1)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.practices_home_duration_minutes, minutes),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.planning_setup_for_practice, state.plannerPracticeTitle),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (state.plannerPracticeId != null) {
                AmuletCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.planning_weekdays),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        WeekDaysSelector(
                            selectedDays = state.plannerSelectedDays,
                            onToggleDay = { onIntent(CalendarIntent.PlannerToggleDay(it)) }
                        )
                    }
                }
            }

            var showTimePicker by remember { mutableStateOf(false) }

            if (state.plannerPracticeId != null) {
                AmuletCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.planning_time),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTimePicker = true }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = state.plannerTimeOfDay,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }

            if (showTimePicker && state.plannerPracticeId != null) {
                TimePickerDialog(
                    initialTime = state.plannerTimeOfDay,
                    onConfirm = { time ->
                        onIntent(CalendarIntent.PlannerChangeTime(time))
                        showTimePicker = false
                    },
                    onDismiss = { showTimePicker = false }
                )
            }

            if (state.plannerPracticeId != null) {
                AmuletCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.planning_reminders),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.Switch(
                            checked = state.plannerReminderEnabled,
                            onCheckedChange = { onIntent(CalendarIntent.PlannerSetReminderEnabled(it)) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onIntent(CalendarIntent.ClosePlanner) },
                    enabled = !state.isPlannerSaving
                ) {
                    Text(text = stringResource(R.string.planning_cancel))
                }

                Button(
                    onClick = { onIntent(CalendarIntent.PlannerSave) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isPlannerSaving && state.plannerPracticeId != null && state.plannerSelectedDays.isNotEmpty()
                ) {
                    Text(stringResource(R.string.planning_save))
                }
            }
        }
    }
}

@Composable
private fun WeekDaysSelector(
    selectedDays: Set<Int>,
    onToggleDay: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val days = listOf(
            stringResource(R.string.weekday_mon),
            stringResource(R.string.weekday_tue),
            stringResource(R.string.weekday_wed),
            stringResource(R.string.weekday_thu),
            stringResource(R.string.weekday_fri),
            stringResource(R.string.weekday_sat),
            stringResource(R.string.weekday_sun)
        )
        days.forEachIndexed { index, dayName ->
            val dayNum = index + 1
            val isSelected = dayNum in selectedDays
            androidx.compose.material3.FilterChip(
                selected = isSelected,
                onClick = { onToggleDay(dayNum) },
                label = { Text(dayName.take(1)) },
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse initial time (HH:mm format)
    val (initialHour, initialMinute) = try {
        val parts = initialTime.split(":")
        Pair(parts[0].toInt(), parts[1].toInt())
    } catch (_: Exception) {
        Pair(9, 0) // Default to 09:00 if parsing fails
    }

    val timePickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.planning_time_select))
        },
        text = {
            androidx.compose.material3.TimePicker(
                state = timePickerState
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val formattedTime = String.format(
                        "%02d:%02d",
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onConfirm(formattedTime)
                }
            ) {
                Text(stringResource(R.string.planning_time_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.planning_time_cancel))
            }
        }
    )
}

@Composable
private fun CalendarViewContent(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit
) {
    var isCalendarExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CalendarHeader(
            currentMonth = state.currentMonth,
            isExpanded = isCalendarExpanded,
            onToggle = { isCalendarExpanded = !isCalendarExpanded },
            onPreviousMonth = { onIntent(CalendarIntent.ChangeMonth(-1)) },
            onNextMonth = { onIntent(CalendarIntent.ChangeMonth(1)) }
        )

        AnimatedVisibility(
            visible = isCalendarExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            CalendarGrid(
                currentMonth = state.currentMonth,
                selectedDate = state.selectedDate,
                sessions = state.sessions,
                onDateSelected = { onIntent(CalendarIntent.SelectDate(it)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(
                R.string.calendar_plans_for_date,
                state.selectedDate.day.toString(),
                state.selectedDate.month.getDisplayName()
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SessionList(
            selectedDate = state.selectedDate,
            sessions = state.sessions,
            onIntent = onIntent
        )
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    isExpanded: Boolean = true,
    onToggle: () -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "calendar_chevron"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Пред. месяц")
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${currentMonth.month.getDisplayName(full = true).replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronRotation)
            )
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "След. месяц")
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    sessions: List<ScheduledSession>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysOfWeek = remember { DayOfWeek.entries.toTypedArray() }
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day.getDisplayName(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Days grid
        val startOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
        val endOfMonth = startOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        
        // Calculate offset for the first day (Monday = 1)
        val firstDayOffset = startOfMonth.dayOfWeek.isoDayNumber - 1
        val totalDays = endOfMonth.day + firstDayOffset
        val rows = (totalDays + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col
                    val dayOfMonth = dayIndex - firstDayOffset + 1
                    
                    if (dayOfMonth in 1..endOfMonth.day) {
                        val date = LocalDate(currentMonth.year, currentMonth.month, dayOfMonth)
                        val isSelected = date == selectedDate
                        val hasSession = sessions.any { 
                             val sessionDate = Instant.fromEpochMilliseconds(it.scheduledTime)
                                 .toLocalDateTime(TimeZone.currentSystemDefault()).date
                             sessionDate == date
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayOfMonth.toString(),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasSession) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun SessionList(
    selectedDate: LocalDate,
    sessions: List<ScheduledSession>,
    onIntent: (CalendarIntent) -> Unit
) {
    val selectedSessions = remember(selectedDate, sessions) {
        sessions.filter {
            val sessionDate = kotlinx.datetime.Instant.fromEpochMilliseconds(it.scheduledTime)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            sessionDate == selectedDate
        }
    }

    if (selectedSessions.isEmpty()) {
        EmptySchedulePlaceholder(onIntent = onIntent)
    } else {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement =Arrangement.spacedBy(8.dp)
        ) {
            items(selectedSessions) { session ->
                SessionItem(session = session, onIntent = onIntent)
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun TimelineSessionItem(
    session: ScheduledSession,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onIntent: (CalendarIntent) -> Unit
) {
    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val time = timeFormatter.format(java.util.Date(session.scheduledTime))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Левая колонка таймлайна
        Box(
            modifier = Modifier
                .width(56.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(
                            if (isFirstInGroup) Color.Transparent else MaterialTheme.colorScheme.primary
                        )
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(
                            if (isLastInGroup) Color.Transparent else MaterialTheme.colorScheme.primary
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Правая колонка — карточка сессии без времени в заголовке
        Box(modifier = Modifier.weight(1f)) {
            SessionItem(session = session, onIntent = onIntent, showTime = false)
        }
    }
}

@Composable
private fun EmptySchedulePlaceholder(onIntent: (CalendarIntent) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.schedule_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.schedule_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onIntent(CalendarIntent.OpenPlannerGlobal) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.schedule_add_plan_global))
            }
        }
    }
}

@OptIn(ExperimentalTime::class, ExperimentalLayoutApi::class)
@Composable
fun SessionItem(
    session: ScheduledSession,
    onIntent: (CalendarIntent) -> Unit,
    showTime: Boolean = true
) {
    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val time = timeFormatter.format(java.util.Date(session.scheduledTime))

    var showCancelDialog by remember { mutableStateOf(false) }
    
    // Status color
    val statusColor = when (session.status) {
        ScheduledSessionStatus.PLANNED -> MaterialTheme.colorScheme.primary
        ScheduledSessionStatus.MISSED -> MaterialTheme.colorScheme.error
        ScheduledSessionStatus.COMPLETED -> Color(0xFF4CAF50) // Green
    }

    AmuletCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onIntent(CalendarIntent.OpenSession(session.id)) },
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        elevation = com.example.amulet.core.design.components.card.CardElevation.None
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Status indicator - левая цветная полоса
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(IntrinsicSize.Min)
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with time and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.practiceTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        if (showTime) {
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        {onIntent(CalendarIntent.StartSession(session.id))}
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    }
                }
                
                // Metadata chips - duration and course indicator
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    session.durationSec?.let { duration ->
                        MetadataChip(
                            icon = Icons.Default.AccessTime,
                            text = "${duration / 60} мин"
                        )
                    }
                    
                    session.courseId?.let { _ ->
                        val label = session.courseTitle ?: "Курс"
                        MetadataChip(
                            icon = Icons.Default.CalendarToday,
                            text = label
                        )
                    }

                    SessionStatusChip(status = session.status)
                }
                
                // Quick actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (session.status == ScheduledSessionStatus.MISSED) {
                        Button(
                            onClick = { onIntent(CalendarIntent.StartSession(session.id)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.schedule_overdue_action_do_now))
                        }
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.schedule_overdue_action_skip))
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onIntent(CalendarIntent.OpenPlanner(session.practiceId)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.session_action_reschedule))
                        }
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.session_action_cancel))
                        }
                    }
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.session_cancel_title)) },
            text = { Text(stringResource(R.string.session_cancel_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(CalendarIntent.CancelSession(session.id))
                    }
                ) {
                    Text(stringResource(R.string.session_cancel_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.common_back))
                }
            }
        )
    }
}

// Metadata Chip Component
@Composable
private fun MetadataChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// Helper functions for display names
private fun Month.getDisplayName(full: Boolean = true): String {
    val style = if (full) TextStyle.FULL else TextStyle.SHORT
    return java.time.Month.of(this.ordinal + 1).getDisplayName(style, Locale("ru"))
}

private fun DayOfWeek.getDisplayName(): String {
    return java.time.DayOfWeek.of(this.isoDayNumber).getDisplayName(TextStyle.SHORT, Locale("ru"))
}

// View Mode Tabs Component
@Composable
private fun ViewModeTabs(
    currentMode: ScheduleViewMode,
    onModeChange: (ScheduleViewMode) -> Unit
) {
    val tabs = listOf(
        ScheduleViewMode.CALENDAR to stringResource(R.string.schedule_view_calendar),
        ScheduleViewMode.LIST to stringResource(R.string.schedule_view_list)
    )

    TabRow(
        selectedTabIndex = if (currentMode == ScheduleViewMode.CALENDAR) 0 else 1
    ) {
        tabs.forEachIndexed { index, (mode, title) ->
            Tab(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                text = { Text(title) },
                modifier = Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
        }
    }
}

// Schedule List View Component
@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
private fun ScheduleListView(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit
) {
    val timeZone = TimeZone.currentSystemDefault()
    val today = kotlin.time.Clock.System.now().toLocalDateTime(timeZone).date
    val endDate = today.plus(7, DateTimeUnit.DAY)

    // Сессии на ближайшие 7 дней
    val upcomingSessions = remember(state.sessions, today) {
        state.sessions.filter {
            val sessionDate = kotlinx.datetime.Instant.fromEpochMilliseconds(it.scheduledTime)
                .toLocalDateTime(timeZone).date
            sessionDate in today..endDate
        }.sortedBy { it.scheduledTime }
    }

    // Группировка ближайших сессий по датам
    val sessionsByDate = remember(upcomingSessions) {
        upcomingSessions.groupBy {
            Instant.fromEpochMilliseconds(it.scheduledTime)
                .toLocalDateTime(timeZone).date
        }
    }

    if (sessionsByDate.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptySchedulePlaceholder(onIntent = onIntent)
        }
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        sessionsByDate.forEach { (date, sessions) ->
            item {
                AmuletCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DateGroupHeader(date = date, today = today)
                        sessions.forEachIndexed { index, session ->
                            TimelineSessionItem(
                                session = session,
                                isFirstInGroup = index == 0,
                                isLastInGroup = index == sessions.lastIndex,
                                onIntent = onIntent
                            )
                        }
                    }
                }
            }
        }
    }
}



@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
private fun OverdueSessionItem(
    session: ScheduledSession,
    onIntent: (CalendarIntent) -> Unit
) {
    val timeZone = TimeZone.currentSystemDefault()
    val sessionTime = remember(session.scheduledTime) {
        kotlinx.datetime.Instant.fromEpochMilliseconds(session.scheduledTime)
            .toLocalDateTime(timeZone)
    }
    val time = remember(sessionTime) {
        val localTime = java.time.LocalTime.of(sessionTime.hour, sessionTime.minute)
        localTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }

    var showSkipDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.practiceTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

            }
            SessionStatusChip(status = session.status)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { onIntent(CalendarIntent.StartSession(session.id)) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.schedule_overdue_action_do_now))
            }
            OutlinedButton(
                onClick = { showSkipDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.schedule_overdue_action_skip))
            }
        }
    }

    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text(stringResource(R.string.session_cancel_title)) },
            text = { Text(stringResource(R.string.session_cancel_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSkipDialog = false
                        onIntent(CalendarIntent.CancelSession(session.id))
                    }
                ) {
                    Text(stringResource(R.string.schedule_overdue_action_skip))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text(stringResource(R.string.common_back))
                }
            }
        )
    }
}

@Composable
private fun DateGroupHeader(date: LocalDate, today: LocalDate) {
    val tomorrow = today.plus(1, DateTimeUnit.DAY)
    val rawHeaderText = when (date) {
        today -> stringResource(R.string.schedule_today)
        tomorrow -> stringResource(R.string.schedule_tomorrow)
        else -> {
            val dayName = java.time.LocalDate.of(date.year, date.month.number, date.day)
                .dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))
            "$dayName, ${date.day} ${date.month.getDisplayName()}"
        }
    }

    val headerText = rawHeaderText.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
    }

    Text(
        text = headerText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SessionStatusChip(status: ScheduledSessionStatus) {
    val (label, borderColor, contentColor) = when (status) {
        ScheduledSessionStatus.PLANNED -> Triple(
            stringResource(id = R.string.session_status_planned),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        ScheduledSessionStatus.MISSED -> Triple(
            stringResource(id = R.string.session_status_missed),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        ScheduledSessionStatus.COMPLETED -> Triple(
            stringResource(id = R.string.session_status_completed),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(borderColor))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

