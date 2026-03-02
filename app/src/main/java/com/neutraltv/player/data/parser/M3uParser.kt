package com.neutraltv.player.data.parser

data class ParsedChannel(
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupTitle: String?,
    val position: Int
)

class M3uParser {

    fun parse(content: String): List<ParsedChannel> {
        val lines = content.lines()
        if (lines.isEmpty()) return emptyList()

        val channels = mutableListOf<ParsedChannel>()
        var position = 0
        var currentInfo: ExtInfData? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() || trimmed == "#EXTM3U" -> continue

                trimmed.startsWith("#EXTINF:") -> {
                    currentInfo = parseExtInf(trimmed)
                }

                trimmed.startsWith("#") -> continue

                currentInfo != null -> {
                    if (isValidUrl(trimmed)) {
                        channels.add(
                            ParsedChannel(
                                name = currentInfo.name,
                                streamUrl = trimmed,
                                logoUrl = currentInfo.logoUrl,
                                groupTitle = currentInfo.groupTitle,
                                position = position++
                            )
                        )
                    }
                    currentInfo = null
                }

                else -> {
                    if (isValidUrl(trimmed)) {
                        channels.add(
                            ParsedChannel(
                                name = "Channel ${position + 1}",
                                streamUrl = trimmed,
                                logoUrl = null,
                                groupTitle = null,
                                position = position++
                            )
                        )
                    }
                }
            }
        }

        return channels
    }

    private fun parseExtInf(line: String): ExtInfData {
        val afterPrefix = line.substringAfter("#EXTINF:")

        val logoUrl = extractAttribute(afterPrefix, "tvg-logo")
        val groupTitle = extractAttribute(afterPrefix, "group-title")
        val tvgName = extractAttribute(afterPrefix, "tvg-name")

        val displayName = afterPrefix.substringAfterLast(",", "").trim()
        val name = when {
            displayName.isNotEmpty() -> displayName
            tvgName != null -> tvgName
            else -> "Unknown"
        }

        return ExtInfData(
            name = name,
            logoUrl = logoUrl?.takeIf { it.isNotBlank() },
            groupTitle = groupTitle?.takeIf { it.isNotBlank() }
        )
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = """$attribute="([^"]*)"""".toRegex()
        return pattern.find(line)?.groupValues?.get(1)
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true) ||
                url.startsWith("rtsp://", ignoreCase = true) ||
                url.startsWith("rtmp://", ignoreCase = true)
    }

    private data class ExtInfData(
        val name: String,
        val logoUrl: String?,
        val groupTitle: String?
    )
}
