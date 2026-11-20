package dev.boes.mediaplayer.hud

import com.mojang.blaze3d.platform.InputConstants
import dev.boes.mediaplayer.MediaPlayer
import dev.boes.mediaplayer.config.ConfigManager
import dev.boes.mediaplayer.media.MediaService
import dev.boes.mediaplayer.media.MediaSnapshot
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import kotlin.math.sign

object MediaHud {
    private enum class DragTarget { SEEK, VOLUME }

    private var pausedSince: Long? = null
    private var dragTarget: DragTarget? = null
    private var seekPreview: Float? = null

    fun register() {
        HudElementRegistry.addLast(MediaPlayer.id("media_hud")) { graphics, _ -> renderHudLayer(graphics) }
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (isInteractive(screen)) attach(screen)
        }
    }

    fun visibleSnapshot(): MediaSnapshot? = MediaService.snapshot?.takeIf { it.title.isNotBlank() || it.artist.isNotBlank() }

    private fun shouldShow(snapshot: MediaSnapshot?, now: Long): Boolean {
        val config = ConfigManager.config
        if (!config.enabled) return false
        if (snapshot == null) {
            pausedSince = null
            return !config.hideWhenNothingPlaying
        }
        if (snapshot.status.isPlaying) {
            pausedSince = null
            return true
        }
        val since = pausedSince ?: now.also { pausedSince = it }
        return !(config.hideWhenPaused && now - since >= config.hideWhenPausedDelaySeconds * 1000L)
    }

    private fun currentFrame(screenWidth: Int, screenHeight: Int): Pair<HudFrame, MediaSnapshot?>? {
        val snapshot = visibleSnapshot()
        if (!shouldShow(snapshot, System.currentTimeMillis())) return null
        return HudFrame.create(ConfigManager.config, snapshot, screenWidth, screenHeight) to snapshot
    }

    private fun renderHudLayer(graphics: GuiGraphicsExtractor) {
        val screen = Minecraft.getInstance().gui.screen()
        if (screen != null && (isInteractive(screen) || screen is HudEditorScreen)) return
        val (frame, snapshot) = currentFrame(graphics.guiWidth(), graphics.guiHeight()) ?: return
        MediaHudRenderer.render(graphics, ConfigManager.config, frame, snapshot, null)
    }

    private fun isInteractive(screen: Screen): Boolean = screen is ChatScreen || screen is AbstractContainerScreen<*>

    private fun attach(screen: Screen) {
        ScreenEvents.afterExtract(screen).register { _, graphics, mouseX, mouseY, _ ->
            val (frame, snapshot) = currentFrame(graphics.guiWidth(), graphics.guiHeight()) ?: return@register
            MediaHudRenderer.render(graphics, ConfigManager.config, frame, snapshot, HudInteractionState(mouseX.toDouble(), mouseY.toDouble(), seekPreview))
        }
        ScreenMouseEvents.allowMouseClick(screen).register { current, event -> !handleClick(current, event) }
        ScreenMouseEvents.allowMouseDrag(screen).register { current, event, _, _ -> !handleDrag(current, event) }
        ScreenMouseEvents.allowMouseRelease(screen).register { current, event -> !handleRelease(current, event) }
        ScreenMouseEvents.allowMouseScroll(screen).register { current, mouseX, mouseY, _, scrollY -> !handleScroll(current, mouseX, mouseY, scrollY) }
        ScreenEvents.remove(screen).register {
            dragTarget = null
            seekPreview = null
        }
    }

    private fun handleClick(screen: Screen, event: MouseButtonEvent): Boolean {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false
        val (frame, snapshot) = currentFrame(screen.width, screen.height) ?: return false
        snapshot ?: return false
        val x = frame.toLocalX(event.x())
        val y = frame.toLocalY(event.y())
        val layout = frame.layout

        layout.controls.entries.firstOrNull { it.value.expand(2).contains(x, y) }?.let { (control, _) ->
            when (control) {
                HudControl.PREVIOUS -> MediaService.previous()
                HudControl.PLAY_PAUSE -> MediaService.togglePlayPause()
                HudControl.NEXT -> MediaService.next()
                HudControl.VOLUME -> MediaService.toggleMute()
            }
            return true
        }

        val bar = layout.progressBar
        if (bar != null && snapshot.hasTimeline && bar.expand(3).contains(x, y)) {
            dragTarget = DragTarget.SEEK
            seekPreview = fraction(bar, x)
            return true
        }

        val slider = layout.volumeSlider
        if (slider != null && snapshot.volume != null && slider.expand(3).contains(x, y)) {
            dragTarget = DragTarget.VOLUME
            MediaService.setVolume(fraction(slider, x))
            return true
        }
        return false
    }

    private fun handleDrag(screen: Screen, event: MouseButtonEvent): Boolean {
        val target = dragTarget ?: return false
        val (frame, _) = currentFrame(screen.width, screen.height) ?: return false
        val x = frame.toLocalX(event.x())
        when (target) {
            DragTarget.SEEK -> frame.layout.progressBar?.let { seekPreview = fraction(it, x) }
            DragTarget.VOLUME -> frame.layout.volumeSlider?.let { MediaService.setVolume(fraction(it, x)) }
        }
        return true
    }

    private fun handleRelease(screen: Screen, event: MouseButtonEvent): Boolean {
        val target = dragTarget ?: return false
        dragTarget = null
        if (target == DragTarget.SEEK) {
            val preview = seekPreview
            seekPreview = null
            val snapshot = MediaService.snapshot
            if (preview != null && snapshot != null) MediaService.seek((snapshot.durationMillis * preview).toLong())
        }
        return true
    }

    private fun handleScroll(screen: Screen, mouseX: Double, mouseY: Double, scrollY: Double): Boolean {
        val (frame, snapshot) = currentFrame(screen.width, screen.height) ?: return false
        if (snapshot?.volume == null || !frame.contains(mouseX, mouseY) || scrollY == 0.0) return false
        MediaService.adjustVolume(sign(scrollY).toInt())
        return true
    }

    private fun fraction(rect: Rect, localX: Double): Float = ((localX - rect.x) / rect.width).toFloat().coerceIn(0f, 1f)
}
