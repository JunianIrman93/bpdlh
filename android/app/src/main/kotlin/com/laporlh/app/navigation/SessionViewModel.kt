package com.laporlh.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laporlh.app.core.session.SessionManager
import com.laporlh.app.feature.auth.domain.AuthRepository
import com.laporlh.app.feature.auth.domain.User
import dagger.hilt.android.lifecycle.HiltViewModel
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
