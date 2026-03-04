package com.neutraltv.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.neutraltv.mobile.ui.navigation.MobileNavigation
import com.neutraltv.mobile.ui.theme.JuanPlayerMobileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JuanPlayerMobileTheme {
                MobileNavigation()
            }
        }
    }
}
