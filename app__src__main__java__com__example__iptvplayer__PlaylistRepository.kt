
package com.example.iptvplayer

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class LoadedContent(
    val channels: List<Channel>,
    val categories: List<ChannelCategory>,
    val movieItems: List<VodItem> = emptyList(),
    val movieCategories: List<VodCategory> = emptyList(),
    val seriesItems: List<VodItem> = emptyList(),
    val seriesCategories: List<VodCategory> = emptyList()
)

class PlaylistRepository(private val context: Context) {

    suspend fun loadM3uFromUrl(url: String): Result<LoadedContent> = withContext(Dispatchers.IO) {
        runCatching {
            val content = URL(url).readText()
            val channels = M3uParser.parse(content)
            LoadedContent(
                channels = channels,
                categories = channels.categoriesFromGroups()
            )
        }
    }

    suspend fun loadM3uFromUri(uri: Uri): Result<LoadedContent> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Unable to read playlist")
            val channels = M3uParser.parse(content)
            LoadedContent(
                channels = channels,
                categories = channels.categoriesFromGroups()
            )
        }
    }

    suspend fun loginXtream(server: String, username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = readJson(
                server = server,
                path = "player_api.php",
                query = mapOf(
                    "username" to username,
                    "password" to password
                )
            )
            val userInfo = payload.optJSONObject("user_info") ?: error("Invalid Xtream response")
            val auth = userInfo.optInt("auth", 0)
            if (auth != 1) error("Xtream login failed")
            Unit
        }
    }

    suspend fun loadXtream(server: String, username: String, password: String): Result<LoadedContent> = withContext(Dispatchers.IO) {
        runCatching {
            val liveCategoriesJson = safeReadArray(
                server, "player_api.php",
                mapOf("username" to username, "password" to password, "action" to "get_live_categories")
            ) ?: JSONArray()

            val liveCategories = buildList {
                add(ChannelCategory(id = "favorites", name = "Favorites"))
                add(ChannelCategory(id = "all", name = "All Channels"))
                for (index in 0 until liveCategoriesJson.length()) {
                    val item = liveCategoriesJson.getJSONObject(index)
                    add(
                        ChannelCategory(
                            id = item.optString("category_id").ifBlank { "cat_$index" },
                            name = item.optString("category_name").ifBlank { "Category ${index + 1}" }
                        )
                    )
                }
            }

            val liveStreamsJson = safeReadArray(
                server, "player_api.php",
                mapOf("username" to username, "password" to password, "action" to "get_live_streams")
            ) ?: JSONArray()

            val channels = buildList {
                for (index in 0 until liveStreamsJson.length()) {
                    val item = liveStreamsJson.getJSONObject(index)
                    val streamId = item.optString("stream_id")
                    if (streamId.isBlank()) continue
                    val categoryId = item.optString("category_id").ifBlank { "other" }

                    val program = fetchShortEpg(server, username, password, streamId)
                    add(
                        Channel(
                            id = "xtream_live_$streamId",
                            name = item.optString("name").ifBlank { "Channel $streamId" },
                            url = buildXtreamStreamUrl(server, username, password, streamId, "live", "ts"),
                            logo = item.optString("stream_icon").takeIf { it.isNotBlank() && it != "null" },
                            group = liveCategories.firstOrNull { it.id == categoryId }?.name ?: "Other",
                            categoryId = categoryId,
                            isLive = true,
                            currentProgram = program.firstOrNull(),
                            nextProgram = program.getOrNull(1)
                        )
                    )
                }
            }

            val movieCategories = loadVodCategories(
                server, username, password,
                action = "get_vod_categories"
            )
            val seriesCategories = loadVodCategories(
                server, username, password,
                action = "get_series_categories"
            )

            val movies = loadVodItems(
                server = server,
                username = username,
                password = password,
                action = "get_vod_streams",
                categories = movieCategories,
                isSeries = false
            )

            val series = loadVodItems(
                server = server,
                username = username,
                password = password,
                action = "get_series",
                categories = seriesCategories,
                isSeries = true
            )

            LoadedContent(
                channels = channels,
                categories = liveCategories,
                movieItems = movies,
                movieCategories = movieCategories,
                seriesItems = series,
                seriesCategories = seriesCategories
            )
        }
    }

    private fun loadVodCategories(
        server: String,
        username: String,
        password: String,
        action: String
    ): List<VodCategory> {
        val json = safeReadArray(
            server, "player_api.php",
            mapOf("username" to username, "password" to password, "action" to action)
        ) ?: JSONArray()

        return buildList {
            add(VodCategory("all", "All"))
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(
                    VodCategory(
                        id = item.optString("category_id").ifBlank { "vod_cat_$index" },
                        name = item.optString("category_name").ifBlank { "Category ${index + 1}" }
                    )
                )
            }
        }.distinctBy { it.id }
    }

    private fun loadVodItems(
        server: String,
        username: String,
        password: String,
        action: String,
        categories: List<VodCategory>,
        isSeries: Boolean
    ): List<VodItem> {
        val json = safeReadArray(
            server, "player_api.php",
            mapOf("username" to username, "password" to password, "action" to action)
        ) ?: JSONArray()

        return buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                val idKey = if (isSeries) item.optString("series_id") else item.optString("stream_id")
                if (idKey.isBlank()) continue
                val categoryId = item.optString("category_id").ifBlank { "uncategorized" }
                val categoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "Other"

                val url = if (isSeries) {
                    ""
                } else {
                    val extension = item.optString("container_extension").ifBlank { "mp4" }
                    buildXtreamStreamUrl(server, username, password, idKey, "movie", extension)
                }

                add(
                    VodItem(
                        id = if (isSeries) "xtream_series_$idKey" else "xtream_movie_$idKey",
                        title = item.optString("name").ifBlank { "Item $idKey" },
                        streamUrl = url,
                        posterUrl = item.optString("stream_icon").takeIf { it.isNotBlank() && it != "null" },
                        categoryId = categoryId,
                        categoryName = categoryName,
                        plot = item.optString("plot").ifBlank { null },
                        rating = item.optString("rating").ifBlank { null },
                        year = item.optString("year").ifBlank { null },
                        isSeries = isSeries
                    )
                )
            }
        }
    }

    private fun fetchShortEpg(
        server: String,
        username: String,
        password: String,
        streamId: String
    ): List<ProgramEntry> {
        val payload = safeReadJson(
            server, "player_api.php",
            mapOf(
                "username" to username,
                "password" to password,
                "action" to "get_short_epg",
                "stream_id" to streamId,
                "limit" to "2"
            )
        ) ?: return emptyList()

        val listings = payload.optJSONArray("epg_listings") ?: JSONArray()
        return buildList {
            for (index in 0 until listings.length()) {
                val item = listings.getJSONObject(index)
                add(
                    ProgramEntry(
                        title = item.optString("title").decodeBase64OrFallback(),
                        start = item.optString("start"),
                        end = item.optString("end"),
                        description = item.optString("description").decodeBase64OrFallback().takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private fun buildXtreamStreamUrl(
        server: String,
        username: String,
        password: String,
        streamId: String,
        pathSegment: String,
        extension: String
    ): String {
        val normalized = server.trim().trimEnd('/')
        return "$normalized/$pathSegment/$username/$password/$streamId.$extension"
    }

    private fun safeReadJson(server: String, path: String, query: Map<String, String>): JSONObject? =
        runCatching { JSONObject(readText(server, path, query)) }.getOrNull()

    private fun safeReadArray(server: String, path: String, query: Map<String, String>): JSONArray? =
        runCatching { JSONArray(readText(server, path, query)) }.getOrNull()

    private fun readJson(server: String, path: String, query: Map<String, String>): JSONObject {
        return JSONObject(readText(server, path, query))
    }

    private fun readText(server: String, path: String, query: Map<String, String>): String {
        val normalized = server.trim().trimEnd('/')
        val encodedQuery = query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val url = URL("$normalized/$path?$encodedQuery")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 18_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "IPTVPlayer/1.0")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }
}

private fun List<Channel>.categoriesFromGroups(): List<ChannelCategory> {
    val generated = this.map { it.group.orEmpty().ifBlank { "Other" } }
        .distinct()
        .sorted()
        .map { name -> ChannelCategory(id = name.lowercase().replace(" ", "_"), name = name) }

    return buildList {
        add(ChannelCategory("favorites", "Favorites"))
        add(ChannelCategory("all", "All Channels"))
        addAll(generated)
    }
}

private fun String.decodeBase64OrFallback(): String {
    return runCatching {
        val decoded = android.util.Base64.decode(this, android.util.Base64.DEFAULT)
        String(decoded).ifBlank { this }
    }.getOrElse { this }
}
