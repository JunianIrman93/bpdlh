package id.lapor.lh.core.network

import id.lapor.lh.core.network.dto.ErrorResponseDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class ApiException(
    val code: String,
    message: String,
    val httpStatus: Int? = null,
) : Exception(message)

suspend fun <T> apiCall(
    json: Json,
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        throw e
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        val parsed = body?.let { runCatching { json.decodeFromString(ErrorResponseDto.serializer(), it) }.getOrNull() }
        throw ApiException(parsed?.error?.code ?: "UNKNOWN", parsed?.error?.message ?: e.message(), e.code())
    } catch (e: IOException) {
        throw ApiException("NETWORK_ERROR", e.message ?: "network error")
    } catch (e: Exception) {
        throw ApiException("UNKNOWN", e.message ?: "unknown error")
    }
