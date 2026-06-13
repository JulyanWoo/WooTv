package com.wootv.app.data.remote

import com.wootv.app.data.local.entity.ChannelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UParser @Inject constructor() {

    companion object {
        private val REGEX_CACHE: Map<String, Regex> = mapOf(
            "tvg-id"      to Regex("""tvg-id\s*=\s*"([^"]*)""""),
            "tvg-name"    to Regex("""tvg-name\s*=\s*"([^"]*)""""),
            "tvg-logo"    to Regex("""tvg-logo\s*=\s*"([^"]*)""""),
            "group-title" to Regex("""group-title\s*=\s*"([^"]*)""""),
            "logo"        to Regex("""logo\s*=\s*"([^"]*)"""")
        )
    }

    suspend fun parse(file: File, playlistId: Long): List<ChannelEntity> = withContext(Dispatchers.Default) {
        val channels = mutableListOf<ChannelEntity>()
        // TODO(Phase 1): chunked Flow parsing para eliminar este cap y soportar listas 100k+
        val maxChannels = 5000

        try {
            file.bufferedReader().use { reader ->
                var currentLine: String? = reader.readLine()
                var nextLine: String? = null

                while (currentLine != null && channels.size < maxChannels) {
                    val line = currentLine.trim()

                    if (line.startsWith("#EXTINF:")) {
                        try {
                            val metadata = parseExtInf(line)
                            nextLine = reader.readLine()
                            var streamUrl: String? = null
                            var extGroupTitle: String? = null

                            // Scan forward to find the actual stream URL
                            while (nextLine != null) {
                                val nextLineTrimmed = nextLine.trim()
                                if (nextLineTrimmed.isNotBlank()) {
                                    if (!nextLineTrimmed.startsWith("#")) {
                                        streamUrl = nextLineTrimmed
                                        break
                                    } else if (nextLineTrimmed.startsWith("#EXTGRP:")) {
                                        val group = nextLineTrimmed.substringAfter("#EXTGRP:").trim()
                                        if (group.isNotEmpty()) {
                                            extGroupTitle = group
                                        }
                                    }
                                }
                                nextLine = reader.readLine()
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
                            }

                            // If we found a URL, currentLine becomes nextLine, otherwise read next
                            if (streamUrl != null) {
                                currentLine = nextLine
                            } else {
                                currentLine = reader.readLine()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            currentLine = reader.readLine()
                        }
                    } else {
                        currentLine = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        channels
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

    private fun extractAttribute(line: String, attribute: String): String? =
        REGEX_CACHE[attribute]?.find(line)?.groupValues?.get(1)?.ifBlank { null }
}
