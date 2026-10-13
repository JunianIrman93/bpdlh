package id.lapor.lh.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination

@Serializable
data object ReportListDestination

@Serializable
data object AccountDestination

@Serializable
data class ReportFormDestination(
    val reportId: String,
)

@Serializable
data class ReportDetailDestination(
    val reportId: String,
)
