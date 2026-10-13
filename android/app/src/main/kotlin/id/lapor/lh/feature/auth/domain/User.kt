package id.lapor.lh.feature.auth.domain

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: Role,
)

enum class Role {
    FIELD_OFFICER,
    REVIEWER,
}
