package com.neutraltv.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neutraltv.core.model.TransferDirection
import kotlinx.coroutines.delay

@Composable
fun TvTransferOverlay(
    visible: Boolean,
    direction: TransferDirection?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(300), initialScale = 0.8f),
        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 1.2f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Switch-style icon
                Text(
                    text = "⚡",
                    fontSize = 72.sp
                )

                Text(
                    text = when (direction) {
                        TransferDirection.TO_TV -> "Recibiendo del móvil..."
                        TransferDirection.TO_MOBILE -> "Enviando al móvil..."
                        null -> ""
                    },
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "🎮 Switch Mode",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp
                )
            }
        }
    }

    if (visible) {
        LaunchedEffect(Unit) {
            delay(2000)
            onDismiss()
        }
    }
}
