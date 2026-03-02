package com.neutraltv.player.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.neutraltv.player.R
import com.neutraltv.player.ui.theme.Error
import com.neutraltv.player.ui.theme.FocusBorder
import com.neutraltv.player.ui.theme.JotaPlayerTypography
import com.neutraltv.player.ui.theme.OnSurface
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary
import com.neutraltv.player.ui.theme.Surface as SurfaceColor
import com.neutraltv.player.ui.theme.SurfaceVariant
import com.neutraltv.player.ui.theme.Background
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPlaylistDeleted: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 40.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = JotaPlayerTypography.headlineLarge,
            color = Primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Manage playlists
        SettingsItem(
            title = stringResource(R.string.settings_manage_playlists),
            subtitle = stringResource(R.string.settings_manage_playlists_desc),
            onClick = onNavigateToPlaylists
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Delete playlist
        SettingsItem(
            title = stringResource(R.string.settings_delete_playlist),
            onClick = { showDeleteConfirm = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Clear cache
        SettingsItem(
            title = stringResource(R.string.settings_clear_cache),
            onClick = {
                scope.launch {
                    coil.Coil.imageLoader(context).memoryCache?.clear()
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // About
        SettingsItem(
            title = stringResource(R.string.settings_about),
            subtitle = stringResource(R.string.about_description),
            onClick = { }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Version
        Text(
            text = stringResource(R.string.settings_version, "1.0.0"),
            style = JotaPlayerTypography.labelMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp)
        )

        // Delete confirmation dialog
        if (showDeleteConfirm) {
            Spacer(modifier = Modifier.height(32.dp))
            Surface(
                onClick = { },
                shape = ClickableSurfaceDefaults.shape(
                    shape = RoundedCornerShape(12.dp)
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = SurfaceVariant,
                    focusedContainerColor = SurfaceVariant,
                    pressedContainerColor = SurfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_delete_confirm),
                        style = JotaPlayerTypography.bodyLarge,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.colors(
                                containerColor = SurfaceColor,
                                contentColor = OnSurface,
                                focusedContainerColor = FocusBorder,
                                focusedContentColor = Background
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.settings_cancel),
                                style = JotaPlayerTypography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                showDeleteConfirm = false
                                scope.launch {
                                    viewModel.deletePlaylist()
                                    onPlaylistDeleted()
                                }
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = Error,
                                contentColor = OnSurface,
                                focusedContainerColor = Error.copy(alpha = 0.8f),
                                focusedContentColor = OnSurface
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.settings_confirm),
                                style = JotaPlayerTypography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
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
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                style = JotaPlayerTypography.bodyLarge,
                color = OnSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = JotaPlayerTypography.labelMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
