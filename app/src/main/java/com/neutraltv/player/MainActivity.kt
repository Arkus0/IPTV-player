package com.neutraltv.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neutraltv.player.data.preferences.PreferencesRepository
import com.neutraltv.player.data.preferences.UserPreferences
import com.neutraltv.player.server.CompanionServerService
import com.neutraltv.player.ui.navigation.AppNavigation
import com.neutraltv.player.ui.theme.JuanPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start companion server for mobile remote control
        startCompanionServer()

        setContent {
            val preferences by preferencesRepository.getUserPreferences()
                .collectAsState(initial = UserPreferences())

            JuanPlayerTheme(
                themeId = preferences.themeId,
                fontScale = preferences.fontScale
            ) {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        stopService(Intent(this, CompanionServerService::class.java))
        super.onDestroy()
    }

    private fun startCompanionServer() {
        val intent = Intent(this, CompanionServerService::class.java)
        startForegroundService(intent)
    }
}
