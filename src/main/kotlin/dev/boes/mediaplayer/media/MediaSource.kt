package dev.boes.mediaplayer.media

enum class MediaCommand {
    TOGGLE_PLAY_PAUSE,
    NEXT,
    PREVIOUS,
}

data class RawMedia(
    val appId: String,
    val title: String,
    val artist: String,
    val album: String,
    val status: PlaybackStatus,
    val positionMillis: Long,
    val durationMillis: Long,
    val positionSampledAt: Long,
) {
    val trackKey: String get() = listOf(appId, title, artist, album).joinToString("\u0000")
}

interface MediaSource : AutoCloseable {
    val isSupported: Boolean

    fun start()

    fun poll(): RawMedia?

    fun fetchArtwork(): ByteArray?

    fun execute(command: MediaCommand)

    fun seek(positionMillis: Long)

    fun volume(appId: String): AppVolume?

    fun setVolume(appId: String, level: Float)

    fun setMuted(appId: String, muted: Boolean)

    override fun close() {}
}

object UnsupportedMediaSource : MediaSource {
    override val isSupported = false
    override fun start() {}
    override fun poll(): RawMedia? = null
    override fun fetchArtwork(): ByteArray? = null
    override fun execute(command: MediaCommand) {}
    override fun seek(positionMillis: Long) {}
    override fun volume(appId: String): AppVolume? = null
    override fun setVolume(appId: String, level: Float) {}
    override fun setMuted(appId: String, muted: Boolean) {}
}
