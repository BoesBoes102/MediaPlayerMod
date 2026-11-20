package dev.boes.mediaplayer.keybind

import com.mojang.blaze3d.platform.InputConstants
import dev.boes.mediaplayer.MediaPlayer
import dev.boes.mediaplayer.config.ConfigManager
import dev.boes.mediaplayer.config.ConfigScreens
import dev.boes.mediaplayer.media.MediaService
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object MediaPlayerKeys {
    private lateinit var openConfig: KeyMapping
    private lateinit var toggleHud: KeyMapping
    private lateinit var playPause: KeyMapping
    private lateinit var next: KeyMapping
    private lateinit var previous: KeyMapping

    fun register() {
        val category = KeyMapping.Category.register(MediaPlayer.id("main"))
        openConfig = unbound("open_config", category)
        toggleHud = unbound("toggle_hud", category)
        playPause = unbound("play_pause", category)
        next = unbound("next", category)
        previous = unbound("previous", category)
    }

    private fun unbound(name: String, category: KeyMapping.Category): KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping("key.${MediaPlayer.MOD_ID}.$name", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.value, category),
    )

    fun tick(client: Minecraft) {
        while (openConfig.consumeClick()) {
            client.gui.setScreen(ConfigScreens.create(client.gui.screen()))
        }
        while (toggleHud.consumeClick()) {
            val config = ConfigManager.config
            config.enabled = !config.enabled
            ConfigManager.save()
            client.player?.sendOverlayMessage(
                Component.translatable(if (config.enabled) "mediaplayer.message.hud_enabled" else "mediaplayer.message.hud_disabled"),
            )
        }
        while (playPause.consumeClick()) MediaService.togglePlayPause()
        while (next.consumeClick()) MediaService.next()
        while (previous.consumeClick()) MediaService.previous()
    }
}
