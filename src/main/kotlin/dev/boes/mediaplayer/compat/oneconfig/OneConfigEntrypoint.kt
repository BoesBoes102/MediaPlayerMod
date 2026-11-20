package dev.boes.mediaplayer.compat.oneconfig

import dev.boes.mediaplayer.config.YaclConfigScreen
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint

class OneConfigEntrypoint : ConfigEntrypoint {
    override fun registerConfigs(context: ConfigEntrypoint.Context) {
        context.register(MediaPlayerOneConfig)
        MediaPlayerOneConfig.registerDependencies()
        context.originalScreen {
            val gui = Minecraft.getInstance().gui
            gui.setScreen(YaclConfigScreen.create(gui.screen()))
        }
    }
}
