package com.example.iptvplayer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesStore by preferencesDataStore(name = "favorites_store")

class FavoritesStore(private val context: Context) {

    fun favorites(sessionKey: String): Flow<Set<String>> {
        val key = stringSetPreferencesKey("favorite_channel_ids_$sessionKey")
        return context.favoritesStore.data.map { prefs -> prefs[key].orEmpty() }
    }

    suspend fun toggle(sessionKey: String, channelId: String) {
        val key = stringSetPreferencesKey("favorite_channel_ids_$sessionKey")
        context.favoritesStore.edit { prefs ->
            val current = prefs[key].orEmpty().toMutableSet()
            if (!current.add(channelId)) current.remove(channelId)
            prefs[key] = current
        }
    }
}
