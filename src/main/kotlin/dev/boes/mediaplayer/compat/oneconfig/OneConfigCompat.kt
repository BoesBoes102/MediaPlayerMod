package dev.boes.mediaplayer.compat.oneconfig

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen

object OneConfigCompat {
    private const val SYNC_INTERVAL_TICKS = 10

    val isLoaded: Boolean by lazy { FabricLoader.getInstance().isModLoaded("oneconfig") }

    private var ticks = 0

    fun register() {
        if (isLoaded) OneConfigBridge.register()
    }

    fun createScreen(): Screen? = if (isLoaded) OneConfigBridge.createScreen() else null

    fun tick() {
        if (!isLoaded || ++ticks % SYNC_INTERVAL_TICKS != 0) return
        OneConfigBridge.sync()
    }
}
