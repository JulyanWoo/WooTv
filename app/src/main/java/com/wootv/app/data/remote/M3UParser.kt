package com.wootv.app.data.remote

import com.wootv.app.data.local.entity.ChannelEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UParser @Inject constructor() {

    fun parse(content: String, playlistId: Long): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val lines = content.lines()
        var index = 0

        while (index < lines.size) {
            val line = lines[index].trim()
            if (line.startsWith("#EXTINF:")) {
                val metadata = parseExtInf(line)
                var nextIndex = index + 1
                var streamUrl: String? = null
                var extGroupTitle: String? = null

                // Scan forward to find the actual stream URL, skipping other tags and blank lines
                while (nextIndex < lines.size) {
                    val nextLine = lines[nextIndex].trim()
                    if (nextLine.isNotBlank()) {
                        if (!nextLine.startsWith("#")) {
                            streamUrl = nextLine
                            break
                        } else if (nextLine.startsWith("#EXTGRP:")) {
                            val group = nextLine.substringAfter("#EXTGRP:").trim()
                            if (group.isNotEmpty()) {
                                extGroupTitle = group
                            }
                        }
                    }
                    nextIndex++
                }

                if (streamUrl != null) {
                    channels.add(
                        ChannelEntity(
                            playlistId = playlistId,
                            name = metadata.name,
                            streamUrl = streamUrl,
                            logoUrl = metadata.logoUrl,
                            groupTitle = metadata.groupTitle ?: extGroupTitle,
                            tvgId = metadata.tvgId,
                            tvgName = metadata.tvgName,
                            tvgLogo = metadata.tvgLogo
                        )
                    )
                    // Advance index to the position of the URL so the outer loop increments correctly
                    index = nextIndex
                }
            }
            index++
        }
        return channels
    }

    private data class ExtInfMetadata(
        val tvgId: String?,
        val tvgName: String?,
        val tvgLogo: String?,
        val groupTitle: String?,
        val logoUrl: String?,
        val name: String
    )

    private fun parseExtInf(line: String): ExtInfMetadata {
        val tvgId = extractAttribute(line, "tvg-id")
        val tvgName = extractAttribute(line, "tvg-name")
        val tvgLogo = extractAttribute(line, "tvg-logo")
        val groupTitle = extractAttribute(line, "group-title")
        val logoUrl = extractAttribute(line, "logo")

        // Find the first comma that is NOT inside double quotes to separate attributes from the channel name
        var commaIndex = -1
        var inQuotes = false
        for (i in 0 until line.length) {
            val char = line[i]
            if (char == '"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                commaIndex = i
                break
            }
        }

        val name = if (commaIndex >= 0) line.substring(commaIndex + 1).trim() else "Unknown"

        return ExtInfMetadata(
            tvgId = tvgId,
            tvgName = tvgName,
            tvgLogo = tvgLogo,
            groupTitle = groupTitle,
            logoUrl = logoUrl,
            name = name
        )
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val regex = """$attribute\s*=\s*"([^"]*)"""".toRegex()
        return regex.find(line)?.groupValues?.get(1)?.ifBlank { null }
    }
}
