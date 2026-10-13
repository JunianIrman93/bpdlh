package id.lapor.lh.feature.auth.data

import id.lapor.lh.core.network.ApiException
import id.lapor.lh.core.network.AuthApi
import id.lapor.lh.core.network.dto.ErrorResponseDto
import id.lapor.lh.core.network.dto.LoginRequestDto
import id.lapor.lh.core.network.dto.UserDto
import id.lapor.lh.core.security.DeviceKeyManager
import id.lapor.lh.feature.auth.domain.AuthRepository
import id.lapor.lh.feature.auth.domain.Role
import id.lapor.lh.feature.auth.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val api: AuthApi,
        private val deviceKeyManager: DeviceKeyManager,
        private val localStore: AuthLocalStore,
        private val sessionManager: SessionManager,
        private val json: Json,
    ) : AuthRepository {
        override suspend fun login(
            email: String,
            password: String,
        ) {
            val deviceId = localStore.deviceId()
            val publicKey = deviceKeyManager.rotateKeyPair()
            val request = LoginRequestDto(email = email, password = password, deviceId = deviceId, devicePublicKey = publicKey)
            val response = runCatching { api.login(request) }.getOrElse { throw it.toApiException() }

            localStore.saveSession(
                user = response.user.toDomain(),
                deviceExpiresAt = System.currentTimeMillis() + response.deviceExpiresIn * 1000L,
            )
            sessionManager.setAccessToken(response.accessToken)
        }

        override fun observeCurrentUser(): Flow<User?> = localStore.currentUser

        private fun Throwable.toApiException(): ApiException {
            if (this is HttpException) {
                val body = response()?.errorBody()?.string()
                val parsed = body?.let { runCatching { json.decodeFromString(ErrorResponseDto.serializer(), it) }.getOrNull() }
                return ApiException(parsed?.error?.code ?: "UNKNOWN", parsed?.error?.message ?: message())
            }
            if (this is IOException) {
                return ApiException("NETWORK_ERROR", message ?: "network error")
            }
            return ApiException("UNKNOWN", message ?: "unknown error")
        }
    }

private fun UserDto.toDomain() = User(id = id, email = email, fullName = fullName, role = Role.valueOf(role))
