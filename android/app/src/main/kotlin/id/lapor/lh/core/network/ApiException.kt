package id.lapor.lh.core.network

class ApiException(
    val code: String,
    message: String,
) : Exception(message)
