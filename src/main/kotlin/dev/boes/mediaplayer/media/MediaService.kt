package dev.boes.mediaplayer.media

import dev.boes.mediaplayer.MediaPlayer
import dev.boes.mediaplayer.media.windows.WindowsMediaSource
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object MediaService {
    private const val POLL_INTERVAL_MILLIS = 500L
    private const val REFRESH_DELAY_MILLIS = 150L
    private const val ARTWORK_RETRY_MILLIS = 2000L
    private const val MAX_CONSECUTIVE_FAILURES = 10
    private const val VOLUME_STEP = 0.05f

    @Volatile
    var snapshot: MediaSnapshot? = null
        private set

    @Volatile
    var isSupported: Boolean = false
        private set

    private var executor: ScheduledExecutorService? = null
    private var source: MediaSource = UnsupportedMediaSource
    private var artwork: Artwork? = null
    private var artworkAttemptKey: String? = null
    private var artworkAttemptAt = 0L
    private var consecutiveFailures = 0
    private var loggedFailure = false
    private val pendingVolume = AtomicReference<Float?>(null)

    fun start() {
        if (executor != null) return
        source = createSource()
        val service = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "MediaPlayer Media Bridge").apply { isDaemon = true }
        }
        executor = service
        service.execute(::startSource)
        service.scheduleWithFixedDelay(::poll, POLL_INTERVAL_MILLIS, POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        val service = executor ?: return
        executor = null
        service.execute { runCatching { source.close() } }
        service.shutdown()
    }

    fun togglePlayPause() = command(MediaCommand.TOGGLE_PLAY_PAUSE) { current ->
        val now = System.currentTimeMillis()
        val status = if (current.status.isPlaying) PlaybackStatus.PAUSED else PlaybackStatus.PLAYING
        current.copy(status = status, positionMillis = current.positionAt(now), positionSampledAt = now)
    }

    fun next() = command(MediaCommand.NEXT)

    fun previous() = command(MediaCommand.PREVIOUS)

    fun seek(positionMillis: Long) = submit {
        val current = snapshot ?: return@submit
        val target = positionMillis.coerceIn(0, current.durationMillis)
        source.seek(target)
        snapshot = current.copy(positionMillis = target, positionSampledAt = System.currentTimeMillis())
        scheduleRefresh()
    }

    fun setVolume(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        snapshot?.let { current -> snapshot = current.copy(volume = AppVolume(clamped, false)) }
        if (pendingVolume.getAndSet(clamped) != null) return
        submit {
            val value = pendingVolume.getAndSet(null) ?: return@submit
            val appId = snapshot?.appId ?: return@submit
            source.setVolume(appId, value)
        }
    }

    fun adjustVolume(direction: Int) {
        val volume = snapshot?.volume ?: return
        setVolume(volume.level + direction * VOLUME_STEP)
    }

    fun toggleMute() = submit {
        val current = snapshot ?: return@submit
        val volume = current.volume ?: return@submit
        source.setMuted(current.appId, !volume.muted)
        snapshot = current.copy(volume = volume.copy(muted = !volume.muted))
        scheduleRefresh()
    }

    private fun command(command: MediaCommand, optimistic: ((MediaSnapshot) -> MediaSnapshot)? = null) = submit {
        source.execute(command)
        val current = snapshot
        if (current != null && optimistic != null) snapshot = optimistic(current)
        scheduleRefresh()
    }

    private fun submit(action: () -> Unit) {
        executor?.execute {
            try {
                action()
            } catch (throwable: Throwable) {
                MediaPlayer.LOGGER.debug("Media command failed", throwable)
            }
        }
    }

    private fun scheduleRefresh() {
        executor?.schedule(::poll, REFRESH_DELAY_MILLIS, TimeUnit.MILLISECONDS)
    }

    private fun startSource() {
        try {
            source.start()
            isSupported = source.isSupported
        } catch (throwable: Throwable) {
            isSupported = false
            MediaPlayer.LOGGER.error("Could not connect to the system media controls", throwable)
            source = UnsupportedMediaSource
        }
    }

    private fun poll() {
        if (!isSupported) return
        try {
            val raw = source.poll()
            snapshot = raw?.let(::buildSnapshot)
            consecutiveFailures = 0
            loggedFailure = false
        } catch (throwable: Throwable) {
            consecutiveFailures++
            if (!loggedFailure) {
                loggedFailure = true
                MediaPlayer.LOGGER.warn("Reading the current media session failed", throwable)
            }
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) restartSource()
        }
    }

    private fun buildSnapshot(raw: RawMedia): MediaSnapshot {
        val key = raw.trackKey
        if (artwork?.key != key) {
            artwork = null
            val now = System.currentTimeMillis()
            if (artworkAttemptKey != key || now - artworkAttemptAt > ARTWORK_RETRY_MILLIS) {
                artworkAttemptKey = key
                artworkAttemptAt = now
                artwork = runCatching { source.fetchArtwork()?.let { ArtworkDecoder.decode(key, it) } }
                    .onFailure { MediaPlayer.LOGGER.debug("Could not load album art", it) }
                    .getOrNull()
            }
        }
        val volume = runCatching { source.volume(raw.appId) }.getOrNull()
        return MediaSnapshot(
            appId = raw.appId,
            title = raw.title,
            artist = raw.artist,
            album = raw.album,
            status = raw.status,
            positionMillis = raw.positionMillis,
            durationMillis = raw.durationMillis,
            positionSampledAt = raw.positionSampledAt,
            trackKey = key,
            artwork = artwork,
            volume = volume,
        )
    }

    private fun restartSource() {
        consecutiveFailures = 0
        runCatching { source.close() }
        source = createSource()
        startSource()
    }

    private fun createSource(): MediaSource = if (isWindows()) WindowsMediaSource() else UnsupportedMediaSource

    private fun isWindows(): Boolean = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
}
