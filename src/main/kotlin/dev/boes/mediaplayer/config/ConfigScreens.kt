package dev.boes.mediaplayer.config

import dev.boes.mediaplayer.compat.oneconfig.OneConfigCompat
import net.minecraft.client.gui.screens.Screen

object ConfigScreens {
    fun create(parent: Screen?): Screen = OneConfigCompat.createScreen() ?: YaclConfigScreen.create(parent)
}
