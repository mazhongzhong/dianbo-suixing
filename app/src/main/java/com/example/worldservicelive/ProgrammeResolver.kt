package com.example.worldservicelive

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ProgrammeEntry(
    val startTimeMillis: Long,
    val durationSeconds: Int,
    val title: String,
    val synopsis: String?,
) {
    val endTimeMillis: Long
        get() = startTimeMillis + durationSeconds.coerceAtLeast(0) * 1_000L
}

object ProgrammeResolver {
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK).apply {
        timeZone = TimeZone.getTimeZone("Europe/London")
    }

    fun fetch(station: RadioStation): List<ProgrammeEntry> {
        val date = synchronized(apiDateFormat) { apiDateFormat.format(Date()) }
        val url =
            "https://rms.api.bbc.co.uk/v2/broadcasts/schedules/${station.scheduleId}/$date"
        return parse(fetchText(url, station)).sortedBy(ProgrammeEntry::startTimeMillis)
    }

    internal fun parse(rawJson: String): List<ProgrammeEntry> {
        val root = JSONObject(rawJson)
        val broadcasts = root.optJSONArray("data")
            ?: root.optJSONArray("results")
            ?: JSONArray()

        return buildList {
            repeat(broadcasts.length()) { index ->
                val item = broadcasts.optJSONObject(index) ?: return@repeat
                val start = parseTime(item.optString("start")) ?: return@repeat
                val title = item.optJSONObject("titles")
                    ?.optString("primary")
                    ?.takeIf(String::isNotBlank)
                    ?: item.optJSONObject("programme")
                        ?.optJSONObject("titles")
                        ?.optString("primary")
                        ?.takeIf(String::isNotBlank)
                    ?: return@repeat
                val synopsisObject = item.optJSONObject("synopses")
                    ?: item.optJSONObject("programme")?.optJSONObject("synopses")
                val synopsis = sequenceOf("short", "medium", "long")
                    .mapNotNull { key -> synopsisObject?.optString(key)?.takeIf(String::isNotBlank) }
                    .firstOrNull()

                add(
                    ProgrammeEntry(
                        startTimeMillis = start,
                        durationSeconds = item.optInt("duration", 0),
                        title = title,
                        synopsis = synopsis,
                    ),
                )
            }
        }
    }

    internal fun upcoming(
        entries: List<ProgrammeEntry>,
        nowMillis: Long,
        limit: Int = 8,
    ): List<ProgrammeEntry> {
        if (entries.isEmpty()) return emptyList()
        val firstRelevant = entries.indexOfFirst { it.endTimeMillis > nowMillis }
            .takeIf { it >= 0 }
            ?: 0
        return entries.drop(firstRelevant).take(limit)
    }

    private fun parseTime(value: String): Long? {
        if (value.isBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.UK).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }.parse(value)?.time
            }.getOrNull()
        }
    }

    private fun fetchText(url: String, station: RadioStation): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "Referer",
                "https://www.bbc.co.uk/sounds/schedules/${station.scheduleId}",
            )
            connection.setRequestProperty(
                "User-Agent",
                "WorldServiceLive/0.2 (Android; public BBC programme schedule)",
            )
            val status = connection.responseCode
            if (status !in 200..299) error("BBC schedule returned HTTP $status")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
