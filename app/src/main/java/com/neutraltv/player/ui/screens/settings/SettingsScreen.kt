package com.neutraltv.player.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.neutraltv.player.ui.theme.BlueDarkColors
import com.neutraltv.player.ui.theme.Error
import com.neutraltv.player.ui.theme.FocusBorder
import com.neutraltv.player.ui.theme.JotaPlayerColors
import com.neutraltv.player.ui.theme.JotaPlayerTypography
import com.neutraltv.player.ui.theme.LightColors
import com.neutraltv.player.ui.theme.OledBlackColors
import com.neutraltv.player.ui.theme.OnSurface
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary
import com.neutraltv.player.ui.theme.PurpleDarkColors
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
    val settingsState by viewModel.settingsState.collectAsState()

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 40.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = JotaPlayerTypography.headlineLarge,
            color = Primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Theme section
        Text(
            text = stringResource(R.string.settings_theme),
            style = JotaPlayerTypography.titleMedium,
            color = OnSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeCard(
                label = stringResource(R.string.theme_purple_dark),
                themeId = "purple_dark",
                colors = PurpleDarkColors,
                isSelected = settingsState.currentTheme == "purple_dark",
                onClick = { viewModel.selectTheme("purple_dark") }
            )
            ThemeCard(
                label = stringResource(R.string.theme_blue_dark),
                themeId = "blue_dark",
                colors = BlueDarkColors,
                isSelected = settingsState.currentTheme == "blue_dark",
                onClick = { viewModel.selectTheme("blue_dark") }
            )
            ThemeCard(
                label = stringResource(R.string.theme_oled_black),
                themeId = "oled_black",
                colors = OledBlackColors,
                isSelected = settingsState.currentTheme == "oled_black",
                onClick = { viewModel.selectTheme("oled_black") }
            )
            ThemeCard(
                label = stringResource(R.string.theme_light),
                themeId = "light",
                colors = LightColors,
                isSelected = settingsState.currentTheme == "light",
                onClick = { viewModel.selectTheme("light") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Font size section
        Text(
            text = stringResource(R.string.settings_font_size),
            style = JotaPlayerTypography.titleMedium,
            color = OnSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FontScaleOption(
                label = stringResource(R.string.font_size_normal),
                scale = 1.0f,
                isSelected = settingsState.currentFontScale == 1.0f,
                onClick = { viewModel.selectFontScale(1.0f) }
            )
            FontScaleOption(
                label = stringResource(R.string.font_size_large),
                scale = 1.2f,
                isSelected = settingsState.currentFontScale == 1.2f,
                onClick = { viewModel.selectFontScale(1.2f) }
            )
            FontScaleOption(
                label = stringResource(R.string.font_size_extra_large),
                scale = 1.4f,
                isSelected = settingsState.currentFontScale == 1.4f,
                onClick = { viewModel.selectFontScale(1.4f) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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

@Composable
private fun ThemeCard(
    label: String,
    themeId: String,
    colors: JotaPlayerColors,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(12.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceColor,
            focusedContainerColor = SurfaceVariant,
            pressedContainerColor = SurfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (isSelected) Border(
                border = BorderStroke(3.dp, Primary),
                shape = RoundedCornerShape(12.dp)
            ) else Border.None,
            focusedBorder = Border(
                border = BorderStroke(3.dp, FocusBorder),
                shape = RoundedCornerShape(12.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color preview swatches
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.background)
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.secondary)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = JotaPlayerTypography.labelMedium,
                color = if (isSelected) Primary else OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun FontScaleOption(
    label: String,
    scale: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.15f) else SurfaceColor,
            focusedContainerColor = SurfaceVariant,
            pressedContainerColor = SurfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (isSelected) Border(
                border = BorderStroke(2.dp, Primary),
                shape = RoundedCornerShape(8.dp)
            ) else Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Text(
            text = label,
            style = JotaPlayerTypography.labelLarge,
            color = if (isSelected) Primary else OnSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}
