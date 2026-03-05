package com.neutraltv.player.ui.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Text
import com.neutraltv.player.ui.theme.JuanPlayerTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val hasPlaylist by viewModel.hasPlaylist.collectAsState()

    LaunchedEffect(hasPlaylist) {
        if (hasPlaylist != null) {
            delay(1000L) // Brief splash display
            if (hasPlaylist == true) {
                onNavigateToHome()
            } else {
                onNavigateToOnboarding()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "JuanPlayer",
            style = JuanPlayerTheme.typography.headlineLarge,
            color = JuanPlayerTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tu reproductor IPTV",
            style = JuanPlayerTheme.typography.bodyMedium,
            color = JuanPlayerTheme.colors.onSurfaceVariant
        )
    }
}
