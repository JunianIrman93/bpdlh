package com.laporlh.app.core.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "laporlh_prefs")
private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

@Singleton
class DeviceIdStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun getOrCreate(): String {
            val existing = context.dataStore.data.first()[DEVICE_ID_KEY]
            if (existing != null) return existing
            val generated = UUID.randomUUID().toString()
            context.dataStore.edit { it[DEVICE_ID_KEY] = generated }
            return generated
        }
    }
