package com.laporlh.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val error: ErrorBodyDto,
)

@Serializable
data class ErrorBodyDto(
    val code: String,
    val message: String,
)
