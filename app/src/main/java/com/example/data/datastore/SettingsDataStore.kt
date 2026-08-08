package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val KEY_THEME_PRESET = stringPreferencesKey("theme_preset")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_NOTIFICATION_SOUND_ENABLED = booleanPreferencesKey("notification_sound_enabled")
        val KEY_NOTIFICATION_VIBRATE_ENABLED = booleanPreferencesKey("notification_vibrate_enabled")
        val KEY_AUTO_SCROLL_ENABLED = booleanPreferencesKey("auto_scroll_enabled")
        val KEY_ONLINE_SEARCH_ENABLED = booleanPreferencesKey("online_search_enabled")
    }

    val systemPromptFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SYSTEM_PROMPT] ?: ""
    }

    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_DARK_MODE] ?: true
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val useDynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_USE_DYNAMIC_COLOR] ?: true
    }

    val themePresetFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_PRESET] ?: "DEFAULT"
    }

    val temperatureFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_TEMPERATURE] ?: 0.7f
    }

    val maxTokensFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_MAX_TOKENS] ?: 2048
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val notificationSoundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATION_SOUND_ENABLED] ?: true
    }

    val notificationVibrateEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATION_VIBRATE_ENABLED] ?: true
    }

    val autoScrollEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_SCROLL_ENABLED] ?: true
    }

    val onlineSearchEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONLINE_SEARCH_ENABLED] ?: true
    }

    suspend fun saveSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SYSTEM_PROMPT] = prompt
        }
    }

    suspend fun saveDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun saveUseDynamicColor(useDynamic: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USE_DYNAMIC_COLOR] = useDynamic
        }
    }

    suspend fun saveThemePreset(preset: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_PRESET] = preset
        }
    }

    suspend fun saveTemperature(temperature: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TEMPERATURE] = temperature
        }
    }

    suspend fun saveMaxTokens(maxTokens: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MAX_TOKENS] = maxTokens
        }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveNotificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_SOUND_ENABLED] = enabled
        }
    }

    suspend fun saveNotificationVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_VIBRATE_ENABLED] = enabled
        }
    }

    suspend fun saveAutoScrollEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_SCROLL_ENABLED] = enabled
        }
    }

    suspend fun saveOnlineSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONLINE_SEARCH_ENABLED] = enabled
        }
    }
}
