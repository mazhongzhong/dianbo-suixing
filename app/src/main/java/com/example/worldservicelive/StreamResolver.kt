package com.example.worldservicelive

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ResolvedStream(
    val url: String,
    val sourceLabel: String,
    val isFallback: Boolean,
)

internal data class StreamCandidate(
    val url: String,
    val score: Int,
)

object StreamResolver {
    fun resolve(station: RadioStation = RadioStations.all.first()): ResolvedStream {
        for (selectorUrl in selectorUrls(station.streamId)) {
            runCatching {
                val response = fetchText(selectorUrl)
                val best = extractCandidates(response).maxByOrNull(StreamCandidate::score)
                    ?: error("BBC returned no playable stream")

                return ResolvedStream(
                    url = best.url,
                    sourceLabel = if (best.url.contains("nonuk") || best.url.contains("/ww/")) {
                        "${station.name} 全球直播源"
                    } else {
                        "${station.name} 公开直播源"
                    },
                    isFallback = false,
                )
            }
        }

        return fallback(station)
    }

    fun fallback(station: RadioStation = RadioStations.all.first()): ResolvedStream = ResolvedStream(
        url = station.fallbackStreamUrl ?: error("${station.name} currently has no fallback stream"),
        sourceLabel = "${station.name} 全球直播备用源",
        isFallback = true,
    )

    private fun selectorUrls(stationId: String) = listOf(
        "https://open.live.bbc.co.uk/mediaselector/6/select/version/3.0/" +
            "mediaset/pc/cvid/urn:bbc:pips:pid:$stationId/format/json/cors/1",
        "https://open.live.bbc.co.uk/mediaselector/6/select/version/2.0/" +
            "mediaset/pc/vpid/$stationId/format/json",
    )

    internal fun extractCandidates(rawJson: String): List<StreamCandidate> {
        val root = JSONTokener(rawJson).nextValue()
        val urls = linkedSetOf<String>()
        collectUrls(root, urls)
        return urls.map { StreamCandidate(it, score(it)) }
    }

    private fun collectUrls(value: Any?, output: MutableSet<String>) {
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                collectUrls(value.opt(key), output)
            }

            is JSONArray -> repeat(value.length()) { index ->
                collectUrls(value.opt(index), output)
            }

            is String -> {
                val normalized = value.lowercase(Locale.ROOT)
                if (
                    value.startsWith("https://") &&
                    (normalized.contains(".mpd") || normalized.contains(".m3u8"))
                ) {
                    output += value.replace("&amp;", "&")
                }
            }
        }
    }

    private fun score(url: String): Int {
        val normalized = url.lowercase(Locale.ROOT)
        return when {
            normalized.contains("geolocation") -> -1_000
            else -> 0
        } + when {
            normalized.contains("nonuk") -> 200
            normalized.contains("/ww/") -> 180
            else -> 0
        } + when {
            normalized.contains(".m3u8") -> 80
            normalized.contains(".mpd") -> 70
            else -> 0
        } + if (normalized.startsWith("https://")) 20 else 0
    }

    private fun fetchText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Referer", "https://www.bbc.com/audio/play/live")
            connection.setRequestProperty(
                "User-Agent",
                "WorldServiceLive/0.2 (Android; public BBC web stream)",
            )

            val status = connection.responseCode
            if (status !in 200..299) {
                error("BBC media selector returned HTTP $status")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
