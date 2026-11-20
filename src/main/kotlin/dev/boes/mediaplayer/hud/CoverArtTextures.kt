package dev.boes.mediaplayer.hud

import com.mojang.blaze3d.platform.NativeImage
import dev.boes.mediaplayer.MediaPlayer
import dev.boes.mediaplayer.media.Artwork
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier

object CoverArtTextures {
    private var currentKey: String? = null
    private var currentId: Identifier? = null
    private var counter = 0

    fun textureFor(artwork: Artwork?): Identifier? {
        if (artwork == null) return null
        if (artwork.key == currentKey) return currentId
        release()
        val image = NativeImage(artwork.size, artwork.size, false)
        for (y in 0 until artwork.size) {
            for (x in 0 until artwork.size) {
                image.setPixel(x, y, artwork.pixels[y * artwork.size + x])
            }
        }
        val id = MediaPlayer.id("cover/${counter++}")
        Minecraft.getInstance().textureManager.register(id, DynamicTexture({ "Media Player cover art" }, image))
        currentKey = artwork.key
        currentId = id
        return id
    }

    fun release() {
        currentId?.let { Minecraft.getInstance().textureManager.release(it) }
        currentId = null
        currentKey = null
    }
}
