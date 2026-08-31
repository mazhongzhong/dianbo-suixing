package com.example.worldservicelive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammeResolverTest {
    @Test
    fun `parses BBC schedule data and programme details`() {
        val json = """
            {
              "data": [
                {
                  "start": "2026-08-29T08:00:00Z",
                  "duration": 3600,
                  "titles": {"primary": "Radio 1 Breakfast"},
                  "synopses": {"short": "Music and guests."}
                },
                {
                  "start": "2026-08-29T09:00:00Z",
                  "duration": 1800,
                  "titles": {"primary": "Newsbeat"},
                  "synopses": {"short": "Latest news."}
                }
              ]
            }
        """.trimIndent()

        val entries = ProgrammeResolver.parse(json)

        assertEquals(2, entries.size)
        assertEquals("Radio 1 Breakfast", entries.first().title)
        assertEquals("Music and guests.", entries.first().synopsis)
        assertEquals(3_600, entries.first().durationSeconds)
    }

    @Test
    fun `returns current programme followed by upcoming programmes`() {
        val entries = listOf(
            ProgrammeEntry(1_000, 10, "Finished", null),
            ProgrammeEntry(20_000, 20, "On now", null),
            ProgrammeEntry(40_000, 20, "Next", null),
        )

        val upcoming = ProgrammeResolver.upcoming(entries, nowMillis = 25_000)

        assertEquals(listOf("On now", "Next"), upcoming.map(ProgrammeEntry::title))
    }

    @Test
    fun `major station identifiers are unique`() {
        val stations = RadioStations.all

        assertTrue(stations.size >= 7)
        assertEquals(stations.size, stations.map(RadioStation::streamId).distinct().size)
        assertEquals(stations.size, stations.map(RadioStation::scheduleId).distinct().size)
        assertTrue(stations.all { it.fallbackStreamUrl?.startsWith("https://") == true })
    }
}
