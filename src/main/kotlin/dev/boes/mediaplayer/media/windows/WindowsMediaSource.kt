package dev.boes.mediaplayer.media.windows

import com.sun.jna.Memory
import com.sun.jna.platform.win32.Guid.GUID
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import dev.boes.mediaplayer.media.AppVolume
import dev.boes.mediaplayer.media.MediaCommand
import dev.boes.mediaplayer.media.MediaSource
import dev.boes.mediaplayer.media.PlaybackStatus
import dev.boes.mediaplayer.media.RawMedia
import java.io.ByteArrayOutputStream

internal class WindowsMediaSource : MediaSource {
    override val isSupported = true

    private var manager: ComObject? = null
    private val audioSessions = WindowsAudioSessions()

    override fun start() {
        WinRt.initializeThread()
        manager = requestManager()
        audioSessions.start()
    }

    private fun requestManager(): ComObject =
        WinRt.activationFactory(MANAGER_CLASS, IID_MANAGER_STATICS).use { statics ->
            statics.getObject(STATICS_REQUEST_ASYNC)!!.use { operation ->
                WinRt.awaitObject(operation, ASYNC_TIMEOUT_MILLIS) ?: error("No media session manager")
            }
        }

    private fun <T> withSession(action: (ComObject) -> T): T? {
        val manager = manager ?: return null
        val session = manager.getObject(MANAGER_GET_CURRENT_SESSION) ?: return null
        return session.use(action)
    }

    override fun poll(): RawMedia? = withSession { session ->
        val appId = session.getString(SESSION_SOURCE_APP_ID)

        val (title, artist, album) = session.getObject(SESSION_TRY_GET_MEDIA_PROPERTIES)!!.use { operation ->
            WinRt.awaitObject(operation, ASYNC_TIMEOUT_MILLIS)?.use { properties ->
                Triple(
                    properties.getString(PROPERTIES_TITLE),
                    properties.getString(PROPERTIES_ARTIST).ifEmpty { properties.getString(PROPERTIES_ALBUM_ARTIST) },
                    properties.getString(PROPERTIES_ALBUM_TITLE),
                )
            } ?: Triple("", "", "")
        }

        val status = session.getObject(SESSION_GET_PLAYBACK_INFO)?.use {
            PlaybackStatus.fromWinRt(it.getInt(PLAYBACK_INFO_STATUS))
        } ?: PlaybackStatus.STOPPED

        val now = System.currentTimeMillis()
        val timeline = session.getObject(SESSION_GET_TIMELINE_PROPERTIES)?.use {
            Timeline(
                start = it.getLong(TIMELINE_START_TIME),
                end = it.getLong(TIMELINE_END_TIME),
                position = it.getLong(TIMELINE_POSITION),
                lastUpdated = it.getLong(TIMELINE_LAST_UPDATED_TIME),
            )
        }

        RawMedia(
            appId = appId,
            title = title,
            artist = artist,
            album = album,
            status = status,
            positionMillis = timeline?.positionMillis ?: 0,
            durationMillis = timeline?.durationMillis ?: 0,
            positionSampledAt = timeline?.sampledAt(now) ?: now,
        )
    }

    override fun fetchArtwork(): ByteArray? = withSession { session ->
        session.getObject(SESSION_TRY_GET_MEDIA_PROPERTIES)!!.use { operation ->
            WinRt.awaitObject(operation, ASYNC_TIMEOUT_MILLIS)?.use { properties ->
                properties.getObject(PROPERTIES_THUMBNAIL)?.use(::readThumbnail)
            }
        }
    }

    private fun readThumbnail(reference: ComObject): ByteArray? {
        val stream = reference.getObject(STREAM_REFERENCE_OPEN_READ_ASYNC)!!.use { operation ->
            WinRt.awaitObject(operation, ASYNC_TIMEOUT_MILLIS)
        } ?: return null
        return stream.use {
            val classicStream = PointerByReference()
            checkHr(
                Shcore.INSTANCE.CreateStreamOverRandomAccessStream(it.pointer, IID_ISTREAM, classicStream),
                "CreateStreamOverRandomAccessStream",
            )
            ComObject(classicStream.value).use(::readStream)
        }
    }

    private fun readStream(stream: ComObject): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = Memory(READ_CHUNK_SIZE.toLong())
        val read = IntByReference()
        while (output.size() < MAX_ARTWORK_BYTES) {
            val hr = stream.invoke(ISTREAM_READ, buffer, READ_CHUNK_SIZE, read)
            checkHr(hr, "IStream.Read")
            if (read.value <= 0) break
            output.write(buffer.getByteArray(0, read.value))
            if (hr == S_FALSE) break
        }
        return output.toByteArray()
    }

    override fun execute(command: MediaCommand) {
        val index = when (command) {
            MediaCommand.TOGGLE_PLAY_PAUSE -> SESSION_TRY_TOGGLE_PLAY_PAUSE
            MediaCommand.NEXT -> SESSION_TRY_SKIP_NEXT
            MediaCommand.PREVIOUS -> SESSION_TRY_SKIP_PREVIOUS
        }
        withSession { it.getObject(index)?.use { operation -> WinRt.await(operation, ASYNC_TIMEOUT_MILLIS) } }
    }

    override fun seek(positionMillis: Long) {
        withSession { session ->
            val ticks = positionMillis * TICKS_PER_MILLI
            session.getObject(SESSION_TRY_CHANGE_PLAYBACK_POSITION, ticks)?.use { operation ->
                WinRt.await(operation, ASYNC_TIMEOUT_MILLIS)
            }
        }
    }

    override fun volume(appId: String): AppVolume? = audioSessions.volume(appId)

    override fun setVolume(appId: String, level: Float) = audioSessions.setVolume(appId, level)

    override fun setMuted(appId: String, muted: Boolean) = audioSessions.setMuted(appId, muted)

    override fun close() {
        audioSessions.close()
        manager?.close()
        manager = null
    }

    private data class Timeline(val start: Long, val end: Long, val position: Long, val lastUpdated: Long) {
        val durationMillis: Long get() = ((end - start) / TICKS_PER_MILLI).coerceAtLeast(0)
        val positionMillis: Long get() = ((position - start) / TICKS_PER_MILLI).coerceAtLeast(0)

        fun sampledAt(nowMillis: Long): Long {
            if (lastUpdated <= 0) return nowMillis
            val epochMillis = lastUpdated / TICKS_PER_MILLI - FILETIME_EPOCH_OFFSET_MILLIS
            return if (epochMillis > nowMillis || epochMillis < nowMillis - MAX_SAMPLE_AGE_MILLIS) nowMillis else epochMillis
        }
    }

    private companion object {
        const val MANAGER_CLASS = "Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager"
        val IID_MANAGER_STATICS = GUID("{2050C4EE-11A0-57DE-AED7-C97C70338245}")
        val IID_ISTREAM = GUID("{0000000C-0000-0000-C000-000000000046}")

        const val ASYNC_TIMEOUT_MILLIS = 3000L
        const val TICKS_PER_MILLI = 10_000L
        const val FILETIME_EPOCH_OFFSET_MILLIS = 11_644_473_600_000L
        const val MAX_SAMPLE_AGE_MILLIS = 6L * 60 * 60 * 1000
        const val READ_CHUNK_SIZE = 64 * 1024
        const val MAX_ARTWORK_BYTES = 16 * 1024 * 1024
        const val S_FALSE = 1

        const val STATICS_REQUEST_ASYNC = 6
        const val MANAGER_GET_CURRENT_SESSION = 6

        const val SESSION_SOURCE_APP_ID = 6
        const val SESSION_TRY_GET_MEDIA_PROPERTIES = 7
        const val SESSION_GET_TIMELINE_PROPERTIES = 8
        const val SESSION_GET_PLAYBACK_INFO = 9
        const val SESSION_TRY_SKIP_NEXT = 16
        const val SESSION_TRY_SKIP_PREVIOUS = 17
        const val SESSION_TRY_TOGGLE_PLAY_PAUSE = 20
        const val SESSION_TRY_CHANGE_PLAYBACK_POSITION = 24

        const val PROPERTIES_TITLE = 6
        const val PROPERTIES_ALBUM_ARTIST = 8
        const val PROPERTIES_ARTIST = 9
        const val PROPERTIES_ALBUM_TITLE = 10
        const val PROPERTIES_THUMBNAIL = 15

        const val TIMELINE_START_TIME = 6
        const val TIMELINE_END_TIME = 7
        const val TIMELINE_POSITION = 10
        const val TIMELINE_LAST_UPDATED_TIME = 11

        const val PLAYBACK_INFO_STATUS = 7

        const val STREAM_REFERENCE_OPEN_READ_ASYNC = 6
        const val ISTREAM_READ = 3
    }
}
