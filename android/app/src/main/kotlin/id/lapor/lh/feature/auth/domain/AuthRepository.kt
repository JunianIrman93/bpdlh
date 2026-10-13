package id.lapor.lh.feature.auth.domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(
        email: String,
        password: String,
    )

    fun observeCurrentUser(): Flow<User?>

    fun observeSessionEnded(): Flow<Boolean>
}
