package com.neutraltv.player.data.parser

data class M3uParseResult(
    val channels: List<ParsedChannel>,
    val epgUrl: String?
)

data class ParsedChannel(
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupTitle: String?,
    val position: Int,
    val tvgId: String? = null,
    val channelType: String = "live"
)

class M3uParser {

    fun parse(content: String): M3uParseResult {
        val lines = content.lines()
        if (lines.isEmpty()) return M3uParseResult(emptyList(), null)

        val channels = mutableListOf<ParsedChannel>()
        var position = 0
        var currentInfo: ExtInfData? = null
        var epgUrl: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> continue

                trimmed.startsWith("#EXTM3U") -> {
                    epgUrl = extractAttribute(trimmed, "url-tvg")
                        ?: extractAttribute(trimmed, "tvg-url")
                        ?: extractAttribute(trimmed, "x-tvg-url")
                    continue
                }

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
                                position = position++,
                                tvgId = currentInfo.tvgId,
                                channelType = classifyChannel(trimmed, currentInfo.groupTitle)
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
                                position = position++,
                                channelType = classifyChannel(trimmed, null)
                            )
                        )
                    }
                }
            }
        }

        return M3uParseResult(channels, epgUrl)
    }

    private fun parseExtInf(line: String): ExtInfData {
        val afterPrefix = line.substringAfter("#EXTINF:")

        val logoUrl = extractAttribute(afterPrefix, "tvg-logo")
        val groupTitle = extractAttribute(afterPrefix, "group-title")
        val tvgName = extractAttribute(afterPrefix, "tvg-name")
        val tvgId = extractAttribute(afterPrefix, "tvg-id")

        val displayName = afterPrefix.substringAfterLast(",", "").trim()
        val name = when {
            displayName.isNotEmpty() -> displayName
            tvgName != null -> tvgName
            else -> "Unknown"
        }

        // Fallback chain for EPG channel ID: tvg-id > tvg-name > sanitized display name
        val effectiveTvgId = tvgId?.takeIf { it.isNotBlank() }
            ?: tvgName?.takeIf { it.isNotBlank() }
            ?: name.takeIf { it != "Unknown" }?.lowercase()
                ?.replace(Regex("[^a-z0-9]"), "")
                ?.takeIf { it.isNotBlank() }

        return ExtInfData(
            name = name,
            logoUrl = logoUrl?.takeIf { it.isNotBlank() },
            groupTitle = groupTitle?.takeIf { it.isNotBlank() },
            tvgId = effectiveTvgId
        )
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = """$attribute="([^"]*)"""".toRegex()
        return pattern.find(line)?.groupValues?.get(1)
    }

    private fun classifyChannel(streamUrl: String, groupTitle: String?): String {
        val vodExtensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".flv")
        val urlLower = streamUrl.lowercase()
        if (vodExtensions.any { urlLower.substringBefore("?").endsWith(it) }) {
            return "vod"
        }

        val vodKeywords = listOf("vod", "movie", "películas", "peliculas", "series", "film")
        val groupLower = groupTitle?.lowercase() ?: ""
        if (vodKeywords.any { groupLower.contains(it) }) {
            return "vod"
        }

        return "live"
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
        val groupTitle: String?,
        val tvgId: String?
    )
}
