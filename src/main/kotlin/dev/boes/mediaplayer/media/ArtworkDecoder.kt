package dev.boes.mediaplayer.media

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

object ArtworkDecoder {
    const val SIZE = 128

    fun decode(key: String, bytes: ByteArray): Artwork? {
        val source = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
        val side = minOf(source.width, source.height)
        if (side <= 0) return null
        val cropped = source.getSubimage((source.width - side) / 2, (source.height - side) / 2, side, side)
        val scaled = downscale(cropped, SIZE)
        val pixels = IntArray(SIZE * SIZE)
        scaled.getRGB(0, 0, SIZE, SIZE, pixels, 0, SIZE)
        return Artwork(key, SIZE, pixels)
    }

    private fun downscale(image: BufferedImage, target: Int): BufferedImage {
        var current = image
        var size = image.width
        do {
            size = maxOf(size / 2, target)
            current = resize(current, size)
        } while (size > target)
        return current
    }

    private fun resize(image: BufferedImage, size: Int): BufferedImage {
        val result = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.drawImage(image, 0, 0, size, size, null)
        } finally {
            graphics.dispose()
        }
        return result
    }
}
