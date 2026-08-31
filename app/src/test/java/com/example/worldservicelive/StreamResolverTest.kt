package com.example.worldservicelive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamResolverTest {
    @Test
    fun `extracts nested manifests and prefers worldwide stream`() {
        val json = """
            {
              "media": [
                {"connection": [{"href": "https://example.test/uk/live.mpd"}]},
                {"connection": [{"href": "https://example.test/nonuk/live.mpd"}]},
                {"connection": [{"href": "https://example.test/ww/live.m3u8"}]}
              ]
            }
        """.trimIndent()

        val candidates = StreamResolver.extractCandidates(json)
        val best = candidates.maxByOrNull(StreamCandidate::score)

        assertEquals(3, candidates.size)
        assertEquals("https://example.test/nonuk/live.mpd", best?.url)
    }

    @Test
    fun `ignores non media urls and decodes escaped ampersands`() {
        val json = """
            {
              "metadata": "https://example.test/data.json",
              "href": "https://example.test/ww/live.m3u8?token=a&amp;quality=96"
            }
        """.trimIndent()

        val candidates = StreamResolver.extractCandidates(json)

        assertEquals(1, candidates.size)
        assertTrue(candidates.single().url.contains("&quality=96"))
    }
}
