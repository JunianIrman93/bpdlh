package com.laporlh.app.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laporlh.app.core.network.ApiException
import com.laporlh.app.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun onEmailChange(value: String) {
            _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
        }

        fun onPasswordChange(value: String) {
            _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
        }

        fun submit() {
            val state = _uiState.value
            if (state.isLoading) return
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            viewModelScope.launch {
                try {
                    authRepository.login(state.email.trim(), state.password)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } catch (e: ApiException) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.toUserMessage())
                }
            }
        }
    }

private fun ApiException.toUserMessage(): String =
    when (code) {
        "INVALID_CREDENTIALS" -> "Surel atau kata sandi salah"
        "VALIDATION_ERROR" -> "Isian belum lengkap atau tidak valid"
        "RATE_LIMITED" -> "Terlalu banyak percobaan, coba lagi nanti"
        "NETWORK_ERROR" -> "Tidak dapat terhubung ke server"
        else -> "Gagal masuk, coba lagi"
    }
