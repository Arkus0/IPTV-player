package com.neutraltv.player.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Text
import com.neutraltv.player.R
import com.neutraltv.player.ui.components.FocusableCard
import com.neutraltv.player.ui.components.LoadingIndicator
import com.neutraltv.player.ui.theme.JotaPlayerTypography
import com.neutraltv.player.ui.theme.OnSurfaceVariant
import com.neutraltv.player.ui.theme.Primary

@Composable
fun HomeScreen(
    onNavigateToChannels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        LoadingIndicator(message = stringResource(R.string.loading))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 40.dp)
    ) {
        Text(
            text = "JotaPlayer",
            style = JotaPlayerTypography.headlineLarge,
            color = Primary
        )

        uiState.playlist?.let { playlist ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlist.name} · ${playlist.channelCount} canales",
                style = JotaPlayerTypography.labelMedium,
                color = OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeMenuItem(
                title = stringResource(R.string.tv_live),
                icon = "\u25B6", // Play triangle
                onClick = onNavigateToChannels,
                modifier = Modifier.size(280.dp, 200.dp)
            )

            Spacer(modifier = Modifier.width(32.dp))

            HomeMenuItem(
                title = stringResource(R.string.settings),
                icon = "\u2699", // Gear
                onClick = onNavigateToSettings,
                modifier = Modifier.size(280.dp, 200.dp)
            )
        }
    }
}

@Composable
private fun HomeMenuItem(
    title: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = JotaPlayerTypography.headlineLarge.copy(
                    fontSize = JotaPlayerTypography.headlineLarge.fontSize * 1.5
                ),
                color = Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = JotaPlayerTypography.titleLarge
            )
        }
    }
}
