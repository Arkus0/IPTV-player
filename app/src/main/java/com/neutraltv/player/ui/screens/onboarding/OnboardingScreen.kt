package com.neutraltv.player.ui.screens.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import androidx.activity.compose.BackHandler
import com.neutraltv.player.R
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.Background
import com.neutraltv.player.ui.theme.Error
import com.neutraltv.player.ui.theme.FocusBorder
import com.neutraltv.player.ui.theme.JuanPlayerTheme
import com.neutraltv.player.ui.theme.OnSurface
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary
import com.neutraltv.player.ui.theme.Surface

@Composable
fun OnboardingScreen(
    onPlaylistLoaded: () -> Unit,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (showBackButton && onBack != null) {
        BackHandler { onBack() }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                if (content != null) {
                    viewModel.loadPlaylistFromContent(content, uri.toString())
                }
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onPlaylistLoaded()
        }
    }

    if (uiState.isLoading) {
        LoadingIndicator(message = stringResource(R.string.loading))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 120.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showBackButton && onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Button(
                    onClick = { onBack() },
                    colors = ButtonDefaults.colors(
                        containerColor = Surface,
                        contentColor = OnSurface,
                        focusedContainerColor = FocusBorder,
                        focusedContentColor = Background
                    )
                ) {
                    Text(
                        text = stringResource(R.string.back),
                        style = JuanPlayerTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            text = if (showBackButton) stringResource(R.string.add_playlist_title) else stringResource(R.string.onboarding_title),
            style = JuanPlayerTheme.typography.headlineMedium,
            color = Primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = JuanPlayerTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChanged,
            label = {
                Text(
                    text = stringResource(R.string.playlist_name_hint),
                    color = OnSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = JuanPlayerTheme.typography.bodyLarge.copy(color = OnSurface),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FocusBorder,
                unfocusedBorderColor = OnSurfaceVariant,
                cursorColor = Primary,
                focusedLabelColor = FocusBorder,
                unfocusedLabelColor = OnSurfaceVariant,
                focusedContainerColor = Background,
                unfocusedContainerColor = Background
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.url,
            onValueChange = viewModel::onUrlChanged,
            label = {
                Text(
                    text = stringResource(R.string.onboarding_url_hint),
                    color = OnSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = JuanPlayerTheme.typography.bodyLarge.copy(color = OnSurface),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { viewModel.loadPlaylist() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FocusBorder,
                unfocusedBorderColor = OnSurfaceVariant,
                cursorColor = Primary,
                focusedLabelColor = FocusBorder,
                unfocusedLabelColor = OnSurfaceVariant,
                focusedContainerColor = Background,
                unfocusedContainerColor = Background
            ),
            shape = RoundedCornerShape(8.dp),
            isError = uiState.error != null
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.error!!,
                style = JuanPlayerTheme.typography.labelMedium,
                color = Error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { viewModel.loadPlaylist() },
                colors = ButtonDefaults.colors(
                    containerColor = Primary,
                    contentColor = Background,
                    focusedContainerColor = FocusBorder,
                    focusedContentColor = Background
                )
            ) {
                Text(
                    text = stringResource(R.string.onboarding_load),
                    style = JuanPlayerTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    filePickerLauncher.launch(intent)
                },
                colors = ButtonDefaults.colors(
                    containerColor = Surface,
                    contentColor = OnSurface,
                    focusedContainerColor = FocusBorder,
                    focusedContentColor = Background
                )
            ) {
                Text(
                    text = stringResource(R.string.onboarding_load_file),
                    style = JuanPlayerTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}
