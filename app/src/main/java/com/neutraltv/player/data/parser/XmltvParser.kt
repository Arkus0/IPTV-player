package com.neutraltv.player.data.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream
import javax.inject.Inject

data class ParsedProgram(
    val channelId: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val category: String?
)

class XmltvParser @Inject constructor() {

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val dateFormatNoSpace = SimpleDateFormat("yyyyMMddHHmmssZ", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(inputStream: InputStream, isGzipped: Boolean = false): Sequence<List<ParsedProgram>> {
        val stream = if (isGzipped) GZIPInputStream(inputStream) else inputStream

        return sequence {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(stream.bufferedReader())

            val batch = mutableListOf<ParsedProgram>()
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "programme") {
                    val program = parseProgramme(parser)
                    if (program != null) {
                        batch.add(program)
                        if (batch.size >= BATCH_SIZE) {
                            yield(batch.toList())
                            batch.clear()
                        }
                    }
                }
                eventType = parser.next()
            }

            if (batch.isNotEmpty()) {
                yield(batch.toList())
            }
        }
    }

    private fun parseProgramme(parser: XmlPullParser): ParsedProgram? {
        val channelId = parser.getAttributeValue(null, "channel") ?: return null
        val startStr = parser.getAttributeValue(null, "start") ?: return null
        val stopStr = parser.getAttributeValue(null, "stop") ?: return null

        val startTime = parseDateTime(startStr) ?: return null
        val endTime = parseDateTime(stopStr) ?: return null

        var title: String? = null
        var description: String? = null
        var category: String? = null

        var depth = 1
        while (depth > 0) {
            val eventType = parser.next()
            when {
                eventType == XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "title" -> title = parser.nextText()
                        "desc" -> description = parser.nextText()
                        "category" -> category = parser.nextText()
                        else -> depth++
                    }
                }
                eventType == XmlPullParser.END_TAG -> {
                    if (parser.name == "programme") depth = 0
                    else depth--
                }
                eventType == XmlPullParser.END_DOCUMENT -> break
            }
        }

        if (title == null) return null

        return ParsedProgram(
            channelId = channelId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            category = category
        )
    }

    private fun parseDateTime(dateStr: String): Long? {
        return try {
            dateFormat.parse(dateStr)?.time
        } catch (_: Exception) {
            try {
                dateFormatNoSpace.parse(dateStr)?.time
            } catch (_: Exception) {
                null
            }
        }
    }

    companion object {
        const val BATCH_SIZE = 500
    }
}
