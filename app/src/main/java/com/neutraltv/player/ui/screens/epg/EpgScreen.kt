package com.neutraltv.player.ui.screens.epg

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.neutraltv.player.R
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.ProgramEntity
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.JuanPlayerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DP_PER_HOUR = 240f
private const val CHANNEL_COLUMN_WIDTH = 160
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun EpgScreen(
    onChannelSelected: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: EpgViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 0.dp)
    ) {
        // Title bar with refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.epg_title),
                style = JuanPlayerTheme.typography.headlineLarge,
                color = JuanPlayerTheme.colors.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isLoadingEpg) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = JuanPlayerTheme.colors.primary,
                        strokeWidth = 2.dp
                    )
                }
                Button(
                    onClick = { viewModel.forceRefreshEpg() },
                    colors = ButtonDefaults.colors(
                        containerColor = JuanPlayerTheme.colors.surface,
                        contentColor = JuanPlayerTheme.colors.onSurface,
                        focusedContainerColor = JuanPlayerTheme.colors.primary,
                        focusedContentColor = JuanPlayerTheme.colors.background
                    ),
                    enabled = !uiState.isLoadingEpg
                ) {
                    Text(
                        text = stringResource(R.string.epg_refresh),
                        style = JuanPlayerTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                LoadingIndicator(message = stringResource(R.string.loading))
            }
            uiState.channels.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.epg_no_data),
                        style = JuanPlayerTheme.typography.bodyLarge,
                        color = JuanPlayerTheme.colors.onSurfaceVariant
                    )
                }
            }
            else -> {
                EpgGrid(
                    channels = uiState.channels,
                    programs = uiState.programs,
                    windowStartTime = uiState.windowStartTime,
                    windowEndTime = uiState.windowEndTime,
                    onChannelSelected = onChannelSelected,
                    onProgramFocused = viewModel::onProgramFocused,
                    modifier = Modifier.weight(1f)
                )

                // Bottom info bar for focused program
                uiState.focusedProgram?.let { program ->
                    ProgramInfoBar(program = program)
                }
            }
        }
    }
}

@Composable
private fun EpgGrid(
    channels: List<ChannelEntity>,
    programs: Map<String, List<ProgramEntity>>,
    windowStartTime: Long,
    windowEndTime: Long,
    onChannelSelected: (Long) -> Unit,
    onProgramFocused: (ProgramEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val totalHours = (windowEndTime - windowStartTime).toFloat() / 3600_000f
    val totalWidth = (totalHours * DP_PER_HOUR).dp

    Column(modifier = modifier) {
        // Time header row (fixed at top, horizontally scrollable)
        Row {
            Spacer(modifier = Modifier.width(CHANNEL_COLUMN_WIDTH.dp))
            Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                TimeHeader(
                    windowStartTime = windowStartTime,
                    windowEndTime = windowEndTime,
                    totalWidth = totalWidth
                )
            }
        }

        // Virtualized channel + program rows
        TvLazyColumn(
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(
                items = channels,
                key = { it.id }
            ) { channel ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Channel name (fixed left)
                    Box(
                        modifier = Modifier
                            .width(CHANNEL_COLUMN_WIDTH.dp)
                            .height(56.dp)
                            .padding(vertical = 2.dp)
                            .background(JuanPlayerTheme.colors.surface, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = channel.name,
                            style = JuanPlayerTheme.typography.labelMedium,
                            color = JuanPlayerTheme.colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Program row (horizontally scrollable, synced)
                    Box(modifier = Modifier.weight(1f).horizontalScroll(horizontalScrollState)) {
                        val channelPrograms = programs[channel.epgChannelId] ?: emptyList()
                        ProgramRow(
                            channel = channel,
                            programs = channelPrograms,
                            windowStartTime = windowStartTime,
                            windowEndTime = windowEndTime,
                            totalWidth = totalWidth,
                            onChannelSelected = onChannelSelected,
                            onProgramFocused = onProgramFocused
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeHeader(
    windowStartTime: Long,
    windowEndTime: Long,
    totalWidth: Dp
) {
    val hourMs = 3600_000L
    val startHour = (windowStartTime / hourMs) * hourMs

    Row(
        modifier = Modifier
            .width(totalWidth)
            .height(32.dp)
    ) {
        var time = startHour
        while (time < windowEndTime) {
            val offsetMs = (time - windowStartTime).coerceAtLeast(0)
            val slotWidth = (DP_PER_HOUR).dp

            Box(
                modifier = Modifier
                    .width(slotWidth)
                    .height(32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = timeFormat.format(Date(time)),
                    style = JuanPlayerTheme.typography.labelSmall,
                    color = JuanPlayerTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            time += hourMs
        }
    }
}

@Composable
private fun ProgramRow(
    channel: ChannelEntity,
    programs: List<ProgramEntity>,
    windowStartTime: Long,
    windowEndTime: Long,
    totalWidth: Dp,
    onChannelSelected: (Long) -> Unit,
    onProgramFocused: (ProgramEntity?) -> Unit
) {
    val windowDuration = (windowEndTime - windowStartTime).toFloat()

    Row(
        modifier = Modifier
            .width(totalWidth)
            .height(56.dp)
            .padding(vertical = 2.dp)
    ) {
        if (programs.isEmpty()) {
            // Empty row
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(JuanPlayerTheme.colors.surface.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.epg_no_data),
                    style = JuanPlayerTheme.typography.labelSmall,
                    color = JuanPlayerTheme.colors.onSurfaceVariant
                )
            }
        } else {
            programs.forEach { program ->
                val progStart = program.startTime.coerceAtLeast(windowStartTime)
                val progEnd = program.endTime.coerceAtMost(windowEndTime)
                val widthFraction = (progEnd - progStart).toFloat() / windowDuration
                val blockWidth = (totalWidth.value * widthFraction).dp

                if (blockWidth > 2.dp) {
                    val now = System.currentTimeMillis()
                    val isCurrent = now in program.startTime..program.endTime

                    Surface(
                        onClick = { onChannelSelected(channel.id) },
                        modifier = Modifier
                            .width(blockWidth)
                            .height(52.dp),
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isCurrent) JuanPlayerTheme.colors.primary.copy(alpha = 0.3f) else JuanPlayerTheme.colors.surface,
                            focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
                            pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = program.title,
                                style = JuanPlayerTheme.typography.labelSmall,
                                color = if (isCurrent) JuanPlayerTheme.colors.focusBorder else JuanPlayerTheme.colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${timeFormat.format(Date(program.startTime))} - ${timeFormat.format(Date(program.endTime))}",
                                style = JuanPlayerTheme.typography.labelSmall,
                                color = JuanPlayerTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramInfoBar(program: ProgramEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JuanPlayerTheme.colors.surfaceVariant)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = program.title,
            style = JuanPlayerTheme.typography.titleMedium,
            color = JuanPlayerTheme.colors.onSurface
        )
        Row {
            Text(
                text = "${timeFormat.format(Date(program.startTime))} - ${timeFormat.format(Date(program.endTime))}",
                style = JuanPlayerTheme.typography.labelMedium,
                color = JuanPlayerTheme.colors.onSurfaceVariant
            )
            if (program.category != null) {
                Text(
                    text = " · ${program.category}",
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = JuanPlayerTheme.colors.onSurfaceVariant
                )
            }
        }
        if (program.description != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = program.description,
                style = JuanPlayerTheme.typography.bodySmall,
                color = JuanPlayerTheme.colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
