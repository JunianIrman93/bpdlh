package id.lapor.lh.feature.auth.data

import dagger.Lazy
import id.lapor.lh.core.network.dto.ErrorResponseDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

private val renewableCodes = setOf("TOKEN_EXPIRED", "UNAUTHENTICATED", "TOKEN_INVALID")
private const val ERROR_PEEK_BYTES = 4096L

@Singleton
class TokenAuthenticator
    @Inject
    constructor(
        private val refresher: Lazy<TokenRefresher>,
        private val json: Json,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (response.priorResponse != null) return null
            val body = response.peekBody(ERROR_PEEK_BYTES).string()
            val code = runCatching { json.decodeFromString(ErrorResponseDto.serializer(), body).error.code }.getOrNull()
            if (code !in renewableCodes) return null

            val staleToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val token = runCatching { runBlocking { refresher.get().refresh(staleToken) } }.getOrNull() ?: return null
            return response.request
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
    }
