package com.wootv.app.data.remote

import com.wootv.app.data.local.entity.ChannelEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class M3UParserTest {

    private val parser = M3UParser()

    // R2.1 — companion object RegexCache has all 5 entries
    @Test
    fun regexCache_containsAllFiveAttributes() {
        // Companion object fields are static on the outer class in bytecode
        val outerClass = M3UParser::class.java
        val cacheField = outerClass.declaredFields.firstOrNull { it.name == "REGEX_CACHE" }
        assertNotNull("REGEX_CACHE field must exist", cacheField)
        cacheField!!.isAccessible = true
        val cache = cacheField.get(null) as Map<*, *>
        assertEquals("REGEX_CACHE must have 5 entries", 5, cache.size)
        assertTrue("must contain tvg-id", cache.containsKey("tvg-id"))
        assertTrue("must contain tvg-name", cache.containsKey("tvg-name"))
        assertTrue("must contain tvg-logo", cache.containsKey("tvg-logo"))
        assertTrue("must contain group-title", cache.containsKey("group-title"))
        assertTrue("must contain logo", cache.containsKey("logo"))
    }

    // R2.2 — 1,000 channels parse under 500ms
    @Test
    fun parse_1kChannels_under500ms() = runTest {
        val m3u = buildM3u(count = 1000)
        val tempFile = writeTempM3u(m3u)
        val start = System.currentTimeMillis()
        val channels = parser.parse(tempFile, playlistId = 1L)
        val elapsed = System.currentTimeMillis() - start
        assertTrue("Parse of 1k channels took ${elapsed}ms, expected < 500ms", elapsed < 500)
        assertEquals(1000, channels.size)
    }

    // R2.3 — Golden fixture matches expected
    @Test
    fun parse_goldenFixture_matchesExpected() = runTest {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="ch1" tvg-name="Channel One" tvg-logo="http://logo1.png" group-title="News",Channel One
            http://stream1.example.com/1.m3u8
            #EXTINF:-1 tvg-id="ch2" tvg-name="Channel Two" group-title="Sports",Channel Two
            http://stream2.example.com/2.m3u8
            #EXTINF:-1 tvg-id="ch3" tvg-name="Channel Three" group-title="Movies",Channel Three
            http://stream3.example.com/3.m3u8
        """.trimIndent()
        val tempFile = writeTempM3u(m3u)
        val channels = parser.parse(tempFile, playlistId = 42L)
        assertEquals(3, channels.size)
        assertEquals("Channel One", channels[0].name)
        assertEquals("News", channels[0].groupTitle)
        assertEquals("ch1", channels[0].tvgId)
        assertEquals("http://logo1.png", channels[0].tvgLogo)
        assertEquals("http://stream1.example.com/1.m3u8", channels[0].streamUrl)
        assertEquals(42L, channels[0].playlistId)
        assertEquals("Sports", channels[1].groupTitle)
        assertEquals("Movies", channels[2].groupTitle)
    }

    // R2.4 — Quoted comma in name preserved
    @Test
    fun parse_quotedCommaInName_preservesComma() = runTest {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="q1" tvg-name="Foo, Bar" group-title="Test",Foo, Bar
            http://stream.example.com/q.m3u8
        """.trimIndent()
        val tempFile = writeTempM3u(m3u)
        val channels = parser.parse(tempFile, playlistId = 1L)
        assertEquals(1, channels.size)
        assertEquals("Foo, Bar", channels[0].name)
        assertEquals("http://stream.example.com/q.m3u8", channels[0].streamUrl)
    }

    // R2.2/R9.2 — 5,000 channels parse under 2.5s
    @Test
    fun parse_5kChannels_under2_5s_returnsAll5000() = runTest {
        val m3u = buildM3u(count = 5000)
        val tempFile = writeTempM3u(m3u)
        val start = System.currentTimeMillis()
        val channels = parser.parse(tempFile, playlistId = 1L)
        val elapsed = System.currentTimeMillis() - start
        assertTrue("Parse of 5k channels took ${elapsed}ms, expected < 2500ms", elapsed < 2500)
        assertEquals(5000, channels.size)
    }

    // Helpers
    private fun buildM3u(count: Int): String = buildString {
        appendLine("#EXTM3U")
        for (i in 1..count) {
            appendLine("#EXTINF:-1 tvg-id=\"ch$i\" tvg-name=\"Channel $i\" tvg-logo=\"http://logo.example.com/$i.png\" group-title=\"Category${i % 10}\",Channel $i")
            appendLine("http://stream.example.com/$i.m3u8")
        }
    }

    private fun writeTempM3u(content: String): File {
        val tempFile = File.createTempFile("m3u-test-", ".m3u")
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use { it.write(content.toByteArray()) }
        return tempFile
    }
}
