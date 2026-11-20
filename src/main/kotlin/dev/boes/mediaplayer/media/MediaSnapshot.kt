package dev.boes.mediaplayer.media

enum class PlaybackStatus {
    CLOSED,
    OPENED,
    CHANGING,
    STOPPED,
    PLAYING,
    PAUSED;

    val isPlaying: Boolean get() = this == PLAYING

    companion object {
        fun fromWinRt(value: Int): PlaybackStatus = entries.getOrElse(value) { STOPPED }
    }
}

data class AppVolume(val level: Float, val muted: Boolean)

class Artwork(val key: String, val size: Int, val pixels: IntArray)

data class MediaSnapshot(
    val appId: String,
    val title: String,
    val artist: String,
    val album: String,
    val status: PlaybackStatus,
    val positionMillis: Long,
    val durationMillis: Long,
    val positionSampledAt: Long,
    val trackKey: String,
    val artwork: Artwork?,
    val volume: AppVolume?,
) {
    val hasTimeline: Boolean get() = durationMillis > 0

    fun positionAt(nowMillis: Long): Long {
        if (!hasTimeline) return 0
        val elapsed = if (status.isPlaying) (nowMillis - positionSampledAt).coerceAtLeast(0) else 0
        return (positionMillis + elapsed).coerceIn(0, durationMillis)
    }

    fun progressAt(nowMillis: Long): Float =
        if (hasTimeline) positionAt(nowMillis).toFloat() / durationMillis else 0f

    val appName: String
        get() {
            val packaged = appId.substringAfterLast('!', "")
            val base = packaged.ifEmpty { appId.substringAfterLast('\\').removeSuffix(".exe").removeSuffix(".EXE") }
            return base.replaceFirstChar { it.uppercaseChar() }
        }
}
