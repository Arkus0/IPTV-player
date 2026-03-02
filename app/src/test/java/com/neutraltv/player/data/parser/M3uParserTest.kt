package com.neutraltv.player.data.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class M3uParserTest {

    private lateinit var parser: M3uParser

    @Before
    fun setup() {
        parser = M3uParser()
    }

    @Test
    fun `parse valid M3U with all attributes`() {
        val content = """
            #EXTM3U url-tvg="http://epg.example.com/guide.xml"
            #EXTINF:-1 tvg-id="ch1" tvg-name="Channel 1" tvg-logo="http://logo.com/1.png" group-title="Sports",Channel One
            http://stream.example.com/ch1.m3u8
            #EXTINF:-1 tvg-id="ch2" tvg-name="Channel 2" tvg-logo="http://logo.com/2.png" group-title="News",Channel Two
            http://stream.example.com/ch2.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.epgUrl).isEqualTo("http://epg.example.com/guide.xml")
        assertThat(result.channels).hasSize(2)

        val ch1 = result.channels[0]
        assertThat(ch1.name).isEqualTo("Channel One")
        assertThat(ch1.streamUrl).isEqualTo("http://stream.example.com/ch1.m3u8")
        assertThat(ch1.logoUrl).isEqualTo("http://logo.com/1.png")
        assertThat(ch1.groupTitle).isEqualTo("Sports")
        assertThat(ch1.tvgId).isEqualTo("ch1")
        assertThat(ch1.position).isEqualTo(0)

        val ch2 = result.channels[1]
        assertThat(ch2.name).isEqualTo("Channel Two")
        assertThat(ch2.tvgId).isEqualTo("ch2")
        assertThat(ch2.position).isEqualTo(1)
    }

    @Test
    fun `parse M3U without EXTM3U header`() {
        val content = """
            #EXTINF:-1,Simple Channel
            http://stream.example.com/simple.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.epgUrl).isNull()
        assertThat(result.channels).hasSize(1)
        assertThat(result.channels[0].name).isEqualTo("Simple Channel")
    }

    @Test
    fun `parse empty content`() {
        val result = parser.parse("")
        assertThat(result.channels).isEmpty()
        assertThat(result.epgUrl).isNull()
    }

    @Test
    fun `parse M3U with bare URLs without EXTINF`() {
        val content = """
            #EXTM3U
            http://stream.example.com/ch1.m3u8
            http://stream.example.com/ch2.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.channels).hasSize(2)
        assertThat(result.channels[0].name).isEqualTo("Channel 1")
        assertThat(result.channels[1].name).isEqualTo("Channel 2")
    }

    @Test
    fun `parse M3U with malformed EXTINF - missing comma`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-name="Test"
            http://stream.example.com/ch1.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.channels).hasSize(1)
        // When there's no comma, substringAfterLast returns empty, falls back to tvg-name
        assertThat(result.channels[0].name).isEqualTo("Test")
    }

    @Test
    fun `parse M3U with mixed valid and invalid URLs`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Valid Channel
            http://stream.example.com/valid.m3u8
            #EXTINF:-1,Invalid Channel
            not-a-url
            #EXTINF:-1,Another Valid
            https://stream.example.com/valid2.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.channels).hasSize(2)
        assertThat(result.channels[0].name).isEqualTo("Valid Channel")
        assertThat(result.channels[1].name).isEqualTo("Another Valid")
        // Positions should be sequential (0, 1) not (0, 2)
        assertThat(result.channels[0].position).isEqualTo(0)
        assertThat(result.channels[1].position).isEqualTo(1)
    }

    @Test
    fun `parse M3U with RTSP and RTMP URLs`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,RTSP Stream
            rtsp://stream.example.com/live
            #EXTINF:-1,RTMP Stream
            rtmp://stream.example.com/live/key
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.channels).hasSize(2)
        assertThat(result.channels[0].streamUrl).startsWith("rtsp://")
        assertThat(result.channels[1].streamUrl).startsWith("rtmp://")
    }

    @Test
    fun `parse M3U with whitespace and blank lines`() {
        val content = """
            #EXTM3U

            #EXTINF:-1,  Channel With Spaces
              http://stream.example.com/ch1.m3u8

            #EXTINF:-1,Channel 2
            http://stream.example.com/ch2.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.channels).hasSize(2)
        assertThat(result.channels[0].name).isEqualTo("Channel With Spaces")
        assertThat(result.channels[0].streamUrl).isEqualTo("http://stream.example.com/ch1.m3u8")
    }

    @Test
    fun `parse M3U with no attributes returns null fields`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Plain Channel
            http://stream.example.com/plain.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.channels).hasSize(1)
        val ch = result.channels[0]
        assertThat(ch.name).isEqualTo("Plain Channel")
        assertThat(ch.logoUrl).isNull()
        assertThat(ch.groupTitle).isNull()
        assertThat(ch.tvgId).isNull()
    }

    @Test
    fun `parse M3U extracts url-tvg from EXTM3U header`() {
        val content = """
            #EXTM3U url-tvg="http://epg.test.com/guide.xml.gz"
            #EXTINF:-1,Test
            http://stream.test.com/live.m3u8
        """.trimIndent()

        val result = parser.parse(content)
        assertThat(result.epgUrl).isEqualTo("http://epg.test.com/guide.xml.gz")
    }

    @Test
    fun `parse M3U without url-tvg returns null epgUrl`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Test
            http://stream.test.com/live.m3u8
        """.trimIndent()

        val result = parser.parse(content)
        assertThat(result.epgUrl).isNull()
    }

    @Test
    fun `parse extracts tvg-id from EXTINF`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="ESPN.us",ESPN
            http://stream.test.com/espn.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.channels).hasSize(1)
        assertThat(result.channels[0].tvgId).isEqualTo("ESPN.us")
    }

    @Test
    fun `parse EXTINF with no name after comma falls back to tvg-name`() {
        val content = """
            #EXTINF:-1 tvg-name="Fallback Name",
            http://stream.test.com/live.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.channels).hasSize(1)
        assertThat(result.channels[0].name).isEqualTo("Fallback Name")
    }

    @Test
    fun `parse EXTINF with no name and no tvg-name returns Unknown`() {
        val content = """
            #EXTINF:-1,
            http://stream.test.com/live.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.channels).hasSize(1)
        assertThat(result.channels[0].name).isEqualTo("Unknown")
    }

    @Test
    fun `parse skips comment lines`() {
        val content = """
            #EXTM3U
            # This is a comment
            #EXTINF:-1,Test Channel
            # Another comment
            http://stream.test.com/live.m3u8
        """.trimIndent()

        val result = parser.parse(content)
        assertThat(result.channels).hasSize(1)
    }

    @Test
    fun `parse large playlist with many channels`() {
        val builder = StringBuilder("#EXTM3U\n")
        for (i in 1..100) {
            builder.append("#EXTINF:-1 tvg-id=\"ch$i\" group-title=\"Group ${i % 5}\",Channel $i\n")
            builder.append("http://stream.example.com/ch$i.m3u8\n")
        }

        val result = parser.parse(builder.toString())

        assertThat(result.channels).hasSize(100)
        assertThat(result.channels.last().position).isEqualTo(99)
        assertThat(result.channels.last().name).isEqualTo("Channel 100")
    }
}
