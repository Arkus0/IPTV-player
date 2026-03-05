package com.neutraltv.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.neutraltv.player.data.preferences.PreferencesRepository
import com.neutraltv.player.data.preferences.UserPreferences
import com.neutraltv.player.server.CompanionServerService
import com.neutraltv.player.ui.navigation.AppNavigation
import com.neutraltv.player.ui.theme.JuanPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private var companionRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe companion mode preference and start/stop service accordingly
        lifecycleScope.launch {
            preferencesRepository.getUserPreferences()
                .distinctUntilChangedBy { it.companionModeEnabled }
                .collect { prefs ->
                    if (prefs.companionModeEnabled) {
                        startCompanionServer()
                    } else {
                        stopCompanionServer()
                    }
                }
        }

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
        stopCompanionServer()
        super.onDestroy()
    }

    private fun startCompanionServer() {
        if (!companionRunning) {
            val intent = Intent(this, CompanionServerService::class.java)
            startForegroundService(intent)
            companionRunning = true
        }
    }

    private fun stopCompanionServer() {
        if (companionRunning) {
            stopService(Intent(this, CompanionServerService::class.java))
            companionRunning = false
        }
    }
}
