package id.lapor.lh.feature.auth.ui

import androidx.annotation.StringRes

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val sessionEnded: Boolean = false,
)
