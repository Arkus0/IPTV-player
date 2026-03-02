package com.neutraltv.player.data.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class XmltvParserTest {

    private lateinit var parser: XmltvParser

    @Before
    fun setUp() {
        parser = XmltvParser()
    }

    @Test
    fun `parse valid XMLTV with single programme`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="20240315120000 +0000" stop="20240315130000 +0000" channel="channel1">
                    <title>Test Show</title>
                    <desc>A test description</desc>
                    <category>News</category>
                </programme>
            </tv>
        """.trimIndent()

        val programs = parser.parse(xml.byteInputStream()).flatMap { it }.toList()

        assertThat(programs).hasSize(1)
        assertThat(programs[0].channelId).isEqualTo("channel1")
        assertThat(programs[0].title).isEqualTo("Test Show")
        assertThat(programs[0].description).isEqualTo("A test description")
        assertThat(programs[0].category).isEqualTo("News")
    }

    @Test
    fun `parse multiple programmes for same channel`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="20240315120000 +0000" stop="20240315130000 +0000" channel="ch1">
                    <title>Show 1</title>
                </programme>
                <programme start="20240315130000 +0000" stop="20240315140000 +0000" channel="ch1">
                    <title>Show 2</title>
                </programme>
                <programme start="20240315120000 +0000" stop="20240315140000 +0000" channel="ch2">
                    <title>Other Channel Show</title>
                </programme>
            </tv>
        """.trimIndent()

        val programs = parser.parse(xml.byteInputStream()).flatMap { it }.toList()

        assertThat(programs).hasSize(3)
        assertThat(programs.filter { it.channelId == "ch1" }).hasSize(2)
        assertThat(programs.filter { it.channelId == "ch2" }).hasSize(1)
    }

    @Test
    fun `parse programme without optional fields`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="20240315120000 +0000" stop="20240315130000 +0000" channel="ch1">
                    <title>Minimal Show</title>
                </programme>
            </tv>
        """.trimIndent()

        val programs = parser.parse(xml.byteInputStream()).flatMap { it }.toList()

        assertThat(programs).hasSize(1)
        assertThat(programs[0].title).isEqualTo("Minimal Show")
        assertThat(programs[0].description).isNull()
        assertThat(programs[0].category).isNull()
    }

    @Test
    fun `skip programme without title`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="20240315120000 +0000" stop="20240315130000 +0000" channel="ch1">
                    <desc>No title here</desc>
                </programme>
            </tv>
        """.trimIndent()

        val programs = parser.parse(xml.byteInputStream()).flatMap { it }.toList()

        assertThat(programs).isEmpty()
    }

    @Test
    fun `skip programme without channel attribute`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="20240315120000 +0000" stop="20240315130000 +0000">
                    <title>No Channel</title>
                </programme>
            </tv>
        """.trimIndent()

        val programs = parser.parse(xml.byteInputStream()).flatMap { it }.toList()

        assertThat(programs).isEmpty()
    }

    @Test
    fun `parse date format without space before timezone`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="20240315120000+0000" stop="20240315130000+0000" channel="ch1">
                    <title>No Space TZ</title>
                </programme>
            </tv>
        """.trimIndent()

        val programs = parser.parse(xml.byteInputStream()).flatMap { it }.toList()

        assertThat(programs).hasSize(1)
        assertThat(programs[0].title).isEqualTo("No Space TZ")
    }

    @Test
    fun `parse empty document`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv></tv>
        """.trimIndent()

        val programs = parser.parse(xml.byteInputStream()).flatMap { it }.toList()

        assertThat(programs).isEmpty()
    }

    @Test
    fun `parse gzipped content`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="20240315120000 +0000" stop="20240315130000 +0000" channel="ch1">
                    <title>Gzipped Show</title>
                </programme>
            </tv>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(xml.toByteArray()) }
        val gzippedInput = ByteArrayInputStream(baos.toByteArray())

        val programs = parser.parse(gzippedInput, isGzipped = true).flatMap { it }.toList()

        assertThat(programs).hasSize(1)
        assertThat(programs[0].title).isEqualTo("Gzipped Show")
    }

    @Test
    fun `batch size yields correct batches`() {
        val programsXml = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("<tv>")
            repeat(XmltvParser.BATCH_SIZE + 10) { i ->
                appendLine("""<programme start="20240315120000 +0000" stop="20240315130000 +0000" channel="ch$i"><title>Show $i</title></programme>""")
            }
            appendLine("</tv>")
        }

        val batches = parser.parse(programsXml.byteInputStream()).toList()

        assertThat(batches).hasSize(2)
        assertThat(batches[0]).hasSize(XmltvParser.BATCH_SIZE)
        assertThat(batches[1]).hasSize(10)
    }

    @Test
    fun `parse timestamps correctly`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="20240315120000 +0000" stop="20240315130000 +0000" channel="ch1">
                    <title>Time Test</title>
                </programme>
            </tv>
        """.trimIndent()

        val programs = parser.parse(xml.byteInputStream()).flatMap { it }.toList()
        val program = programs[0]

        // 1 hour difference = 3600000ms
        assertThat(program.endTime - program.startTime).isEqualTo(3600_000L)
    }

    @Test
    fun `skip programme with invalid start time`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="invalid" stop="20240315130000 +0000" channel="ch1">
                    <title>Bad Start</title>
                </programme>
            </tv>
        """.trimIndent()

        val programs = parser.parse(xml.byteInputStream()).flatMap { it }.toList()

        assertThat(programs).isEmpty()
    }
}
