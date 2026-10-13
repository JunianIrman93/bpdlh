package id.lapor.lh.feature.auth.data

import id.lapor.lh.core.database.UserDao
import id.lapor.lh.core.database.UserEntity
import id.lapor.lh.core.datastore.DeviceIdStore
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
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val api: AuthApi,
        private val deviceKeyManager: DeviceKeyManager,
        private val deviceIdStore: DeviceIdStore,
        private val sessionManager: SessionManager,
        private val userDao: UserDao,
        private val json: Json,
    ) : AuthRepository {
        override suspend fun login(
            email: String,
            password: String,
        ) {
            val deviceId = deviceIdStore.getOrCreate()
            val publicKey = deviceKeyManager.rotateKeyPair()
            val request = LoginRequestDto(email = email, password = password, deviceId = deviceId, devicePublicKey = publicKey)
            val response = runCatching { api.login(request) }.getOrElse { throw it.toApiException() }

            sessionManager.setAccessToken(response.accessToken)
            userDao.upsert(response.user.toEntity())
        }

        override fun observeCurrentUser(): Flow<User?> = userDao.observeCurrentUser().map { it?.toDomain() }

        private fun Throwable.toApiException(): ApiException {
            if (this is HttpException) {
                val body = response()?.errorBody()?.string()
                val parsed = body?.let { runCatching { json.decodeFromString(ErrorResponseDto.serializer(), it) }.getOrNull() }
                return ApiException(parsed?.error?.code ?: "UNKNOWN", parsed?.error?.message ?: message())
            }
            if (this is IOException) {
                return ApiException("NETWORK_ERROR", "Tidak dapat terhubung ke server")
            }
            return ApiException("UNKNOWN", message ?: "Terjadi galat")
        }
    }

private fun UserDto.toEntity() = UserEntity(id = id, email = email, fullName = fullName, role = role)

private fun UserEntity.toDomain() = User(id = id, email = email, fullName = fullName, role = Role.valueOf(role))
