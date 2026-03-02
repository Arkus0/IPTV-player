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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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
import com.neutraltv.player.ui.theme.FocusBorder
import com.neutraltv.player.ui.theme.JotaPlayerTypography
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary
import com.neutraltv.player.ui.theme.Surface as SurfaceColor
import com.neutraltv.player.ui.theme.SurfaceVariant

@Composable
fun ChannelListScreen(
    onChannelSelected: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ChannelListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBack() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, top = 24.dp, bottom = 24.dp)
    ) {
        // Groups sidebar
        if (uiState.groups.size > 1) {
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
            Text(
                text = uiState.selectedGroup ?: stringResource(R.string.tv_live),
                style = JotaPlayerTypography.titleLarge,
                color = Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.channel_count, uiState.channels.size),
                style = JotaPlayerTypography.labelMedium,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                            onClick = { onChannelSelected(channel.id) }
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
            containerColor = if (isSelected) SurfaceVariant else SurfaceColor,
            focusedContainerColor = SurfaceVariant,
            pressedContainerColor = SurfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Text(
            text = name,
            style = JotaPlayerTypography.bodyMedium,
            color = if (isSelected) Primary else OnSurfaceVariant,
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceColor,
            focusedContainerColor = SurfaceVariant,
            pressedContainerColor = SurfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
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
                style = JotaPlayerTypography.labelMedium,
                color = OnSurfaceVariant,
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

            // Channel name and group
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = JotaPlayerTypography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (groupTitle != null) {
                    Text(
                        text = groupTitle,
                        style = JotaPlayerTypography.labelMedium,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
