package com.neutraltv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.neutraltv.player.ui.navigation.AppNavigation
import com.neutraltv.player.ui.theme.JotaPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JotaPlayerTheme {
                AppNavigation()
            }
        }
    }
}
