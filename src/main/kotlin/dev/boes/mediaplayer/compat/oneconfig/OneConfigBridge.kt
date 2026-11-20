package dev.boes.mediaplayer.compat.oneconfig

import dev.boes.mediaplayer.MediaPlayer
import dev.boes.mediaplayer.config.ConfigManager
import net.minecraft.client.gui.screens.Screen
import org.polyfrost.oneconfig.utils.v1.dsl.createScreen as createOneConfigScreen

internal object OneConfigBridge {
    private var pushedInitialState = false

    fun register() {
        ConfigManager.onSave { push() }
    }

    fun createScreen(): Screen = MediaPlayerOneConfig.createOneConfigScreen()

    fun sync() {
        if (!pushedInitialState) {
            pushedInitialState = true
            push()
            return
        }
        val changed = runCatching { MediaPlayerOneConfig.pullInto(ConfigManager.config) }
            .onFailure { MediaPlayer.LOGGER.debug("Could not read OneConfig values", it) }
            .getOrDefault(false)
        if (changed) ConfigManager.save()
    }

    private fun push() {
        runCatching {
            if (MediaPlayerOneConfig.pushFrom(ConfigManager.config)) MediaPlayerOneConfig.save()
        }.onFailure { MediaPlayer.LOGGER.debug("Could not update OneConfig values", it) }
    }
}
