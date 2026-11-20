package dev.boes.mediaplayer.compat.oneconfig

import dev.boes.mediaplayer.MediaPlayer
import dev.boes.mediaplayer.config.ConfigManager
import dev.boes.mediaplayer.config.ConfigSection
import dev.boes.mediaplayer.config.HudColor
import dev.boes.mediaplayer.config.HudLayoutPreset
import dev.boes.mediaplayer.config.HudTheme
import dev.boes.mediaplayer.config.MediaPlayerConfig
import dev.boes.mediaplayer.hud.HudEditorScreen
import net.minecraft.client.Minecraft
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.KtConfig
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

@Suppress("unused")
internal object MediaPlayerOneConfig : KtConfig(MediaPlayer.MOD_ID, "Media Player", Category.HUD) {
    private const val GENERAL = "General"
    private const val ELEMENTS = "Elements"
    private const val APPEARANCE = "Appearance"
    private const val RESET = "Reset"
    private val defaults = MediaPlayerConfig()

    var enabled by switch(name = "Enabled", def = defaults.enabled, description = "Show the media HUD", category = GENERAL, subcategory = "HUD")
    var layout by dropdown(
        name = "Layout",
        defaultOption = defaults.layout,
        options = HudLayoutPreset.entries.toTypedArray(),
        description = "Full shows everything, compact uses a single row",
        category = GENERAL,
        subcategory = "HUD",
        stringTransformer = { preset -> preset.name.lowercase().replaceFirstChar { it.uppercaseChar() } },
    )

    val openEditor by button(
        name = "HUD Editor",
        description = "Drag the HUD around, scroll to scale it and change the rounding and theme",
        category = GENERAL,
        subcategory = "HUD",
        text = "Open",
    ) {
        Minecraft.getInstance().execute {
            val gui = Minecraft.getInstance().gui
            gui.setScreen(HudEditorScreen(gui.screen()))
        }
    }

    var width by slider(
        name = "Width",
        def = defaults.width.toFloat(),
        category = GENERAL,
        subcategory = "HUD",
        min = MediaPlayerConfig.MIN_WIDTH.toFloat(),
        max = MediaPlayerConfig.MAX_WIDTH.toFloat(),
        step = 2f,
    )
    var scale by slider(
        name = "Scale",
        def = defaults.scale,
        category = GENERAL,
        subcategory = "HUD",
        min = MediaPlayerConfig.MIN_SCALE,
        max = MediaPlayerConfig.MAX_SCALE,
        step = 0.05f,
    )
    val resetHud by resetButton("Reset HUD", "Put the layout, size and position back to default", GENERAL, "HUD", ConfigSection.HUD, ConfigSection.PLACEMENT)

    var hideWhenNothingPlaying by switch(name = "Hide when nothing is playing", def = defaults.hideWhenNothingPlaying, category = GENERAL, subcategory = "Visibility")
    var hideWhenPaused by switch(name = "Hide when paused", def = defaults.hideWhenPaused, category = GENERAL, subcategory = "Visibility")
    var hideWhenPausedDelay by slider(
        name = "Hide after pausing (seconds)",
        def = defaults.hideWhenPausedDelaySeconds.toFloat(),
        category = GENERAL,
        subcategory = "Visibility",
        min = 0f,
        max = MediaPlayerConfig.MAX_HIDE_DELAY_SECONDS.toFloat(),
        step = 1f,
    )
    val resetVisibility by resetButton("Reset visibility", "Put the visibility settings back to default", GENERAL, "Visibility", ConfigSection.VISIBILITY)

    val resetEverything by resetButton("Reset everything", "Put every Media Player setting back to default", GENERAL, RESET, *ConfigSection.entries.toTypedArray())

    var showCover by switch(name = "Album cover", def = defaults.showCover, category = ELEMENTS, subcategory = "Elements")
    var showTitle by switch(name = "Title", def = defaults.showTitle, category = ELEMENTS, subcategory = "Elements")
    var showArtist by switch(name = "Artist", def = defaults.showArtist, category = ELEMENTS, subcategory = "Elements")
    var showAlbum by switch(name = "Album", def = defaults.showAlbum, category = ELEMENTS, subcategory = "Elements")
    var showProgressBar by switch(name = "Progress bar", def = defaults.showProgressBar, category = ELEMENTS, subcategory = "Elements")
    var showTimestamps by switch(name = "Timestamps", def = defaults.showTimestamps, category = ELEMENTS, subcategory = "Elements")
    var showControls by switch(name = "Playback buttons", def = defaults.showControls, category = ELEMENTS, subcategory = "Elements")
    var showVolume by switch(name = "Volume", def = defaults.showVolume, category = ELEMENTS, subcategory = "Elements")
    val resetElements by resetButton("Reset elements", "Show the default elements again", ELEMENTS, "Elements", ConfigSection.ELEMENTS)

    var theme by dropdown(
        name = "Theme",
        defaultOption = defaults.theme,
        options = HudTheme.entries.toTypedArray(),
        description = "Pick a color preset. Changing any color afterwards switches this to Custom",
        category = APPEARANCE,
        subcategory = "Theme",
        stringTransformer = { theme -> theme.name.lowercase().replaceFirstChar { it.uppercaseChar() } },
    )

    var showBackground by switch(name = "Background", def = defaults.showBackground, category = APPEARANCE, subcategory = "Background")
    var backgroundColor by color(name = "Background color", def = baseColor(defaults.backgroundColor), category = APPEARANCE, subcategory = "Background")
    var backgroundGradient by switch(name = "Background gradient", def = defaults.backgroundColor.gradient, description = "Fade the background into a second color", category = APPEARANCE, subcategory = "Background")
    var backgroundGradientEnd by color(name = "Background gradient end", def = PolyColor(defaults.backgroundColor.gradientArgb), category = APPEARANCE, subcategory = "Background")
    var roundedCorners by switch(name = "Rounded corners", def = defaults.roundedCorners, category = APPEARANCE, subcategory = "Background")
    var cornerRadius by slider(
        name = "Corner radius",
        def = defaults.cornerRadius.toFloat(),
        category = APPEARANCE,
        subcategory = "Background",
        min = 1f,
        max = MediaPlayerConfig.MAX_CORNER_RADIUS.toFloat(),
        step = 1f,
    )
    val resetBackground by resetButton("Reset background", "Put the background and rounding back to default", APPEARANCE, "Background", ConfigSection.SHAPE)

    var accentColor by color(name = "Accent color", def = baseColor(defaults.accentColor), category = APPEARANCE, subcategory = "Colors")
    var accentGradient by switch(name = "Accent gradient", def = defaults.accentColor.gradient, description = "Fade the progress and volume bars into a second color", category = APPEARANCE, subcategory = "Colors")
    var accentGradientEnd by color(name = "Accent gradient end", def = PolyColor(defaults.accentColor.gradientArgb), category = APPEARANCE, subcategory = "Colors")
    var textColor by color(name = "Text color", def = baseColor(defaults.textColor), category = APPEARANCE, subcategory = "Colors")
    var secondaryTextColor by color(name = "Secondary text color", def = baseColor(defaults.secondaryTextColor), category = APPEARANCE, subcategory = "Colors")
    var trackColor by color(name = "Bar background color", def = baseColor(defaults.trackColor), category = APPEARANCE, subcategory = "Colors")
    var iconColor by color(name = "Button color", def = baseColor(defaults.iconColor), category = APPEARANCE, subcategory = "Colors")
    val resetColors by resetButton("Reset colors", "Put the theme and every color back to default", APPEARANCE, "Colors", ConfigSection.COLORS)

    var textShadow by switch(name = "Text shadow", def = defaults.textShadow, category = APPEARANCE, subcategory = "Text")
    var scrollLongText by switch(name = "Scroll long text", def = defaults.scrollLongText, category = APPEARANCE, subcategory = "Text")
    val resetText by resetButton("Reset text", "Put the text settings back to default", APPEARANCE, "Text", ConfigSection.TEXT)

    private val themeBinding = same(::theme, MediaPlayerConfig::theme)

    private val bindings: List<Binding<*, *>> = listOf(
        same(::enabled, MediaPlayerConfig::enabled),
        same(::layout, MediaPlayerConfig::layout),
        intSlider(::width, MediaPlayerConfig::width),
        same(::scale, MediaPlayerConfig::scale),
        same(::hideWhenNothingPlaying, MediaPlayerConfig::hideWhenNothingPlaying),
        same(::hideWhenPaused, MediaPlayerConfig::hideWhenPaused),
        intSlider(::hideWhenPausedDelay, MediaPlayerConfig::hideWhenPausedDelaySeconds),
        same(::showCover, MediaPlayerConfig::showCover),
        same(::showTitle, MediaPlayerConfig::showTitle),
        same(::showArtist, MediaPlayerConfig::showArtist),
        same(::showAlbum, MediaPlayerConfig::showAlbum),
        same(::showProgressBar, MediaPlayerConfig::showProgressBar),
        same(::showTimestamps, MediaPlayerConfig::showTimestamps),
        same(::showControls, MediaPlayerConfig::showControls),
        same(::showVolume, MediaPlayerConfig::showVolume),
        same(::showBackground, MediaPlayerConfig::showBackground),
        color(::backgroundColor, MediaPlayerConfig::backgroundColor),
        gradientSwitch(::backgroundGradient, MediaPlayerConfig::backgroundColor),
        gradientEnd(::backgroundGradientEnd, MediaPlayerConfig::backgroundColor),
        same(::roundedCorners, MediaPlayerConfig::roundedCorners),
        intSlider(::cornerRadius, MediaPlayerConfig::cornerRadius),
        color(::accentColor, MediaPlayerConfig::accentColor),
        gradientSwitch(::accentGradient, MediaPlayerConfig::accentColor),
        gradientEnd(::accentGradientEnd, MediaPlayerConfig::accentColor),
        color(::textColor, MediaPlayerConfig::textColor),
        color(::secondaryTextColor, MediaPlayerConfig::secondaryTextColor),
        color(::trackColor, MediaPlayerConfig::trackColor),
        color(::iconColor, MediaPlayerConfig::iconColor),
        same(::textShadow, MediaPlayerConfig::textShadow),
        same(::scrollLongText, MediaPlayerConfig::scrollLongText),
        themeBinding,
    )

    fun registerDependencies() {
        hideIf(::backgroundGradientEnd.name, ::backgroundGradient.name)
        hideIf(::accentGradientEnd.name, ::accentGradient.name)
        hideIf(::cornerRadius.name, ::roundedCorners.name)
    }

    fun pushFrom(config: MediaPlayerConfig): Boolean = bindings.map { it.push(config) }.any { it }

    fun pullInto(config: MediaPlayerConfig): Boolean {
        val previousTheme = config.theme
        val changed = bindings.map { it.pull(config) }.any { it }
        if (config.theme != previousTheme) config.applyTheme(config.theme)
        return changed
    }

    private fun resetButton(name: String, description: String, category: String, subcategory: String, vararg sections: ConfigSection) =
        button(name = name, description = description, category = category, subcategory = subcategory, text = "Reset") {
            Minecraft.getInstance().execute {
                ConfigManager.config.reset(*sections)
                ConfigManager.save()
            }
        }

    private fun baseColor(color: HudColor) = PolyColor(color.argb, color.chroma, color.chromaSpeed)

    private fun <T> same(oneConfig: KMutableProperty0<T>, model: KMutableProperty1<MediaPlayerConfig, T>) =
        Binding(oneConfig, model::get, model::set, { it }, { it })

    private fun intSlider(oneConfig: KMutableProperty0<Float>, model: KMutableProperty1<MediaPlayerConfig, Int>) =
        Binding(oneConfig, model::get, model::set, Int::toFloat, Float::roundToInt)

    private fun color(oneConfig: KMutableProperty0<PolyColor>, model: KMutableProperty1<MediaPlayerConfig, HudColor>) =
        Binding(
            oneConfig,
            { config -> model.get(config).let { BaseColor(it.argb, it.chroma, it.chromaSpeed) } },
            { config, base -> model.set(config, model.get(config).copy(argb = base.argb, chroma = base.chroma, chromaSpeed = base.chromaSpeed)) },
            { base -> PolyColor(base.argb, base.chroma, base.chromaSpeed) },
            { color -> BaseColor(color.rawArgb, color.chroma, color.chromaSpeed) },
        )

    private fun gradientSwitch(oneConfig: KMutableProperty0<Boolean>, model: KMutableProperty1<MediaPlayerConfig, HudColor>) =
        Binding(
            oneConfig,
            { config -> model.get(config).gradient },
            { config, enabled -> model.set(config, model.get(config).copy(gradient = enabled)) },
            { it },
            { it },
        )

    private fun gradientEnd(oneConfig: KMutableProperty0<PolyColor>, model: KMutableProperty1<MediaPlayerConfig, HudColor>) =
        Binding(
            oneConfig,
            { config -> model.get(config).gradientArgb },
            { config, argb -> model.set(config, model.get(config).copy(gradientArgb = argb)) },
            ::PolyColor,
            PolyColor::rawArgb,
        )

    private data class BaseColor(val argb: Int, val chroma: Boolean, val chromaSpeed: Float)

    private class Binding<O, M>(
        private val oneConfig: KMutableProperty0<O>,
        private val read: (MediaPlayerConfig) -> M,
        private val write: (MediaPlayerConfig, M) -> Unit,
        private val toOneConfig: (M) -> O,
        private val toModel: (O) -> M,
    ) {
        fun push(config: MediaPlayerConfig): Boolean {
            val value = read(config)
            if (toModel(oneConfig.get()) == value) return false
            oneConfig.set(toOneConfig(value))
            return true
        }

        fun pull(config: MediaPlayerConfig): Boolean {
            val value = toModel(oneConfig.get())
            if (read(config) == value) return false
            write(config, value)
            return true
        }
    }
}
