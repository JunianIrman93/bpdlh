package com.laporlh.app.core.network

class ApiException(
    val code: String,
    message: String,
) : Exception(message)
