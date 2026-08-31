package com.example.worldservicelive

data class RadioStation(
    val streamId: String,
    val scheduleId: String,
    val name: String,
    val description: String,
    val webSlug: String = streamId,
    val fallbackStreamUrl: String? = null,
) {
    override fun toString(): String = name
}

object RadioStations {
    private const val WORLD_SERVICE_FALLBACK =
        "https://a.files.bbci.co.uk/ms6/live/3441A116-B12E-4D2F-ACA8-C1984642FA4B/" +
            "audio/simulcast/dash/nonuk/pc_hd_abr_v2/cfsgc/" +
            "bbc_world_service_news_internet.mpd"

    private fun worldwideHls(poolId: String, stationId: String): String =
        "https://as-hls-ww-live.akamaized.net/pool_$poolId/live/ww/$stationId/" +
            "$stationId.isml/$stationId-audio%3d96000.norewind.m3u8"

    val all = listOf(
        RadioStation(
            streamId = "bbc_world_service_news_internet",
            scheduleId = "bbc_world_service",
            name = "BBC World Service",
            description = "全球英语新闻与节目",
            webSlug = "bbc_world_service",
            fallbackStreamUrl = WORLD_SERVICE_FALLBACK,
        ),
        RadioStation(
            streamId = "bbc_radio_one",
            scheduleId = "bbc_radio_one",
            name = "BBC Radio 1",
            description = "流行音乐、新歌与娱乐",
            fallbackStreamUrl = worldwideHls("01505109", "bbc_radio_one"),
        ),
        RadioStation(
            streamId = "bbc_radio_two",
            scheduleId = "bbc_radio_two",
            name = "BBC Radio 2",
            description = "音乐、访谈与娱乐",
            fallbackStreamUrl = worldwideHls("74208725", "bbc_radio_two"),
        ),
        RadioStation(
            streamId = "bbc_radio_three",
            scheduleId = "bbc_radio_three",
            name = "BBC Radio 3",
            description = "古典、爵士与文化",
            fallbackStreamUrl = worldwideHls("23461179", "bbc_radio_three"),
        ),
        RadioStation(
            streamId = "bbc_radio_fourfm",
            scheduleId = "bbc_radio_fourfm",
            name = "BBC Radio 4",
            description = "新闻、纪录片与广播剧",
            fallbackStreamUrl = worldwideHls("55057080", "bbc_radio_fourfm"),
        ),
        RadioStation(
            streamId = "bbc_radio_five_live",
            scheduleId = "bbc_radio_five_live",
            name = "BBC Radio 5 Live",
            description = "新闻、评论与体育直播",
            fallbackStreamUrl = worldwideHls("89021708", "bbc_radio_five_live"),
        ),
        RadioStation(
            streamId = "bbc_6music",
            scheduleId = "bbc_6music",
            name = "BBC Radio 6 Music",
            description = "另类音乐与现场演出",
            fallbackStreamUrl = worldwideHls("81827798", "bbc_6music"),
        ),
    )
}
