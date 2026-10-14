package id.lapor.lh.core.settings

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import id.lapor.lh.core.ui.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val THEME_MODE = stringPreferencesKey("theme_mode")

@Singleton
class ThemeSettings
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val store = context.settingsDataStore

        val themeMode: Flow<ThemeMode> =
            store.data.map { preferences ->
                ThemeMode.entries.firstOrNull { it.name == preferences[THEME_MODE] } ?: ThemeMode.SYSTEM
            }

        suspend fun setThemeMode(mode: ThemeMode) {
            store.edit { it[THEME_MODE] = mode.name }
            applyToSystem(mode)
        }

        private fun applyToSystem(mode: ThemeMode) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
            val nightMode =
                when (mode) {
                    ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                    ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                    ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                }
            context.getSystemService(UiModeManager::class.java).setApplicationNightMode(nightMode)
        }
    }
