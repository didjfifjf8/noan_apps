package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pyforge_settings")

data class IdeSettings(
    val fontSizeSp: Int = 14,
    val tabSize: Int = 4,
    val autoSave: Boolean = true,
    val showLineNumbers: Boolean = true,
    val showSymbolBar: Boolean = true,
    val executionTimeoutSeconds: Int = 30,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val FONT_SIZE = intPreferencesKey("font_size_sp")
        val TAB_SIZE = intPreferencesKey("tab_size")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val SHOW_SYMBOL_BAR = booleanPreferencesKey("show_symbol_bar")
        val EXEC_TIMEOUT = intPreferencesKey("execution_timeout_seconds")
    }

    val settings: Flow<IdeSettings> = context.dataStore.data.map { prefs ->
        IdeSettings(
            fontSizeSp = prefs[Keys.FONT_SIZE] ?: 14,
            tabSize = prefs[Keys.TAB_SIZE] ?: 4,
            autoSave = prefs[Keys.AUTO_SAVE] ?: true,
            showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
            showSymbolBar = prefs[Keys.SHOW_SYMBOL_BAR] ?: true,
            executionTimeoutSeconds = prefs[Keys.EXEC_TIMEOUT] ?: 30,
        )
    }

    suspend fun setFontSize(sp: Int) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = sp.coerceIn(11, 22) }
    }

    suspend fun setTabSize(spaces: Int) {
        context.dataStore.edit { it[Keys.TAB_SIZE] = if (spaces == 2) 2 else 4 }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun setShowLineNumbers(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_LINE_NUMBERS] = enabled }
    }

    suspend fun setShowSymbolBar(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SYMBOL_BAR] = enabled }
    }

    suspend fun setExecutionTimeout(seconds: Int) {
        context.dataStore.edit { it[Keys.EXEC_TIMEOUT] = seconds }
    }
}
