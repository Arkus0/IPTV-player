package com.neutraltv.player.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.neutraltv.player.ui.theme.AppColors
import com.neutraltv.player.ui.theme.JuanPlayerTheme
import com.neutraltv.player.ui.theme.LightColors
import com.neutraltv.player.ui.theme.OledBlackColors
import com.neutraltv.player.ui.theme.PurpleDarkColors
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

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = viewModel.exportBackup()
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    Toast.makeText(context, context.getString(R.string.settings_export_success), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.settings_import_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(it)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: return@launch
                    val result = viewModel.importBackup(json)
                    if (result.isSuccess) {
                        Toast.makeText(context, context.getString(R.string.settings_import_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.settings_import_error), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.settings_import_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 40.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = JuanPlayerTheme.typography.headlineLarge,
            color = JuanPlayerTheme.colors.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Theme section
        Text(
            text = stringResource(R.string.settings_theme),
            style = JuanPlayerTheme.typography.titleMedium,
            color = JuanPlayerTheme.colors.onSurface
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
            style = JuanPlayerTheme.typography.titleMedium,
            color = JuanPlayerTheme.colors.onSurface
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

        // EPG URL section
        Text(
            text = stringResource(R.string.settings_epg_url),
            style = JuanPlayerTheme.typography.titleMedium,
            color = JuanPlayerTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_epg_url_desc),
            style = JuanPlayerTheme.typography.labelMedium,
            color = JuanPlayerTheme.colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        var epgUrlText by remember(settingsState.customEpgUrl) {
            mutableStateOf(settingsState.customEpgUrl)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = epgUrlText,
                onValueChange = { epgUrlText = it },
                label = {
                    Text(
                        text = stringResource(R.string.settings_epg_url_hint),
                        color = JuanPlayerTheme.colors.onSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = JuanPlayerTheme.typography.bodyMedium.copy(color = JuanPlayerTheme.colors.onSurface),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.saveCustomEpgUrl(epgUrlText) }
                ),
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
            Button(
                onClick = { viewModel.saveCustomEpgUrl(epgUrlText) },
                colors = ButtonDefaults.colors(
                    containerColor = JuanPlayerTheme.colors.primary,
                    contentColor = JuanPlayerTheme.colors.background,
                    focusedContainerColor = JuanPlayerTheme.colors.focusBorder,
                    focusedContentColor = JuanPlayerTheme.colors.background
                )
            ) {
                Text(
                    text = stringResource(R.string.settings_epg_url_save),
                    style = JuanPlayerTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Companion mode toggle
        Text(
            text = stringResource(R.string.settings_companion_mode),
            style = JuanPlayerTheme.typography.titleMedium,
            color = JuanPlayerTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_companion_mode_desc),
            style = JuanPlayerTheme.typography.labelMedium,
            color = JuanPlayerTheme.colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isEnabled = settingsState.companionModeEnabled
            Surface(
                onClick = { viewModel.toggleCompanionMode() },
                shape = ClickableSurfaceDefaults.shape(
                    shape = RoundedCornerShape(8.dp)
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isEnabled) JuanPlayerTheme.colors.primary.copy(alpha = 0.15f) else JuanPlayerTheme.colors.surface,
                    focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
                    pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
                ),
                border = ClickableSurfaceDefaults.border(
                    border = if (isEnabled) Border(
                        border = BorderStroke(2.dp, JuanPlayerTheme.colors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) else Border.None,
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, JuanPlayerTheme.colors.focusBorder),
                        shape = RoundedCornerShape(8.dp)
                    )
                )
            ) {
                Text(
                    text = if (isEnabled) stringResource(R.string.settings_companion_enabled)
                           else stringResource(R.string.settings_companion_disabled),
                    style = JuanPlayerTheme.typography.labelLarge,
                    color = if (isEnabled) JuanPlayerTheme.colors.primary else JuanPlayerTheme.colors.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Export configuration
        SettingsItem(
            title = stringResource(R.string.settings_export),
            subtitle = stringResource(R.string.settings_export_desc),
            onClick = {
                exportLauncher.launch("juanplayer_backup.json")
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Import configuration
        SettingsItem(
            title = stringResource(R.string.settings_import),
            subtitle = stringResource(R.string.settings_import_desc),
            onClick = {
                importLauncher.launch(arrayOf("application/json"))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            style = JuanPlayerTheme.typography.labelMedium,
            color = JuanPlayerTheme.colors.onSurfaceVariant,
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
                    containerColor = JuanPlayerTheme.colors.surfaceVariant,
                    focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
                    pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_delete_confirm),
                        style = JuanPlayerTheme.typography.bodyLarge,
                        color = JuanPlayerTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.colors(
                                containerColor = JuanPlayerTheme.colors.surface,
                                contentColor = JuanPlayerTheme.colors.onSurface,
                                focusedContainerColor = JuanPlayerTheme.colors.focusBorder,
                                focusedContentColor = JuanPlayerTheme.colors.background
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.settings_cancel),
                                style = JuanPlayerTheme.typography.labelLarge,
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
                                containerColor = JuanPlayerTheme.colors.error,
                                contentColor = JuanPlayerTheme.colors.onSurface,
                                focusedContainerColor = JuanPlayerTheme.colors.error.copy(alpha = 0.8f),
                                focusedContentColor = JuanPlayerTheme.colors.onSurface
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.settings_confirm),
                                style = JuanPlayerTheme.typography.labelLarge,
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
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                style = JuanPlayerTheme.typography.bodyLarge,
                color = JuanPlayerTheme.colors.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = JuanPlayerTheme.typography.labelMedium,
                    color = JuanPlayerTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    label: String,
    themeId: String,
    colors: AppColors,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(12.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = JuanPlayerTheme.colors.surface,
            focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
            pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (isSelected) Border(
                border = BorderStroke(3.dp, JuanPlayerTheme.colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) else Border.None,
            focusedBorder = Border(
                border = BorderStroke(3.dp, JuanPlayerTheme.colors.focusBorder),
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
                style = JuanPlayerTheme.typography.labelMedium,
                color = if (isSelected) JuanPlayerTheme.colors.primary else JuanPlayerTheme.colors.onSurfaceVariant
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
            containerColor = if (isSelected) JuanPlayerTheme.colors.primary.copy(alpha = 0.15f) else JuanPlayerTheme.colors.surface,
            focusedContainerColor = JuanPlayerTheme.colors.surfaceVariant,
            pressedContainerColor = JuanPlayerTheme.colors.surfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (isSelected) Border(
                border = BorderStroke(2.dp, JuanPlayerTheme.colors.primary),
                shape = RoundedCornerShape(8.dp)
            ) else Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, JuanPlayerTheme.colors.focusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Text(
            text = label,
            style = JuanPlayerTheme.typography.labelLarge,
            color = if (isSelected) JuanPlayerTheme.colors.primary else JuanPlayerTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}
