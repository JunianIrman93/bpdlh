package id.lapor.lh.core.network

import id.lapor.lh.core.network.dto.ChallengeRequestDto
import id.lapor.lh.core.network.dto.ChallengeResponseDto
import id.lapor.lh.core.network.dto.LoginRequestDto
import id.lapor.lh.core.network.dto.LoginResponseDto
import id.lapor.lh.core.network.dto.RefreshRequestDto
import id.lapor.lh.core.network.dto.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto,
    ): LoginResponseDto

    @POST("auth/challenge")
    suspend fun challenge(
        @Body request: ChallengeRequestDto,
    ): ChallengeResponseDto

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequestDto,
    ): TokenResponseDto
}
