package dev.boes.mediaplayer.config

import dev.boes.mediaplayer.hud.HudEditorScreen
import dev.isxander.yacl3.api.ButtonOption
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.ColorControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0

object YaclConfigScreen {
    private const val PREFIX = "mediaplayer.config"

    fun create(parent: Screen?): Screen {
        val config = ConfigManager.config
        val defaults = MediaPlayerConfig()

        val general = ConfigCategory.createBuilder()
            .name(text("category.general"))
            .option(toggle("enabled", defaults.enabled, config::enabled))
            .option(
                Option.createBuilder<HudLayoutPreset>()
                    .name(text("layout"))
                    .description(describe("layout"))
                    .binding(defaults.layout, { config.layout }, { config.layout = it })
                    .controller {
                        EnumControllerBuilder.create(it)
                            .enumClass(HudLayoutPreset::class.java)
                            .formatValue { preset -> Component.translatable(preset.translationKey) }
                    }
                    .build(),
            )
            .option(
                ButtonOption.createBuilder()
                    .name(text("open_editor"))
                    .description(describe("open_editor"))
                    .text(text("open_editor.button"))
                    .action { screen, _ -> Minecraft.getInstance().gui.setScreen(HudEditorScreen(screen)) }
                    .build(),
            )
            .option(intSlider("width", defaults.width, config::width, MediaPlayerConfig.MIN_WIDTH, MediaPlayerConfig.MAX_WIDTH, 2))
            .option(
                Option.createBuilder<Float>()
                    .name(text("scale"))
                    .description(describe("scale"))
                    .binding(defaults.scale, { config.scale }, { config.scale = it })
                    .controller {
                        FloatSliderControllerBuilder.create(it)
                            .range(MediaPlayerConfig.MIN_SCALE, MediaPlayerConfig.MAX_SCALE)
                            .step(0.05f)
                            .formatValue { value -> Component.literal("${(value * 100).roundToInt()}%") }
                    }
                    .build(),
            )
            .group(
                OptionGroup.createBuilder()
                    .name(text("group.visibility"))
                    .description(describe("group.visibility"))
                    .option(toggle("hide_when_nothing_playing", defaults.hideWhenNothingPlaying, config::hideWhenNothingPlaying))
                    .option(toggle("hide_when_paused", defaults.hideWhenPaused, config::hideWhenPaused))
                    .option(
                        intSlider(
                            "hide_when_paused_delay",
                            defaults.hideWhenPausedDelaySeconds,
                            config::hideWhenPausedDelaySeconds,
                            0,
                            MediaPlayerConfig.MAX_HIDE_DELAY_SECONDS,
                            1,
                        ) { Component.translatable("$PREFIX.seconds", it) },
                    )
                    .build(),
            )
            .build()

        val elements = ConfigCategory.createBuilder()
            .name(text("category.elements"))
            .option(tickBox("show_cover", defaults.showCover, config::showCover))
            .option(tickBox("show_title", defaults.showTitle, config::showTitle))
            .option(tickBox("show_artist", defaults.showArtist, config::showArtist))
            .option(tickBox("show_album", defaults.showAlbum, config::showAlbum))
            .option(tickBox("show_progress_bar", defaults.showProgressBar, config::showProgressBar))
            .option(tickBox("show_timestamps", defaults.showTimestamps, config::showTimestamps))
            .option(tickBox("show_controls", defaults.showControls, config::showControls))
            .option(tickBox("show_volume", defaults.showVolume, config::showVolume))
            .build()

        val appearance = ConfigCategory.createBuilder()
            .name(text("category.appearance"))
            .option(
                Option.createBuilder<HudTheme>()
                    .name(text("theme"))
                    .description(describe("theme"))
                    .binding(defaults.theme, { config.theme }, { config.applyTheme(it) })
                    .controller {
                        EnumControllerBuilder.create(it)
                            .enumClass(HudTheme::class.java)
                            .formatValue { theme -> Component.translatable(theme.translationKey) }
                    }
                    .build(),
            )
            .option(toggle("show_background", defaults.showBackground, config::showBackground))
            .option(color("background_color", defaults.backgroundColor, config::backgroundColor))
            .option(gradientToggle("background_gradient", defaults.backgroundColor, config::backgroundColor))
            .option(gradientEnd("background_gradient_end", defaults.backgroundColor, config::backgroundColor))
            .option(color("accent_color", defaults.accentColor, config::accentColor))
            .option(gradientToggle("accent_gradient", defaults.accentColor, config::accentColor))
            .option(gradientEnd("accent_gradient_end", defaults.accentColor, config::accentColor))
            .option(color("text_color", defaults.textColor, config::textColor))
            .option(color("secondary_text_color", defaults.secondaryTextColor, config::secondaryTextColor))
            .option(color("track_color", defaults.trackColor, config::trackColor))
            .option(color("icon_color", defaults.iconColor, config::iconColor))
            .option(toggle("rounded_corners", defaults.roundedCorners, config::roundedCorners))
            .option(intSlider("corner_radius", defaults.cornerRadius, config::cornerRadius, 1, MediaPlayerConfig.MAX_CORNER_RADIUS, 1))
            .option(toggle("text_shadow", defaults.textShadow, config::textShadow))
            .option(toggle("scroll_long_text", defaults.scrollLongText, config::scrollLongText))
            .build()

        return YetAnotherConfigLib.createBuilder()
            .title(text("title"))
            .category(general)
            .category(elements)
            .category(appearance)
            .save(ConfigManager::save)
            .build()
            .generateScreen(parent)
    }

    private fun text(key: String): Component = Component.translatable("$PREFIX.$key")

    private fun describe(key: String): OptionDescription = OptionDescription.of(Component.translatable("$PREFIX.$key.description"))

    private fun toggle(key: String, default: Boolean, property: KMutableProperty0<Boolean>): Option<Boolean> =
        Option.createBuilder<Boolean>()
            .name(text(key))
            .description(describe(key))
            .binding(default, property::get, property::set)
            .controller { BooleanControllerBuilder.create(it).coloured(true) }
            .build()

    private fun tickBox(key: String, default: Boolean, property: KMutableProperty0<Boolean>): Option<Boolean> =
        Option.createBuilder<Boolean>()
            .name(text(key))
            .description(describe(key))
            .binding(default, property::get, property::set)
            .controller(TickBoxControllerBuilder::create)
            .build()

    private fun intSlider(
        key: String,
        default: Int,
        property: KMutableProperty0<Int>,
        min: Int,
        max: Int,
        step: Int,
        format: ((Int) -> Component)? = null,
    ): Option<Int> =
        Option.createBuilder<Int>()
            .name(text(key))
            .description(describe(key))
            .binding(default, property::get, property::set)
            .controller {
                IntegerSliderControllerBuilder.create(it).range(min, max).step(step).apply {
                    if (format != null) formatValue { value -> format(value) }
                }
            }
            .build()

    private fun color(key: String, default: HudColor, property: KMutableProperty0<HudColor>): Option<Color> =
        colorOption(key, default.argb, { property.get().argb }, { property.set(property.get().copy(argb = it)) })

    private fun gradientEnd(key: String, default: HudColor, property: KMutableProperty0<HudColor>): Option<Color> =
        colorOption(key, default.gradientArgb, { property.get().gradientArgb }, { property.set(property.get().copy(gradientArgb = it)) })

    private fun gradientToggle(key: String, default: HudColor, property: KMutableProperty0<HudColor>): Option<Boolean> =
        Option.createBuilder<Boolean>()
            .name(text(key))
            .description(describe(key))
            .binding(default.gradient, { property.get().gradient }, { property.set(property.get().copy(gradient = it)) })
            .controller { BooleanControllerBuilder.create(it).coloured(true) }
            .build()

    private fun colorOption(key: String, default: Int, get: () -> Int, set: (Int) -> Unit): Option<Color> =
        Option.createBuilder<Color>()
            .name(text(key))
            .description(describe(key))
            .binding(Color(default, true), { Color(get(), true) }, { set(it.rgb) })
            .controller { ColorControllerBuilder.create(it).allowAlpha(true) }
            .build()
}
