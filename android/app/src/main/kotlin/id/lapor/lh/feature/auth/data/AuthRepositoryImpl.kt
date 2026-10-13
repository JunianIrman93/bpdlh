package id.lapor.lh.feature.auth.data

import id.lapor.lh.core.network.AuthApi
import id.lapor.lh.core.network.apiCall
import id.lapor.lh.core.network.dto.LoginRequestDto
import id.lapor.lh.core.network.dto.UserDto
import id.lapor.lh.core.security.DeviceKeyManager
import id.lapor.lh.feature.auth.domain.AuthRepository
import id.lapor.lh.feature.auth.domain.Role
import id.lapor.lh.feature.auth.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
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
            val response = apiCall(json) { api.login(request) }

            sessionManager.setAccessToken(response.accessToken, response.expiresIn)
            localStore.saveSession(
                user = response.user.toDomain(),
                deviceExpiresAt = System.currentTimeMillis() + response.deviceExpiresIn * 1000L,
            )
        }

        override fun observeCurrentUser(): Flow<User?> = localStore.currentUser

        override fun observeSessionEnded(): Flow<Boolean> = localStore.sessionEnded
    }

private fun UserDto.toDomain() = User(id = id, email = email, fullName = fullName, role = Role.valueOf(role))
