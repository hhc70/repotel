package com.example.iptvplayer

import java.util.UUID

object M3uParser {
    fun parse(content: String): List<Channel> {
        val lines = content.lines()
        val channels = mutableListOf<Channel>()

        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                currentName = line.substringAfterLast(",").trim().ifBlank { "Untitled channel" }
                currentLogo = extractAttribute(line, "tvg-logo")
                currentGroup = extractAttribute(line, "group-title")
            } else if (!line.startsWith("#")) {
                val group = currentGroup?.ifBlank { "Other" } ?: "Other"
                channels += Channel(
                    id = UUID.nameUUIDFromBytes((currentName.orEmpty() + line).toByteArray()).toString(),
                    name = currentName ?: line,
                    url = line,
                    logo = currentLogo,
                    group = group,
                    categoryId = group.lowercase().replace(" ", "_")
                )
                currentName = null
                currentLogo = null
                currentGroup = null
            }
        }
        return channels
    }

    private fun extractAttribute(line: String, name: String): String? {
        val regex = Regex("$name=\"([^\"]+)\"")
        return regex.find(line)?.groupValues?.getOrNull(1)
    }
}
