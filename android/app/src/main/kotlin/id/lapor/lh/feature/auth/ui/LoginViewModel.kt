package id.lapor.lh.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.R
import id.lapor.lh.core.network.ApiException
import id.lapor.lh.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
            _uiState.update { it.copy(email = value, errorRes = null) }
        }

        fun onPasswordChange(value: String) {
            _uiState.update { it.copy(password = value, errorRes = null) }
        }

        fun submit() {
            val state = _uiState.value
            if (state.isLoading) return
            _uiState.update { it.copy(isLoading = true, errorRes = null) }
            viewModelScope.launch {
                try {
                    authRepository.login(state.email.trim(), state.password)
                    _uiState.update { it.copy(isLoading = false) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, errorRes = e.messageRes()) }
                }
            }
        }
    }

private fun ApiException.messageRes(): Int =
    when (code) {
        "INVALID_CREDENTIALS" -> R.string.login_error_credentials
        "VALIDATION_ERROR" -> R.string.login_error_validation
        "RATE_LIMITED" -> R.string.login_error_rate_limited
        "NETWORK_ERROR" -> R.string.login_error_network
        else -> R.string.login_error_generic
    }
