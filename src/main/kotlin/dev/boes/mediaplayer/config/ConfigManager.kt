package dev.boes.mediaplayer.config

import com.google.gson.GsonBuilder
import dev.boes.mediaplayer.MediaPlayer
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object ConfigManager {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(HudColor::class.java, HudColor.Deserializer)
        .create()
    private val path: Path = FabricLoader.getInstance().configDir.resolve("${MediaPlayer.MOD_ID}.json")
    private val saveListeners = mutableListOf<() -> Unit>()

    var config = MediaPlayerConfig()
        private set

    fun load() {
        config = try {
            if (Files.exists(path)) {
                Files.newBufferedReader(path).use { gson.fromJson(it, MediaPlayerConfig::class.java) }?.sanitize() ?: MediaPlayerConfig()
            } else {
                MediaPlayerConfig()
            }
        } catch (exception: Exception) {
            MediaPlayer.LOGGER.error("Could not read the Media Player config, using defaults", exception)
            MediaPlayerConfig()
        }
        save()
    }

    fun save() {
        config.sanitize()
        try {
            Files.createDirectories(path.parent)
            Files.newBufferedWriter(path).use { gson.toJson(config, it) }
        } catch (exception: Exception) {
            MediaPlayer.LOGGER.error("Could not save the Media Player config", exception)
        }
        saveListeners.forEach { it() }
    }

    fun onSave(listener: () -> Unit) {
        saveListeners += listener
    }
}
