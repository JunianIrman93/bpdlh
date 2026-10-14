package id.lapor.lh.feature.report.domain

sealed interface LocationResult {
    data class Found(
        val location: ReportLocation,
    ) : LocationResult

    data object PermissionDenied : LocationResult

    data object Disabled : LocationResult

    data object Timeout : LocationResult
}

interface LocationProvider {
    suspend fun currentLocation(): LocationResult

    suspend fun addressOf(location: ReportLocation): String?
}
