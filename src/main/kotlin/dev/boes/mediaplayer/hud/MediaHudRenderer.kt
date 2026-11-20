package dev.boes.mediaplayer.hud

import dev.boes.mediaplayer.MediaPlayer
import dev.boes.mediaplayer.config.HudColor
import dev.boes.mediaplayer.config.MediaPlayerConfig
import dev.boes.mediaplayer.media.MediaService
import dev.boes.mediaplayer.media.MediaSnapshot
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import kotlin.math.sqrt

data class HudFrame(val x: Float, val y: Float, val scale: Float, val layout: HudLayout) {
    val screenWidth: Float get() = layout.width * scale
    val screenHeight: Float get() = layout.height * scale

    fun toLocalX(mouseX: Double): Double = (mouseX - x) / scale
    fun toLocalY(mouseY: Double): Double = (mouseY - y) / scale

    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX < x + screenWidth && mouseY >= y && mouseY < y + screenHeight

    companion object {
        fun create(config: MediaPlayerConfig, snapshot: MediaSnapshot?, screenWidth: Int, screenHeight: Int): HudFrame {
            val font = Minecraft.getInstance().font
            val layout = HudLayout.compute(config, timestampWidth(font, snapshot), compactTimestampWidth(font, snapshot))
            val width = layout.width * config.scale
            val height = layout.height * config.scale
            val x = config.positionX * (screenWidth - width).coerceAtLeast(0f)
            val y = config.positionY * (screenHeight - height).coerceAtLeast(0f)
            return HudFrame(x, y, config.scale, layout)
        }

        private fun timestampWidth(font: Font, snapshot: MediaSnapshot?): Int {
            val duration = snapshot?.durationMillis ?: 0
            return font.width(TimeFormat.format(duration).replace(Regex("\\d"), "0")).coerceAtLeast(font.width("0:00"))
        }

        private fun compactTimestampWidth(font: Font, snapshot: MediaSnapshot?): Int {
            val duration = TimeFormat.format(snapshot?.durationMillis ?: 0).replace(Regex("\\d"), "0")
            return font.width("$duration/$duration")
        }
    }
}

data class HudInteractionState(
    val mouseX: Double,
    val mouseY: Double,
    val seekPreview: Float? = null,
)

object MediaHudRenderer {
    private const val SCROLL_SPEED = 22.0
    private const val SCROLL_PAUSE = 1.6
    private const val COVER_PLACEHOLDER_COLOR = 0x40FFFFFF
    private const val HOVER_BACKGROUND = 0x30FFFFFF
    private const val SEPARATOR = " • "

    private val PLAY = MediaPlayer.id("hud/play")
    private val PAUSE = MediaPlayer.id("hud/pause")
    private val NEXT = MediaPlayer.id("hud/next")
    private val PREVIOUS = MediaPlayer.id("hud/previous")
    private val VOLUME_HIGH = MediaPlayer.id("hud/volume_high")
    private val VOLUME_LOW = MediaPlayer.id("hud/volume_low")
    private val VOLUME_MUTE = MediaPlayer.id("hud/volume_mute")
    private val NOTE = MediaPlayer.id("hud/note")

    fun render(
        graphics: GuiGraphicsExtractor,
        config: MediaPlayerConfig,
        frame: HudFrame,
        snapshot: MediaSnapshot?,
        interaction: HudInteractionState?,
    ) {
        val font = Minecraft.getInstance().font
        val layout = frame.layout
        val now = System.currentTimeMillis()
        val localMouseX = interaction?.let { frame.toLocalX(it.mouseX) } ?: Double.NaN
        val localMouseY = interaction?.let { frame.toLocalY(it.mouseY) } ?: Double.NaN

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(frame.x, frame.y)
        pose.scale(frame.scale, frame.scale)

        if (config.showBackground) {
            fillRounded(graphics, 0, 0, layout.width, layout.height, config.effectiveCornerRadius, config.backgroundColor)
        }

        layout.cover?.let { drawCover(graphics, it, snapshot) }

        val title = snapshot?.title?.ifEmpty { snapshot.appName } ?: Component.translatable("mediaplayer.hud.nothing_playing").string
        val artist = snapshot?.artist ?: placeholderSubtitle()
        val album = snapshot?.album.orEmpty()

        if (layout.compact) {
            layout.title?.let { rect ->
                val text = listOf(
                    title.takeIf { config.showTitle },
                    artist.takeIf { config.showArtist && it.isNotEmpty() },
                ).filterNotNull().joinToString(SEPARATOR)
                drawText(graphics, font, config, rect, text, config.textColor.start, now)
            }
            layout.elapsed?.let { rect ->
                val text = snapshot?.takeIf { it.hasTimeline }?.let {
                    "${TimeFormat.format(it.positionAt(now))}/${TimeFormat.format(it.durationMillis)}"
                } ?: "--:--"
                drawTextRight(graphics, font, config, rect, text, config.secondaryTextColor.start)
            }
        } else {
            layout.title?.let { drawText(graphics, font, config, it, title, config.textColor.start, now) }
            layout.artist?.let { drawText(graphics, font, config, it, artist, config.secondaryTextColor.start, now) }
            layout.album?.let { drawText(graphics, font, config, it, album, config.secondaryTextColor.start, now) }
        }

        val progress = interaction?.seekPreview ?: snapshot?.progressAt(now) ?: 0f
        layout.progressBar?.let { bar ->
            val hovered = bar.expand(3).contains(localMouseX, localMouseY) || interaction?.seekPreview != null
            drawProgress(graphics, config, bar, progress, hovered && snapshot?.hasTimeline == true)
        }

        if (!layout.compact) {
            val position = snapshot?.let {
                if (interaction?.seekPreview != null) (it.durationMillis * interaction.seekPreview).toLong() else it.positionAt(now)
            } ?: 0
            layout.elapsed?.let {
                drawTextLeft(graphics, font, config, it, if (snapshot?.hasTimeline == true) TimeFormat.format(position) else "--:--")
            }
            layout.duration?.let {
                drawTextRight(graphics, font, config, it, if (snapshot?.hasTimeline == true) TimeFormat.format(snapshot.durationMillis) else "--:--", config.secondaryTextColor.start)
            }
        }

        layout.controls.forEach { (control, rect) ->
            val hovered = rect.expand(2).contains(localMouseX, localMouseY)
            drawControl(graphics, config, control, rect, snapshot, hovered)
        }

        layout.volumeSlider?.let { slider ->
            val volume = snapshot?.volume
            val level = if (volume == null || volume.muted) 0f else volume.level
            val hovered = slider.expand(3).contains(localMouseX, localMouseY)
            drawSlider(graphics, config, slider, level, hovered && volume != null, volume != null)
        }

        pose.popMatrix()
    }

    private fun placeholderSubtitle(): String =
        Component.translatable(
            if (MediaService.isSupported) "mediaplayer.hud.start_playing" else "mediaplayer.hud.unsupported",
        ).string

    private fun drawCover(graphics: GuiGraphicsExtractor, rect: Rect, snapshot: MediaSnapshot?) {
        val texture = CoverArtTextures.textureFor(snapshot?.artwork)
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, rect.x, rect.y, 0f, 0f, rect.width, rect.height, rect.width, rect.height)
            return
        }
        graphics.fill(rect.x, rect.y, rect.right, rect.bottom, COVER_PLACEHOLDER_COLOR)
        val icon = (rect.width * 0.6f).toInt()
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NOTE, rect.x + (rect.width - icon) / 2, rect.y + (rect.height - icon) / 2, icon, icon, 0xC0FFFFFF.toInt())
    }

    private fun drawText(graphics: GuiGraphicsExtractor, font: Font, config: MediaPlayerConfig, rect: Rect, text: String, color: Int, now: Long) {
        if (text.isEmpty() || rect.width <= 0) return
        val textWidth = font.width(text)
        if (textWidth <= rect.width) {
            graphics.text(font, text, rect.x, rect.y, color, config.textShadow)
            return
        }
        if (!config.scrollLongText) {
            val ellipsis = "..."
            val trimmed = font.plainSubstrByWidth(text, rect.width - font.width(ellipsis)) + ellipsis
            graphics.text(font, trimmed, rect.x, rect.y, color, config.textShadow)
            return
        }
        val offset = scrollOffset(textWidth - rect.width, now)
        graphics.enableScissor(rect.x, rect.y - 1, rect.right, rect.bottom + 1)
        graphics.text(font, text, rect.x - offset, rect.y, color, config.textShadow)
        graphics.disableScissor()
    }

    private fun drawTextLeft(graphics: GuiGraphicsExtractor, font: Font, config: MediaPlayerConfig, rect: Rect, text: String) {
        graphics.text(font, text, rect.x, rect.y, config.secondaryTextColor.start, config.textShadow)
    }

    private fun drawTextRight(graphics: GuiGraphicsExtractor, font: Font, config: MediaPlayerConfig, rect: Rect, text: String, color: Int) {
        graphics.text(font, text, rect.right - font.width(text), rect.y, color, config.textShadow)
    }

    private fun scrollOffset(overflow: Int, now: Long): Int {
        val travel = overflow / SCROLL_SPEED
        val period = 2 * (travel + SCROLL_PAUSE)
        val phase = (now / 1000.0) % period
        val offset = when {
            phase < SCROLL_PAUSE -> 0.0
            phase < SCROLL_PAUSE + travel -> (phase - SCROLL_PAUSE) * SCROLL_SPEED
            phase < 2 * SCROLL_PAUSE + travel -> overflow.toDouble()
            else -> overflow - (phase - 2 * SCROLL_PAUSE - travel) * SCROLL_SPEED
        }
        return offset.toInt().coerceIn(0, overflow)
    }

    private fun drawProgress(graphics: GuiGraphicsExtractor, config: MediaPlayerConfig, bar: Rect, progress: Float, hovered: Boolean) {
        val radius = config.effectiveCornerRadius
        fillRounded(graphics, bar.x, bar.y, bar.width, bar.height, radius, config.trackColor)
        val filled = (bar.width * progress.coerceIn(0f, 1f)).toInt()
        if (filled > 0) fillRounded(graphics, bar.x, bar.y, filled, bar.height, radius, config.accentColor, bar.width)
        if (hovered) {
            val knob = bar.height + 3
            val knobX = bar.x + filled - knob / 2
            val knobY = bar.y + (bar.height - knob) / 2
            fillRounded(graphics, knobX, knobY, knob, knob, radius, config.textColor.start)
        }
    }

    private fun drawSlider(graphics: GuiGraphicsExtractor, config: MediaPlayerConfig, slider: Rect, level: Float, hovered: Boolean, enabled: Boolean) {
        val radius = config.effectiveCornerRadius
        val track = config.trackColor.start.let { if (enabled) it else withAlpha(it, 0.4f) }
        fillRounded(graphics, slider.x, slider.y, slider.width, slider.height, radius, track)
        val filled = (slider.width * level.coerceIn(0f, 1f)).toInt()
        if (filled > 0) fillRounded(graphics, slider.x, slider.y, filled, slider.height, radius, config.accentColor, slider.width)
        if (hovered) {
            val knob = slider.height + 2
            fillRounded(graphics, slider.x + filled - knob / 2, slider.y - 1, knob, knob, radius, config.textColor.start)
        }
    }

    private fun drawControl(graphics: GuiGraphicsExtractor, config: MediaPlayerConfig, control: HudControl, rect: Rect, snapshot: MediaSnapshot?, hovered: Boolean) {
        val sprite = when (control) {
            HudControl.PREVIOUS -> PREVIOUS
            HudControl.NEXT -> NEXT
            HudControl.PLAY_PAUSE -> if (snapshot?.status?.isPlaying == true) PAUSE else PLAY
            HudControl.VOLUME -> {
                val volume = snapshot?.volume
                when {
                    volume == null || volume.muted || volume.level <= 0.001f -> VOLUME_MUTE
                    volume.level < 0.5f -> VOLUME_LOW
                    else -> VOLUME_HIGH
                }
            }
        }
        val enabled = snapshot != null && (control != HudControl.VOLUME || snapshot.volume != null)
        if (hovered && enabled) {
            val background = rect.expand(2)
            fillRounded(graphics, background.x, background.y, background.width, background.height, config.effectiveCornerRadius, HOVER_BACKGROUND)
        }
        val color = when {
            !enabled -> withAlpha(config.iconColor.start, 0.4f)
            hovered -> config.accentColor.start
            else -> config.iconColor.start
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, rect.x, rect.y, rect.width, rect.height, color)
    }

    fun fillRounded(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        if (width <= 0 || height <= 0) return
        val r = radius.coerceIn(0, minOf(width, height) / 2)
        if (r <= 0) {
            graphics.fill(x, y, x + width, y + height, color)
            return
        }
        graphics.fill(x, y + r, x + width, y + height - r, color)
        for (row in 0 until r) {
            val inset = cornerInset(r, row)
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color)
            graphics.fill(x + inset, y + height - row - 1, x + width - inset, y + height - row, color)
        }
    }

    fun fillRounded(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, radius: Int, paint: HudColor, gradientWidth: Int = width) {
        if (!paint.gradient) {
            fillRounded(graphics, x, y, width, height, radius, paint.start)
            return
        }
        if (width <= 0 || height <= 0) return
        val r = radius.coerceIn(0, minOf(width, height) / 2)
        val start = paint.start
        val end = paint.end
        val span = (gradientWidth - 1).coerceAtLeast(1).toFloat()
        for (column in 0 until width) {
            val inset = when {
                column < r -> cornerInset(r, column)
                column >= width - r -> cornerInset(r, width - 1 - column)
                else -> 0
            }
            graphics.fill(x + column, y + inset, x + column + 1, y + height - inset, HudColor.lerp(start, end, column / span))
        }
    }

    private fun cornerInset(radius: Int, index: Int): Int {
        val distance = radius - index - 0.5
        return (radius - sqrt((radius * radius - distance * distance).coerceAtLeast(0.0))).toInt()
    }

    private fun withAlpha(color: Int, factor: Float): Int {
        val alpha = ((color ushr 24) * factor).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (color and 0xFFFFFF)
    }
}
