package id.lapor.lh.feature.auth.data

import id.lapor.lh.core.network.ApiException
import id.lapor.lh.core.network.AuthApi
import id.lapor.lh.core.network.apiCall
import id.lapor.lh.core.network.dto.ChallengeRequestDto
import id.lapor.lh.core.network.dto.RefreshRequestDto
import id.lapor.lh.core.security.DeviceKeyManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

const val DEVICE_KEY_INVALID = "DEVICE_KEY_INVALID"
private const val NONCE_INVALID = "NONCE_INVALID"
private const val MIN_REMAINING_SECONDS = 60
private const val SIGNING_CONTEXT = "laporlh-auth-v1"

@Singleton
class TokenRefresher
    @Inject
    constructor(
        private val api: AuthApi,
        private val deviceKeyManager: DeviceKeyManager,
        private val localStore: AuthLocalStore,
        private val sessionManager: SessionManager,
        private val json: Json,
    ) {
        private val mutex = Mutex()

        suspend fun ensureFresh() {
            if (sessionManager.remainingSeconds >= MIN_REMAINING_SECONDS) return
            refresh(sessionManager.accessToken)
        }

        suspend fun refresh(staleToken: String?): String =
            mutex.withLock {
                val current = sessionManager.accessToken
                if (current != null && current != staleToken && sessionManager.remainingSeconds >= MIN_REMAINING_SECONDS) {
                    return@withLock current
                }
                if (localStore.currentUser() == null) throw sessionEnded()
                try {
                    try {
                        renew()
                    } catch (e: ApiException) {
                        if (e.code != NONCE_INVALID) throw e
                        renew()
                    }
                } catch (e: ApiException) {
                    if (e.code != DEVICE_KEY_INVALID && e.code != NONCE_INVALID) throw e
                    sessionManager.clear()
                    localStore.endSession()
                    throw sessionEnded()
                }
            }

        private suspend fun renew(): String {
            val deviceId = localStore.deviceId()
            val nonce = apiCall(json) { api.challenge(ChallengeRequestDto(deviceId)) }.nonce
            val signature =
                try {
                    deviceKeyManager.sign("$SIGNING_CONTEXT\n$deviceId\n$nonce")
                } catch (e: GeneralSecurityException) {
                    throw ApiException(DEVICE_KEY_INVALID, e.message ?: "device key unusable")
                }
            val response = apiCall(json) { api.refresh(RefreshRequestDto(deviceId, nonce, signature)) }
            sessionManager.setAccessToken(response.accessToken, response.expiresIn)
            localStore.updateDeviceExpiry(System.currentTimeMillis() + response.deviceExpiresIn * 1000L)
            return response.accessToken
        }

        private fun sessionEnded() = ApiException(DEVICE_KEY_INVALID, "session ended")
    }
