package com.example.worldservicelive

import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val networkExecutor = Executors.newFixedThreadPool(2)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var resolving = false
    private var attemptedPlaybackFallback = false
    private var streamRequestId = 0
    private var scheduleRequestId = 0
    private var currentStation = RadioStations.all.first()

    private lateinit var stationSpinner: Spinner
    private lateinit var stationTitle: TextView
    private lateinit var stationSubtitle: TextView
    private lateinit var playbackBadge: View
    private lateinit var playbackIndicator: PlaybackIndicatorView
    private lateinit var playbackBadgeText: TextView
    private lateinit var playButton: Button
    private lateinit var statusText: TextView
    private lateinit var sourceText: TextView
    private lateinit var progress: View
    private lateinit var keepAwakeSwitch: Switch
    private lateinit var currentProgrammeTitle: TextView
    private lateinit var currentProgrammeTime: TextView
    private lateinit var currentProgrammeSynopsis: TextView
    private lateinit var scheduleProgress: ProgressBar
    private lateinit var scheduleStatus: TextView
    private lateinit var scheduleContainer: LinearLayout

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            renderPlayerState()
            applyKeepScreenOn()
        }

        override fun onPlaybackStateChanged(playbackState: Int) = renderPlayerState()

        override fun onPlayerError(error: PlaybackException) {
            val station = currentStation
            if (!attemptedPlaybackFallback && station.fallbackStreamUrl != null) {
                attemptedPlaybackFallback = true
                playResolvedStream(StreamResolver.fallback(station), station)
            } else {
                showError(getString(R.string.stream_unavailable))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stationSpinner = findViewById(R.id.station_spinner)
        stationTitle = findViewById(R.id.station_title)
        stationSubtitle = findViewById(R.id.station_subtitle)
        playbackBadge = findViewById(R.id.playback_badge)
        playbackIndicator = findViewById(R.id.playback_indicator)
        playbackBadgeText = findViewById(R.id.playback_badge_text)
        playButton = findViewById(R.id.play_button)
        statusText = findViewById(R.id.status_text)
        sourceText = findViewById(R.id.source_text)
        progress = findViewById(R.id.progress)
        keepAwakeSwitch = findViewById(R.id.keep_awake_switch)
        currentProgrammeTitle = findViewById(R.id.current_programme_title)
        currentProgrammeTime = findViewById(R.id.current_programme_time)
        currentProgrammeSynopsis = findViewById(R.id.current_programme_synopsis)
        scheduleProgress = findViewById(R.id.schedule_progress)
        scheduleStatus = findViewById(R.id.schedule_status)
        scheduleContainer = findViewById(R.id.schedule_container)

        setupStationPicker()
        setupKeepAwakeSetting()
        playButton.setOnClickListener { togglePlayback() }

        updateStationHeader()
        loadSchedule()
        connectToPlaybackService()
    }

    private fun setupStationPicker() {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        val savedStationId = preferences.getString(KEY_STATION_ID, null)
        val savedIndex = RadioStations.all.indexOfFirst { it.streamId == savedStationId }
            .takeIf { it >= 0 }
            ?: 0
        currentStation = RadioStations.all[savedIndex]

        stationSpinner.adapter = ArrayAdapter(
            this,
            R.layout.item_station_spinner,
            RadioStations.all,
        ).apply {
            setDropDownViewResource(R.layout.item_station_dropdown)
        }
        stationSpinner.setSelection(savedIndex, false)
        stationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val selected = RadioStations.all[position]
                if (selected != currentStation) changeStation(selected)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupKeepAwakeSetting() {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        keepAwakeSwitch.isChecked = preferences.getBoolean(KEY_KEEP_AWAKE, false)
        keepAwakeSwitch.setOnCheckedChangeListener { _, enabled ->
            preferences.edit { putBoolean(KEY_KEEP_AWAKE, enabled) }
            applyKeepScreenOn()
        }
    }

    private fun changeStation(station: RadioStation) {
        val resumePlayback = resolving || controller?.isPlaying == true
        streamRequestId++
        resolving = false
        attemptedPlaybackFallback = false
        currentStation = station
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit { putString(KEY_STATION_ID, station.streamId) }

        controller?.run {
            stop()
            clearMediaItems()
        }
        updateStationHeader()
        loadSchedule()
        if (resumePlayback) resolveAndPlay() else renderPlayerState()
    }

    private fun updateStationHeader() {
        stationTitle.setText(R.string.app_name)
        stationSubtitle.setText(R.string.app_tagline)
        sourceText.setText(R.string.privacy_tagline)
    }

    private fun connectToPlaybackService() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync().also { future ->
            future.addListener(
                {
                    runCatching { future.get() }
                        .onSuccess { connectedController ->
                            controller = connectedController
                            connectedController.addListener(playerListener)
                            playButton.isEnabled = true
                            renderPlayerState()
                            applyKeepScreenOn()
                        }
                        .onFailure { showError(getString(R.string.player_init_failed)) }
                },
                ContextCompat.getMainExecutor(this),
            )
        }
    }

    private fun togglePlayback() {
        val activeController = controller ?: return
        when {
            activeController.isPlaying -> activeController.pause()
            activeController.mediaItemCount > 0 -> activeController.play()
            else -> resolveAndPlay()
        }
    }

    private fun resolveAndPlay() {
        if (resolving) return
        val station = currentStation
        val requestId = ++streamRequestId
        resolving = true
        attemptedPlaybackFallback = false
        renderLoading(getString(R.string.resolving_station, station.name))

        networkExecutor.execute {
            val result = runCatching { StreamResolver.resolve(station) }
            runOnUiThread {
                if (isDestroyed || requestId != streamRequestId || station != currentStation) {
                    return@runOnUiThread
                }
                resolving = false
                result
                    .onSuccess { playResolvedStream(it, station) }
                    .onFailure { showError(getString(R.string.stream_unavailable)) }
            }
        }
    }

    private fun playResolvedStream(stream: ResolvedStream, station: RadioStation) {
        if (station != currentStation) return
        val activeController = controller ?: return
        sourceText.text = stream.sourceLabel
        attemptedPlaybackFallback = attemptedPlaybackFallback || stream.isFallback

        val mimeType = if (stream.url.contains(".m3u8", ignoreCase = true)) {
            MimeTypes.APPLICATION_M3U8
        } else {
            MimeTypes.APPLICATION_MPD
        }

        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .setMimeType(mimeType)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(12_000)
                    .build(),
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(getString(R.string.live_global_stream))
                    .setStation(station.name)
                    .build(),
            )
            .build()

        activeController.setMediaItem(mediaItem)
        activeController.prepare()
        activeController.play()
        renderPlayerState()
    }

    private fun loadSchedule() {
        val station = currentStation
        val requestId = ++scheduleRequestId
        scheduleContainer.removeAllViews()
        renderCurrentProgrammeLoading()
        scheduleProgress.visibility = View.VISIBLE
        scheduleStatus.visibility = View.VISIBLE
        scheduleStatus.setText(R.string.schedule_loading)

        networkExecutor.execute {
            val result = runCatching { ProgrammeResolver.fetch(station) }
            runOnUiThread {
                if (isDestroyed || requestId != scheduleRequestId || station != currentStation) {
                    return@runOnUiThread
                }
                scheduleProgress.visibility = View.GONE
                result
                    .onSuccess { entries ->
                        renderSchedule(entries)
                    }
                    .onFailure {
                        renderCurrentProgrammeUnavailable()
                        scheduleStatus.visibility = View.VISIBLE
                        scheduleStatus.setText(R.string.schedule_unavailable)
                    }
            }
        }
    }

    private fun renderSchedule(entries: List<ProgrammeEntry>) {
        scheduleContainer.removeAllViews()
        if (entries.isEmpty()) {
            renderCurrentProgramme(null)
            scheduleStatus.visibility = View.VISIBLE
            scheduleStatus.setText(R.string.schedule_empty)
            return
        }

        val now = System.currentTimeMillis()
        val current = entries.firstOrNull { entry ->
            entry.durationSeconds > 0 &&
                now >= entry.startTimeMillis && now < entry.endTimeMillis
        }
        val nextEntries = entries
            .asSequence()
            .filter { it.startTimeMillis > now }
            .sortedBy(ProgrammeEntry::startTimeMillis)
            .take(MAX_NEXT_PROGRAMMES)
            .toList()

        renderCurrentProgramme(current)
        if (nextEntries.isEmpty()) {
            scheduleStatus.visibility = View.VISIBLE
            scheduleStatus.setText(R.string.schedule_empty)
            return
        }

        scheduleStatus.visibility = View.GONE
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        nextEntries.forEach { entry ->
            val item = layoutInflater.inflate(R.layout.item_programme, scheduleContainer, false)
            val time = timeFormat.format(Date(entry.startTimeMillis))
            item.findViewById<TextView>(R.id.programme_time).text = time
            item.findViewById<TextView>(R.id.programme_title).text = entry.title
            item.findViewById<TextView>(R.id.programme_synopsis).apply {
                val summary = entry.synopsis
                visibility = if (summary.isNullOrBlank()) View.GONE else View.VISIBLE
                text = summary.orEmpty()
            }
            scheduleContainer.addView(item)
        }
    }

    private fun renderCurrentProgramme(entry: ProgrammeEntry?) {
        if (entry == null) {
            currentProgrammeTitle.setText(R.string.current_programme_unknown)
            currentProgrammeTime.text = null
            currentProgrammeSynopsis.visibility = View.GONE
            return
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentProgrammeTitle.text = entry.title
        currentProgrammeTime.text = getString(
            R.string.current_programme_time,
            timeFormat.format(Date(entry.startTimeMillis)),
            timeFormat.format(Date(entry.endTimeMillis)),
        )
        currentProgrammeSynopsis.apply {
            val summary = entry.synopsis
            visibility = if (summary.isNullOrBlank()) View.GONE else View.VISIBLE
            text = summary.orEmpty()
        }
    }

    private fun renderCurrentProgrammeLoading() {
        currentProgrammeTitle.setText(R.string.schedule_loading_short)
        currentProgrammeTime.setText(R.string.schedule_time_waiting)
        currentProgrammeSynopsis.visibility = View.GONE
    }

    private fun renderCurrentProgrammeUnavailable() {
        currentProgrammeTitle.setText(R.string.current_programme_unavailable)
        currentProgrammeTime.text = null
        currentProgrammeSynopsis.visibility = View.GONE
    }

    private fun renderPlayerState() {
        val activeController = controller
        val buffering = activeController?.playbackState == Player.STATE_BUFFERING

        progress.visibility = if (resolving || buffering) View.VISIBLE else View.INVISIBLE
        playButton.setText(if (activeController?.isPlaying == true) R.string.pause else R.string.play_live)
        playButton.isEnabled = activeController != null && !resolving

        statusText.text = when {
            resolving -> getString(R.string.status_resolving)
            buffering -> getString(R.string.status_buffering)
            activeController?.isPlaying == true -> getString(R.string.status_live)
            (activeController?.mediaItemCount ?: 0) > 0 -> getString(R.string.status_paused)
            else -> getString(R.string.status_ready)
        }

        when {
            resolving || buffering -> updatePlaybackBadge(
                R.string.playback_connecting,
                animate = false,
                color = COLOR_CONNECTING,
                background = R.drawable.bg_playback_connecting,
            )
            activeController?.isPlaying == true -> updatePlaybackBadge(
                R.string.playback_playing,
                animate = true,
                color = COLOR_PLAYING,
                background = R.drawable.bg_live_badge,
            )
            (activeController?.mediaItemCount ?: 0) > 0 -> updatePlaybackBadge(
                R.string.playback_paused,
                animate = false,
                color = COLOR_IDLE,
                background = R.drawable.bg_playback_idle,
            )
            else -> updatePlaybackBadge(
                R.string.playback_ready,
                animate = false,
                color = COLOR_IDLE,
                background = R.drawable.bg_playback_idle,
            )
        }
    }

    private fun renderLoading(message: String) {
        progress.visibility = View.VISIBLE
        playButton.isEnabled = false
        statusText.text = message
        sourceText.setText(R.string.privacy_tagline)
        updatePlaybackBadge(
            R.string.playback_connecting,
            animate = false,
            color = COLOR_CONNECTING,
            background = R.drawable.bg_playback_connecting,
        )
    }

    private fun showError(message: String) {
        resolving = false
        progress.visibility = View.INVISIBLE
        playButton.isEnabled = controller != null
        playButton.setText(R.string.retry)
        statusText.text = message
        sourceText.setText(R.string.check_bbc_access)
        updatePlaybackBadge(
            R.string.playback_unavailable,
            animate = false,
            color = COLOR_PLAYING,
            background = R.drawable.bg_live_badge,
        )
        applyKeepScreenOn()
    }

    private fun updatePlaybackBadge(
        label: Int,
        animate: Boolean,
        color: Int,
        background: Int,
    ) {
        playbackBadge.setBackgroundResource(background)
        playbackBadgeText.setText(label)
        playbackBadgeText.setTextColor(color)
        playbackIndicator.setIndicatorState(animate, color)
    }

    private fun applyKeepScreenOn() {
        val keepOn = keepAwakeSwitch.isChecked && controller?.isPlaying == true
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        streamRequestId++
        scheduleRequestId++
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        networkExecutor.shutdownNow()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    private companion object {
        const val PREFERENCES_NAME = "radio_settings"
        const val KEY_STATION_ID = "station_id"
        const val KEY_KEEP_AWAKE = "keep_awake_while_playing"
        const val MAX_NEXT_PROGRAMMES = 8
        val COLOR_PLAYING: Int = "#FF9B9B".toColorInt()
        val COLOR_CONNECTING: Int = "#C6BFFF".toColorInt()
        val COLOR_IDLE: Int = "#A9A8B6".toColorInt()
    }
}
