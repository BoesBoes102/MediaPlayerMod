package dev.boes.mediaplayer

import dev.boes.mediaplayer.compat.oneconfig.OneConfigCompat
import dev.boes.mediaplayer.config.ConfigManager
import dev.boes.mediaplayer.hud.CoverArtTextures
import dev.boes.mediaplayer.hud.MediaHud
import dev.boes.mediaplayer.keybind.MediaPlayerKeys
import dev.boes.mediaplayer.media.MediaService
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object MediaPlayerClient : ClientModInitializer {
    override fun onInitializeClient() {
        ConfigManager.load()
        MediaPlayerKeys.register()
        MediaHud.register()
        OneConfigCompat.register()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            MediaPlayerKeys.tick(client)
            OneConfigCompat.tick()
        }
        ClientLifecycleEvents.CLIENT_STARTED.register { MediaService.start() }
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            MediaService.stop()
            CoverArtTextures.release()
        }
    }
}
