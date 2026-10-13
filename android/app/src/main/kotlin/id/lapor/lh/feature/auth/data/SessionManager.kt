package id.lapor.lh.feature.auth.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager
    @Inject
    constructor() {
        private val _accessToken = MutableStateFlow<String?>(null)
        val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

        fun setAccessToken(token: String) {
            _accessToken.value = token
        }
    }
