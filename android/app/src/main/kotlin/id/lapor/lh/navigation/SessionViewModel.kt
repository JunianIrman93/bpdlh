package id.lapor.lh.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SessionViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
    ) : ViewModel() {
        val isAuthenticated: StateFlow<Boolean?> =
            authRepository
                .observeCurrentUser()
                .map { it != null }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }
