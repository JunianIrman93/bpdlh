package id.lapor.lh.feature.auth.data

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager
    @Inject
    constructor() {
        private class Token(
            val value: String,
            val expiresAtElapsed: Long,
        )

        @Volatile
        private var token: Token? = null

        val accessToken: String? get() = token?.value

        val remainingSeconds: Long
            get() = token?.let { (it.expiresAtElapsed - SystemClock.elapsedRealtime()) / 1000 } ?: 0

        fun setAccessToken(
            value: String,
            expiresInSeconds: Int,
        ) {
            token = Token(value, SystemClock.elapsedRealtime() + expiresInSeconds * 1000L)
        }

        fun clear() {
            token = null
        }
    }
