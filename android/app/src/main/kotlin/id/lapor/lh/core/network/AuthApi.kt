package id.lapor.lh.core.network

import id.lapor.lh.core.network.dto.LoginRequestDto
import id.lapor.lh.core.network.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto,
    ): LoginResponseDto
}
