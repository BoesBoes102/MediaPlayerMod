package dev.boes.mediaplayer.media.windows

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Guid.GUID
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import dev.boes.mediaplayer.media.AppVolume

internal class WindowsAudioSessions : AutoCloseable {
    private var enumerator: ComObject? = null
    private val processPaths = object : LinkedHashMap<Int, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, String>) = size > 128
    }
    private val ownProcessId = ProcessHandle.current().pid().toInt()

    fun start() {
        val result = PointerByReference()
        checkHr(
            Ole32.INSTANCE.CoCreateInstance(CLSID_MM_DEVICE_ENUMERATOR, null, CLSCTX_ALL, IID_MM_DEVICE_ENUMERATOR, result).toInt(),
            "CoCreateInstance(MMDeviceEnumerator)",
        )
        enumerator = ComObject(result.value)
    }

    fun volume(appId: String): AppVolume? = withVolumes(appId) { volumes ->
        val first = volumes.first()
        AppVolume(first.getFloat(VOLUME_GET_MASTER), first.getInt(VOLUME_GET_MUTE) != 0)
    }

    fun setVolume(appId: String, level: Float) {
        withVolumes(appId) { volumes ->
            volumes.forEach {
                it.call(VOLUME_SET_MASTER, level.coerceIn(0f, 1f), Pointer.NULL)
                if (level > 0f) it.call(VOLUME_SET_MUTE, 0, Pointer.NULL)
            }
        }
    }

    fun setMuted(appId: String, muted: Boolean) {
        withVolumes(appId) { volumes -> volumes.forEach { it.call(VOLUME_SET_MUTE, if (muted) 1 else 0, Pointer.NULL) } }
    }

    private fun <T> withVolumes(appId: String, action: (List<ComObject>) -> T): T? {
        if (appId.isEmpty()) return null
        val sessions = activeSessions()
        try {
            val matching = sessions.filter { matches(appId, it.path) }.ifEmpty { fallbackSessions(sessions) }
            if (matching.isEmpty()) return null
            val volumes = matching.mapNotNull { it.control.queryInterface(IID_SIMPLE_AUDIO_VOLUME) }
            return try {
                if (volumes.isEmpty()) null else action(volumes)
            } finally {
                volumes.forEach(ComObject::close)
            }
        } finally {
            sessions.forEach { it.control.close() }
        }
    }

    private fun fallbackSessions(sessions: List<AudioSession>): List<AudioSession> {
        val active = sessions.filter { it.active }
        return if (active.map { it.processId }.distinct().size == 1) active else emptyList()
    }

    private fun activeSessions(): List<AudioSession> {
        val enumerator = enumerator ?: return emptyList()
        val device = enumerator.getObject(ENUMERATOR_GET_DEFAULT_ENDPOINT, DATA_FLOW_RENDER, ROLE_MULTIMEDIA) ?: return emptyList()
        val manager = device.use { it.getObject(DEVICE_ACTIVATE, IID_AUDIO_SESSION_MANAGER2, CLSCTX_ALL, Pointer.NULL) } ?: return emptyList()
        val sessionEnumerator = manager.use { it.getObject(MANAGER_GET_SESSION_ENUMERATOR) } ?: return emptyList()
        return sessionEnumerator.use { list ->
            (0 until list.getInt(SESSION_ENUMERATOR_GET_COUNT)).mapNotNull { index ->
                val control = list.getObject(SESSION_ENUMERATOR_GET_SESSION, index) ?: return@mapNotNull null
                val control2 = control.use { it.queryInterface(IID_AUDIO_SESSION_CONTROL2) } ?: return@mapNotNull null
                val processId = control2.getInt(CONTROL_GET_PROCESS_ID)
                val isSystem = control2.invoke(CONTROL_IS_SYSTEM_SOUNDS) == S_OK
                if (isSystem || processId == 0 || processId == ownProcessId) {
                    control2.close()
                    null
                } else {
                    AudioSession(control2, processId, processPath(processId), control2.getInt(CONTROL_GET_STATE) == SESSION_STATE_ACTIVE)
                }
            }
        }
    }

    private fun processPath(processId: Int): String = processPaths.getOrPut(processId) {
        val handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, processId) ?: return@getOrPut ""
        try {
            val buffer = CharArray(1024)
            val size = IntByReference(buffer.size)
            if (Kernel32.INSTANCE.QueryFullProcessImageName(handle, 0, buffer, size)) String(buffer, 0, size.value) else ""
        } finally {
            Kernel32.INSTANCE.CloseHandle(handle)
        }
    }

    private fun matches(appId: String, path: String): Boolean {
        if (path.isEmpty()) return false
        val id = appId.lowercase()
        val lowerPath = path.lowercase()
        val exeName = lowerPath.substringAfterLast('\\')
        return when {
            id.endsWith(".exe") -> id.substringAfterLast('\\') == exeName
            '!' in id -> lowerPath.contains("\\" + id.substringBefore('!').substringBefore('_') + "_")
            else -> exeName.removeSuffix(".exe").let { it.isNotEmpty() && id.contains(it) }
        }
    }

    override fun close() {
        enumerator?.close()
        enumerator = null
    }

    private class AudioSession(val control: ComObject, val processId: Int, val path: String, val active: Boolean)

    private companion object {
        val CLSID_MM_DEVICE_ENUMERATOR = GUID("{BCDE0395-E52F-467C-8E3D-C4579291692E}")
        val IID_MM_DEVICE_ENUMERATOR = GUID("{A95664D2-9614-4F35-A746-DE8DB63617E6}")
        val IID_AUDIO_SESSION_MANAGER2 = GUID("{77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F}")
        val IID_AUDIO_SESSION_CONTROL2 = GUID("{BFB7FF88-7239-4FC9-8FA2-07C950BE9C6D}")
        val IID_SIMPLE_AUDIO_VOLUME = GUID("{87CE5498-68D6-44E5-9215-6DA47EF883D8}")

        const val CLSCTX_ALL = 0x17
        const val DATA_FLOW_RENDER = 0
        const val ROLE_MULTIMEDIA = 1
        const val S_OK = 0
        const val SESSION_STATE_ACTIVE = 1

        const val ENUMERATOR_GET_DEFAULT_ENDPOINT = 4
        const val DEVICE_ACTIVATE = 3
        const val MANAGER_GET_SESSION_ENUMERATOR = 5
        const val SESSION_ENUMERATOR_GET_COUNT = 3
        const val SESSION_ENUMERATOR_GET_SESSION = 4
        const val CONTROL_GET_STATE = 3
        const val CONTROL_GET_PROCESS_ID = 14
        const val CONTROL_IS_SYSTEM_SOUNDS = 15
        const val VOLUME_SET_MASTER = 3
        const val VOLUME_GET_MASTER = 4
        const val VOLUME_SET_MUTE = 5
        const val VOLUME_GET_MUTE = 6
    }
}
