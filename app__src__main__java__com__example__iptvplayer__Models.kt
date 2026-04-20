
package com.example.iptvplayer

enum class SourceType { M3U, XTREAM }
enum class ContentMode { LIVE, MOVIES, SERIES }

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val categoryId: String? = null,
    val isLive: Boolean = true,
    val currentProgram: ProgramEntry? = null,
    val nextProgram: ProgramEntry? = null
)

data class ChannelCategory(
    val id: String,
    val name: String
)

data class VodItem(
    val id: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val categoryId: String,
    val categoryName: String,
    val plot: String? = null,
    val rating: String? = null,
    val year: String? = null,
    val isSeries: Boolean = false
)

data class VodCategory(
    val id: String,
    val name: String
)

data class ProgramEntry(
    val title: String,
    val start: String? = null,
    val end: String? = null,
    val description: String? = null
)

data class UserSession(
    val profileName: String,
    val sourceType: SourceType,
    val playlistUrl: String? = null,
    val xtreamServer: String? = null,
    val xtreamUsername: String? = null,
    val xtreamPassword: String? = null
) {
    val key: String
        get() = buildString {
            append(profileName)
            append("|")
            append(sourceType.name)
            append("|")
            append(playlistUrl.orEmpty())
            append("|")
            append(xtreamServer.orEmpty())
            append("|")
            append(xtreamUsername.orEmpty())
        }.hashCode().toString()
}

data class LoginFormState(
    val profileName: String = "",
    val playlistUrl: String = "",
    val xtreamServer: String = "",
    val xtreamUsername: String = "",
    val xtreamPassword: String = "",
    val sourceType: SourceType = SourceType.M3U
)
