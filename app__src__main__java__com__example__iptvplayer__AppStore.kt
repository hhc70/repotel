package com.example.iptvplayer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "iptv_app_store")

class AppStore(private val context: Context) {
    private val sessionProfileKey = stringPreferencesKey("session_profile")
    private val sessionTypeKey = stringPreferencesKey("session_type")
    private val sessionPlaylistUrlKey = stringPreferencesKey("session_playlist_url")
    private val sessionXtreamServerKey = stringPreferencesKey("session_xtream_server")
    private val sessionXtreamUserKey = stringPreferencesKey("session_xtream_user")
    private val sessionXtreamPassKey = stringPreferencesKey("session_xtream_pass")

    val session: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val profile = prefs[sessionProfileKey] ?: return@map null
        val type = prefs[sessionTypeKey]?.let { runCatching { SourceType.valueOf(it) }.getOrNull() } ?: SourceType.M3U
        UserSession(
            profileName = profile,
            sourceType = type,
            playlistUrl = prefs[sessionPlaylistUrlKey],
            xtreamServer = prefs[sessionXtreamServerKey],
            xtreamUsername = prefs[sessionXtreamUserKey],
            xtreamPassword = prefs[sessionXtreamPassKey]
        )
    }

    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[sessionProfileKey] = session.profileName
            prefs[sessionTypeKey] = session.sourceType.name
            session.playlistUrl?.let { prefs[sessionPlaylistUrlKey] = it } ?: prefs.remove(sessionPlaylistUrlKey)
            session.xtreamServer?.let { prefs[sessionXtreamServerKey] = it } ?: prefs.remove(sessionXtreamServerKey)
            session.xtreamUsername?.let { prefs[sessionXtreamUserKey] = it } ?: prefs.remove(sessionXtreamUserKey)
            session.xtreamPassword?.let { prefs[sessionXtreamPassKey] = it } ?: prefs.remove(sessionXtreamPassKey)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(sessionProfileKey)
            prefs.remove(sessionTypeKey)
            prefs.remove(sessionPlaylistUrlKey)
            prefs.remove(sessionXtreamServerKey)
            prefs.remove(sessionXtreamUserKey)
            prefs.remove(sessionXtreamPassKey)
        }
    }
}
