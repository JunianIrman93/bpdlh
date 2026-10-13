package id.lapor.lh.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.feature.auth.data.SessionManager
import id.lapor.lh.feature.auth.domain.AuthRepository
import id.lapor.lh.feature.auth.domain.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SessionViewModel
    @Inject
    constructor(
        sessionManager: SessionManager,
        authRepository: AuthRepository,
    ) : ViewModel() {
        val isAuthenticated: StateFlow<Boolean> =
            sessionManager.accessToken
                .map { it != null }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = sessionManager.accessToken.value != null,
                )

        val currentUser: StateFlow<User?> =
            authRepository
                .observeCurrentUser()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )
    }
