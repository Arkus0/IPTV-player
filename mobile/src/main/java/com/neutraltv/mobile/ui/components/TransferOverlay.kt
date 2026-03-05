package com.neutraltv.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neutraltv.core.transfer.TransferSoundPlayer
import com.neutraltv.mobile.R
import kotlinx.coroutines.delay

@Composable
fun TransferOverlay(
    visible: Boolean,
    isReceiving: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInHorizontally(
            tween(400),
            initialOffsetX = { if (isReceiving) it else -it }
        ),
        exit = fadeOut(tween(300)) + slideOutHorizontally(
            tween(400),
            targetOffsetX = { if (isReceiving) -it else it }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "⚡",
                    fontSize = 80.sp
                )

                Text(
                    text = if (isReceiving) "Recibiendo de TV..." else "Enviando a TV...",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Switch Mode",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
        }
    }

    if (visible) {
        LaunchedEffect(Unit) {
            // Play the Switch click sound
            try {
                val soundPlayer = TransferSoundPlayer(context)
                soundPlayer.load(R.raw.switch_click)
                delay(200) // brief delay for sound to load
                soundPlayer.playClick()
                delay(1800)
                soundPlayer.release()
            } catch (_: Exception) {
                delay(2000)
            }
            onDismiss()
        }
    }
}
