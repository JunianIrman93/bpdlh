package id.lapor.lh.feature.auth.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import id.lapor.lh.feature.auth.domain.Role
import id.lapor.lh.feature.auth.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth")

private val DEVICE_ID = stringPreferencesKey("device_id")
private val USER_ID = stringPreferencesKey("user_id")
private val USER_EMAIL = stringPreferencesKey("user_email")
private val USER_FULL_NAME = stringPreferencesKey("user_full_name")
private val USER_ROLE = stringPreferencesKey("user_role")
private val DEVICE_EXPIRES_AT = longPreferencesKey("device_expires_at")
private val SESSION_ENDED = booleanPreferencesKey("session_ended")

@Singleton
class AuthLocalStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val store = context.authDataStore

        val currentUser: Flow<User?> = store.data.map { it.toActiveUser() }

        val sessionEnded: Flow<Boolean> = store.data.map { it[SESSION_ENDED] == true }

        suspend fun currentUser(): User? = store.data.first().toActiveUser()

        suspend fun deviceId(): String {
            store.data.first()[DEVICE_ID]?.let { return it }
            val generated = UUID.randomUUID().toString()
            store.edit { it[DEVICE_ID] = generated }
            return generated
        }

        suspend fun saveSession(
            user: User,
            deviceExpiresAt: Long,
        ) {
            store.edit {
                it[USER_ID] = user.id
                it[USER_EMAIL] = user.email
                it[USER_FULL_NAME] = user.fullName
                it[USER_ROLE] = user.role.name
                it[DEVICE_EXPIRES_AT] = deviceExpiresAt
                it.remove(SESSION_ENDED)
            }
        }

        suspend fun updateDeviceExpiry(deviceExpiresAt: Long) {
            store.edit { if (it.contains(USER_ID)) it[DEVICE_EXPIRES_AT] = deviceExpiresAt }
        }

        suspend fun endSession() {
            store.edit {
                it.removeProfile()
                it[SESSION_ENDED] = true
            }
        }

        suspend fun clearSession() {
            store.edit {
                it.removeProfile()
                it.remove(SESSION_ENDED)
            }
        }

        private fun MutablePreferences.removeProfile() {
            remove(USER_ID)
            remove(USER_EMAIL)
            remove(USER_FULL_NAME)
            remove(USER_ROLE)
            remove(DEVICE_EXPIRES_AT)
        }

        private fun Preferences.toActiveUser(): User? {
            val id = this[USER_ID] ?: return null
            if ((this[DEVICE_EXPIRES_AT] ?: 0L) <= System.currentTimeMillis()) return null
            return User(
                id = id,
                email = this[USER_EMAIL].orEmpty(),
                fullName = this[USER_FULL_NAME].orEmpty(),
                role = Role.valueOf(this[USER_ROLE] ?: return null),
            )
        }
    }
