package dev.boes.mediaplayer.hud

import com.mojang.blaze3d.platform.InputConstants
import dev.boes.mediaplayer.config.ConfigManager
import dev.boes.mediaplayer.config.ConfigSection
import dev.boes.mediaplayer.config.HudLayoutPreset
import dev.boes.mediaplayer.config.HudTheme
import dev.boes.mediaplayer.config.MediaPlayerConfig
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

class HudEditorScreen(private val parent: Screen?) : Screen(Component.translatable("mediaplayer.editor.title")) {
    private val config: MediaPlayerConfig get() = ConfigManager.config

    private var dragging = false
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0
    private var snapX: Float? = null
    private var snapY: Float? = null

    override fun init() {
        val totalWidth = BUTTON_WIDTH * 3 + BUTTON_GAP * 2
        val left = (width - totalWidth) / 2
        val column = { index: Int -> left + (BUTTON_WIDTH + BUTTON_GAP) * index }
        val topRow = height - BOTTOM_MARGIN - BUTTON_HEIGHT * 2 - BUTTON_GAP
        val bottomRow = height - BOTTOM_MARGIN - BUTTON_HEIGHT

        addRenderableWidget(
            Button.builder(layoutLabel()) {
                config.layout = HudLayoutPreset.entries[(config.layout.ordinal + 1) % HudLayoutPreset.entries.size]
                ConfigManager.save()
                it.message = layoutLabel()
            }.bounds(column(0), topRow, BUTTON_WIDTH, BUTTON_HEIGHT).build(),
        )
        addRenderableWidget(
            Button.builder(themeLabel()) {
                val themes = HudTheme.selectableEntries
                config.applyTheme(themes[(themes.indexOf(config.theme) + 1) % themes.size])
                ConfigManager.save()
                it.message = themeLabel()
            }.bounds(column(1), topRow, BUTTON_WIDTH, BUTTON_HEIGHT).build(),
        )
        addRenderableWidget(RoundingSlider(column(2), topRow, BUTTON_WIDTH, BUTTON_HEIGHT))
        addRenderableWidget(
            Button.builder(Component.translatable("mediaplayer.editor.reset")) { resetPlacement() }
                .bounds(column(0), bottomRow, BUTTON_WIDTH, BUTTON_HEIGHT).build(),
        )
        addRenderableWidget(
            Button.builder(Component.translatable("mediaplayer.editor.reset_defaults")) { resetToDefaults() }
                .bounds(column(1), bottomRow, BUTTON_WIDTH, BUTTON_HEIGHT).build(),
        )
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onClose() }
                .bounds(column(2), bottomRow, BUTTON_WIDTH, BUTTON_HEIGHT).build(),
        )
    }

    private fun layoutLabel(): Component =
        Component.translatable("mediaplayer.editor.layout", Component.translatable(config.layout.translationKey))

    private fun themeLabel(): Component =
        Component.translatable("mediaplayer.editor.theme", Component.translatable(config.theme.translationKey))

    private fun frame(): HudFrame = HudFrame.create(config, MediaHud.visibleSnapshot(), width, height)

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (minecraft.level == null) super.extractBackground(graphics, mouseX, mouseY, delta)
        else graphics.fill(0, 0, width, height, 0x50000000)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        if (dragging) {
            snapX?.let { graphics.fill(it.roundToInt(), 0, it.roundToInt() + 1, height, GUIDE_COLOR) }
            snapY?.let { graphics.fill(0, it.roundToInt(), width, it.roundToInt() + 1, GUIDE_COLOR) }
        }

        val frame = frame()
        MediaHudRenderer.render(graphics, config, frame, MediaHud.visibleSnapshot(), null)
        val hovered = dragging || frame.contains(mouseX.toDouble(), mouseY.toDouble())
        val x1 = frame.x.toInt() - 1
        val y1 = frame.y.toInt() - 1
        val x2 = (frame.x + frame.screenWidth).roundToInt() + 1
        val y2 = (frame.y + frame.screenHeight).roundToInt() + 1
        graphics.outline(x1, y1, x2 - x1, y2 - y1, if (hovered) OUTLINE_ACTIVE else OUTLINE_IDLE)

        val textTop = if (frame.y < HINT_AREA_HEIGHT) height - CONTROLS_HEIGHT - HINT_AREA_HEIGHT else 8
        graphics.centeredText(font, title, width / 2, textTop, 0xFFFFFFFF.toInt())
        graphics.centeredText(font, Component.translatable("mediaplayer.editor.hint"), width / 2, textTop + 12, HINT_COLOR)
        graphics.centeredText(
            font,
            Component.translatable("mediaplayer.editor.scale", (config.scale * 100).roundToInt()),
            width / 2,
            textTop + 24,
            HINT_COLOR,
        )
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, doubleClick)) return true
        val frame = frame()
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !frame.contains(event.x(), event.y())) return false
        dragging = true
        dragOffsetX = event.x() - frame.x
        dragOffsetY = event.y() - frame.y
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (!dragging) return super.mouseDragged(event, dragX, dragY)
        val frame = frame()
        val newX = snap(event.x() - dragOffsetX, frame.screenWidth, width.toFloat()) { snapX = it }
        val newY = snap(event.y() - dragOffsetY, frame.screenHeight, height.toFloat()) { snapY = it }
        applyPosition(newX, newY, frame)
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (dragging) {
            dragging = false
            snapX = null
            snapY = null
            ConfigManager.save()
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (scrollY == 0.0) return false
        val step = SCALE_STEP * sign(scrollY).toFloat()
        config.scale = ((config.scale + step) * 20).roundToInt() / 20f
        config.scale = config.scale.coerceIn(MediaPlayerConfig.MIN_SCALE, MediaPlayerConfig.MAX_SCALE)
        ConfigManager.save()
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val frame = frame()
        when {
            event.key() == InputConstants.KEY_R -> resetPlacement()
            event.isLeft -> applyPosition(frame.x - 1.0, frame.y.toDouble(), frame)
            event.isRight -> applyPosition(frame.x + 1.0, frame.y.toDouble(), frame)
            event.isUp -> applyPosition(frame.x.toDouble(), frame.y - 1.0, frame)
            event.isDown -> applyPosition(frame.x.toDouble(), frame.y + 1.0, frame)
            else -> return super.keyPressed(event)
        }
        return true
    }

    private fun resetPlacement() {
        config.reset(ConfigSection.PLACEMENT)
        ConfigManager.save()
    }

    private fun resetToDefaults() {
        config.reset(ConfigSection.PLACEMENT, ConfigSection.HUD, ConfigSection.SHAPE, ConfigSection.COLORS)
        ConfigManager.save()
        rebuildWidgets()
    }

    private fun applyPosition(x: Double, y: Double, frame: HudFrame) {
        val freeWidth = width - frame.screenWidth
        val freeHeight = height - frame.screenHeight
        config.positionX = if (freeWidth > 0) (x / freeWidth).toFloat().coerceIn(0f, 1f) else 0f
        config.positionY = if (freeHeight > 0) (y / freeHeight).toFloat().coerceIn(0f, 1f) else 0f
    }

    private fun snap(position: Double, size: Float, screenSize: Float, guide: (Float?) -> Unit): Double {
        val candidates = listOf(
            0.0 to 0f,
            (screenSize - size).toDouble() to screenSize - 1,
            ((screenSize - size) / 2).toDouble() to screenSize / 2,
        )
        val match = candidates.firstOrNull { abs(position - it.first) <= SNAP_DISTANCE }
        guide(match?.second)
        return match?.first ?: position
    }

    override fun onClose() {
        ConfigManager.save()
        minecraft.gui.setScreen(parent)
    }

    private inner class RoundingSlider(x: Int, y: Int, width: Int, height: Int) :
        AbstractSliderButton(x, y, width, height, Component.empty(), config.effectiveCornerRadius / MediaPlayerConfig.MAX_CORNER_RADIUS.toDouble()) {
        private val radius: Int get() = (value * MediaPlayerConfig.MAX_CORNER_RADIUS).roundToInt()

        init {
            updateMessage()
        }

        override fun updateMessage() {
            val amount = if (radius == 0) CommonComponents.OPTION_OFF else Component.literal(radius.toString())
            message = Component.translatable("mediaplayer.editor.rounding", amount)
        }

        override fun applyValue() {
            if (radius == config.effectiveCornerRadius) return
            config.setCornerRounding(radius)
            ConfigManager.save()
        }
    }

    private companion object {
        const val BUTTON_WIDTH = 100
        const val BUTTON_HEIGHT = 20
        const val BUTTON_GAP = 4
        const val BOTTOM_MARGIN = 8
        const val CONTROLS_HEIGHT = BOTTOM_MARGIN + BUTTON_HEIGHT * 2 + BUTTON_GAP
        const val SNAP_DISTANCE = 4.0
        const val HINT_AREA_HEIGHT = 40
        const val HINT_COLOR = 0xFFB0B0B0.toInt()
        const val SCALE_STEP = 0.05f
        const val GUIDE_COLOR = 0x80FFD24A.toInt()
        const val OUTLINE_ACTIVE = 0xFFFFFFFF.toInt()
        const val OUTLINE_IDLE = 0x80FFFFFF.toInt()
    }
}
