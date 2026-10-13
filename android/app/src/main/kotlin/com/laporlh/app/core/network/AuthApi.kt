package com.laporlh.app.core.network

import com.laporlh.app.core.network.dto.LoginRequestDto
import com.laporlh.app.core.network.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto,
    ): LoginResponseDto
}
