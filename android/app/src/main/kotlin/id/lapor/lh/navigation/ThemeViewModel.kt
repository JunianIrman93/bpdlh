package id.lapor.lh.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.core.settings.ThemeSettings
import id.lapor.lh.core.ui.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel
    @Inject
    constructor(
        themeSettings: ThemeSettings,
    ) : ViewModel() {
        val themeMode: StateFlow<ThemeMode?> =
            themeSettings.themeMode.stateIn<ThemeMode?>(viewModelScope, SharingStarted.Eagerly, null)
    }
