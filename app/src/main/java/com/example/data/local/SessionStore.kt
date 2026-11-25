package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ifintech_session")

class SessionStore(private val context: Context) {
    private val keyUserId = stringPreferencesKey("current_user_id")

    val currentUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[keyUserId]
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(keyUserId)
            } else {
                prefs[keyUserId] = userId
            }
        }
    }
}
