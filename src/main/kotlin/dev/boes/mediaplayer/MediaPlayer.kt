package dev.boes.mediaplayer

import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object MediaPlayer {
    const val MOD_ID = "mediaplayer"
    val LOGGER: Logger = LoggerFactory.getLogger("Media Player")

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
