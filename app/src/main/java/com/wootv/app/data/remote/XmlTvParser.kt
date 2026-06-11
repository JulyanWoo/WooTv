package com.wootv.app.data.remote

import com.wootv.app.data.local.entity.EpgProgramEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XmlTvParser @Inject constructor() {

    fun parse(content: String, playlistId: Long): List<EpgProgramEntity> {
        val programs = mutableListOf<EpgProgramEntity>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(content))

        var eventType = parser.eventType
        var currentChannelId: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                        }
                        "title" -> {
                            // handled inside programme block
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    // text content
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            currentChannelId = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        // Re-parse with proper structure
        parsePrograms(content, playlistId, programs)
        return programs
    }

    private fun parsePrograms(content: String, playlistId: Long, programs: MutableList<EpgProgramEntity>) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(content))

        var eventType = parser.eventType
        var currentChannelId: String? = null
        var currentTitle: String? = null
        var currentDesc: String? = null
        var currentStart: Long? = null
        var currentEnd: Long? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            currentChannelId = parser.getAttributeValue(null, "channel")?.removePrefix("CH")
                            currentStart = parseXmltvTime(parser.getAttributeValue(null, "start"))
                            currentEnd = parseXmltvTime(parser.getAttributeValue(null, "stop"))
                            currentTitle = null
                            currentDesc = null
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "title" -> currentTitle = parser.text?.trim()
                        "desc" -> currentDesc = parser.text?.trim()
                        "programme" -> {
                            if (currentChannelId != null && currentTitle != null && currentStart != null && currentEnd != null) {
                                programs.add(
                                    EpgProgramEntity(
                                        channelId = currentChannelId,
                                        title = currentTitle,
                                        description = currentDesc,
                                        startTime = currentStart,
                                        endTime = currentEnd,
                                        playlistId = playlistId
                                    )
                                )
                            }
                            currentChannelId = null
                            currentTitle = null
                            currentDesc = null
                            currentStart = null
                            currentEnd = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseXmltvTime(time: String?): Long? {
        if (time == null) return null
        return try {
            val clean = time.replace(Regex("[^0-9]"), "")
            if (clean.length < 14) return null
            val dateStr = clean.substring(0, 14)
            java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
}
