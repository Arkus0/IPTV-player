package com.neutraltv.player.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val themeIdKey = stringPreferencesKey("theme_id")
    private val fontScaleKey = floatPreferencesKey("font_scale")

    fun getUserPreferences(): Flow<UserPreferences> {
        return dataStore.data.map { prefs ->
            UserPreferences(
                themeId = prefs[themeIdKey] ?: "purple_dark",
                fontScale = prefs[fontScaleKey] ?: 1.0f
            )
        }
    }

    suspend fun setTheme(themeId: String) {
        dataStore.edit { prefs ->
            prefs[themeIdKey] = themeId
        }
    }

    suspend fun setFontScale(scale: Float) {
        dataStore.edit { prefs ->
            prefs[fontScaleKey] = scale
        }
    }
}
