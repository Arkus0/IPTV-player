package com.neutraltv.player.ui.screens.channels

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.neutraltv.player.R
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.JuanPlayerTheme

@Composable
fun ChannelListScreen(
    onChannelSelected: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ChannelListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    BackHandler {
        if (uiState.isSearchActive) {
            viewModel.toggleSearch()
        } else {
            onBack()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, top = 24.dp, bottom = 24.dp)
    ) {
        // Groups sidebar (hidden during search)
        if (uiState.groups.size > 1 && !uiState.isSearchActive) {
            GroupsSidebar(
                groups = uiState.groups,
                selectedGroup = uiState.selectedGroup,
                onGroupSelected = { viewModel.selectGroup(it) },
                onAllSelected = { viewModel.selectAllChannels() },
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // Channel list
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 24.dp)
        ) {
            // Search bar
            if (uiState.isSearchActive) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    label = {
                        Text(
                            text = stringResource(R.string.search_hint),
                            color = JuanPlayerTheme.colors.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = JuanPlayerTheme.typography.bodyLarge.copy(color = JuanPlayerTheme.colors.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JuanPlayerTheme.colors.focusBorder,
                        unfocusedBorderColor = JuanPlayerTheme.colors.onSurfaceVariant,
                        cursorColor = JuanPlayerTheme.colors.primary,
                        focusedLabelColor = JuanPlayerTheme.colors.focusBorder,
                        unfocusedLabelColor = JuanPlayerTheme.colors.onSurfaceVariant,
                        focusedContainerColor = JuanPlayerTheme.colors.background,
                        unfocusedContainerColor = JuanPlayerTheme.colors.background
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (uiState.isSearchActive && uiState.searchQuery.isNotBlank())
                            stringResource(R.string.channel_count, uiState.channels.size)
                        else
                            uiState.selectedGroup ?: stringResource(R.string.tv_live),
                        style = JuanPlayerTheme.typography.titleLarge,
                        color = JuanPlayerTheme.colors.primary
                    )
                    if (!uiState.isSearchActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.channel_count, uiState.channels.size),
                            style = JuanPlayerTheme.typography.labelMedium,
                            color = JuanPlayerTheme.colors.onSurfaceVariant
                        )
                    }
                }
                // Search toggle button and hidden toggle button
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Show/hide hidden channels toggle
                    Surface(
                        onClick = { viewModel.toggleShowHidden() },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (uiState.showHiddenChannels) JuanPlayerTheme.colors.surfaceVariant else JuanPlayerTheme.colors.surface,
                            focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
                            pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, JuanPlayerTheme.colors.focusBorder),
                                shape = RoundedCornerShape(8.dp)
                            )
                        )
                    ) {
                        Text(
                            text = if (uiState.showHiddenChannels) "\uD83D\uDC41" else "\uD83D\uDC41\u200D\uD83D\uDDE8",
                            style = JuanPlayerTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    // Search toggle button
                    Surface(
                        onClick = { viewModel.toggleSearch() },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (uiState.isSearchActive) JuanPlayerTheme.colors.surfaceVariant else JuanPlayerTheme.colors.surface,
                            focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
                            pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, JuanPlayerTheme.colors.focusBorder),
                                shape = RoundedCornerShape(8.dp)
                            )
                        )
                    ) {
                        Text(
                            text = if (uiState.isSearchActive) "\u2716" else "\uD83D\uDD0D",
                            style = JuanPlayerTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                LoadingIndicator(message = stringResource(R.string.loading))
            } else {
                TvLazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.channels,
                        key = { it.id }
                    ) { channel ->
                        ChannelItem(
                            name = channel.name,
                            number = channel.position + 1,
                            logoUrl = channel.logoUrl,
                            groupTitle = channel.groupTitle,
                            isFavorite = channel.id in uiState.favoriteIds,
                            isHidden = channel.isHidden,
                            showHiddenMode = uiState.showHiddenChannels,
                            currentProgram = channel.epgChannelId?.let { uiState.currentPrograms[it] },
                            onClick = { onChannelSelected(channel.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
                            onToggleHidden = { viewModel.toggleChannelHidden(channel.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupsSidebar(
    groups: List<String?>,
    selectedGroup: String?,
    onGroupSelected: (String?) -> Unit,
    onAllSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvLazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            GroupItem(
                name = "Todos",
                isSelected = selectedGroup == null,
                onClick = onAllSelected
            )
        }
        items(
            items = groups,
            key = { it ?: "__null__" }
        ) { group ->
            GroupItem(
                name = group ?: "Sin Categoría",
                isSelected = selectedGroup == group,
                onClick = { onGroupSelected(group) }
            )
        }
    }
}

@Composable
private fun GroupItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) JuanPlayerTheme.colors.surfaceVariant else JuanPlayerTheme.colors.surface,
            focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
            pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, JuanPlayerTheme.colors.focusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Text(
            text = name,
            style = JuanPlayerTheme.typography.bodyMedium,
            color = if (isSelected) JuanPlayerTheme.colors.primary else JuanPlayerTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChannelItem(
    name: String,
    number: Int,
    logoUrl: String?,
    groupTitle: String?,
    isFavorite: Boolean = false,
    isHidden: Boolean = false,
    showHiddenMode: Boolean = false,
    currentProgram: String? = null,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onToggleHidden: () -> Unit = {}
) {
    val itemAlpha = if (isHidden && showHiddenMode) 0.4f else 1f
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(itemAlpha),
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = JuanPlayerTheme.colors.surface,
            focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
            pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, JuanPlayerTheme.colors.focusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel number
            Text(
                text = number.toString().padStart(3, ' '),
                style = JuanPlayerTheme.typography.labelMedium,
                color = JuanPlayerTheme.colors.onSurfaceVariant,
                modifier = Modifier.width(48.dp)
            )

            // Channel logo
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Channel name, group, and current program
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = JuanPlayerTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentProgram != null) {
                    Text(
                        text = "${stringResource(R.string.epg_now)}: $currentProgram",
                        style = JuanPlayerTheme.typography.labelMedium,
                        color = JuanPlayerTheme.colors.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (groupTitle != null) {
                    Text(
                        text = groupTitle,
                        style = JuanPlayerTheme.typography.labelMedium,
                        color = JuanPlayerTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Favorite toggle button
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onToggleFavorite,
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = JuanPlayerTheme.colors.surface,
                    focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
                    pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
                ),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, JuanPlayerTheme.colors.focusBorder),
                        shape = CircleShape
                    )
                )
            ) {
                Text(
                    text = if (isFavorite) "\u2605" else "\u2606",
                    style = JuanPlayerTheme.typography.titleMedium,
                    color = if (isFavorite) JuanPlayerTheme.colors.primary else JuanPlayerTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Hide/show toggle button
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                onClick = onToggleHidden,
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = JuanPlayerTheme.colors.surface,
                    focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
                    pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
                ),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, JuanPlayerTheme.colors.focusBorder),
                        shape = CircleShape
                    )
                )
            ) {
                Text(
                    text = if (isHidden) "\uD83D\uDC41\u200D\uD83D\uDDE8" else "\uD83D\uDEAB",
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = if (isHidden) JuanPlayerTheme.colors.onSurfaceVariant else JuanPlayerTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
