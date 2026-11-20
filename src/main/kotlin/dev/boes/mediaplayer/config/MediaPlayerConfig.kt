package dev.boes.mediaplayer.config

import kotlin.reflect.KMutableProperty1

enum class HudLayoutPreset(val translationKey: String) {
    FULL("mediaplayer.layout.full"),
    COMPACT("mediaplayer.layout.compact"),
}

class MediaPlayerConfig {
    var enabled = true
    var layout = HudLayoutPreset.FULL
    var positionX = 0.01f
    var positionY = 0.02f
    var scale = 1.0f
    var width = 180

    var showCover = true
    var showTitle = true
    var showArtist = true
    var showAlbum = false
    var showProgressBar = true
    var showTimestamps = true
    var showControls = true
    var showVolume = true

    var showBackground = true
    var theme = HudTheme.CLASSIC
    var backgroundColor = DEFAULT_THEME.background
    var textColor = DEFAULT_THEME.text
    var secondaryTextColor = DEFAULT_THEME.secondaryText
    var accentColor = DEFAULT_THEME.accent
    var trackColor = DEFAULT_THEME.track
    var iconColor = DEFAULT_THEME.icon
    var roundedCorners = true
    var cornerRadius = 4
    var textShadow = true
    var scrollLongText = true

    var hideWhenNothingPlaying = true
    var hideWhenPaused = false
    var hideWhenPausedDelaySeconds = 10

    val effectiveCornerRadius: Int get() = if (roundedCorners) cornerRadius else 0

    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    fun sanitize(): MediaPlayerConfig = apply {
        layout = layout ?: HudLayoutPreset.FULL
        theme = theme ?: HudTheme.CUSTOM
        backgroundColor = backgroundColor ?: DEFAULT_THEME.background
        textColor = textColor ?: DEFAULT_THEME.text
        secondaryTextColor = secondaryTextColor ?: DEFAULT_THEME.secondaryText
        accentColor = accentColor ?: DEFAULT_THEME.accent
        trackColor = trackColor ?: DEFAULT_THEME.track
        iconColor = iconColor ?: DEFAULT_THEME.icon
        positionX = positionX.coerceIn(0f, 1f)
        positionY = positionY.coerceIn(0f, 1f)
        scale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        width = width.coerceIn(MIN_WIDTH, MAX_WIDTH)
        cornerRadius = cornerRadius.coerceIn(1, MAX_CORNER_RADIUS)
        hideWhenPausedDelaySeconds = hideWhenPausedDelaySeconds.coerceIn(0, MAX_HIDE_DELAY_SECONDS)
        if (!matchesTheme(theme)) theme = HudTheme.CUSTOM
    }

    fun applyTheme(selected: HudTheme) {
        theme = selected
        val colors = selected.colors ?: return
        backgroundColor = colors.background
        textColor = colors.text
        secondaryTextColor = colors.secondaryText
        accentColor = colors.accent
        trackColor = colors.track
        iconColor = colors.icon
    }

    fun setCornerRounding(radius: Int) {
        roundedCorners = radius > 0
        if (radius > 0) cornerRadius = radius.coerceAtMost(MAX_CORNER_RADIUS)
    }

    fun reset(vararg sections: ConfigSection) {
        val defaults = MediaPlayerConfig()
        sections.forEach { section -> section.fields.forEach { it.copy(defaults, this) } }
    }

    fun resetAll() = reset(*ConfigSection.entries.toTypedArray())

    private fun matchesTheme(candidate: HudTheme): Boolean {
        val colors = candidate.colors ?: return true
        return backgroundColor == colors.background &&
            textColor == colors.text &&
            secondaryTextColor == colors.secondaryText &&
            accentColor == colors.accent &&
            trackColor == colors.track &&
            iconColor == colors.icon
    }

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 3.0f
        const val MIN_WIDTH = 140
        const val MAX_WIDTH = 320
        const val MAX_CORNER_RADIUS = 20
        const val MAX_HIDE_DELAY_SECONDS = 120
        private val DEFAULT_THEME = requireNotNull(HudTheme.CLASSIC.colors)
    }
}

class ConfigField<T>(private val property: KMutableProperty1<MediaPlayerConfig, T>) {
    fun copy(from: MediaPlayerConfig, to: MediaPlayerConfig) = property.set(to, property.get(from))
}

enum class ConfigSection(vararg val fields: ConfigField<*>) {
    PLACEMENT(
        ConfigField(MediaPlayerConfig::positionX),
        ConfigField(MediaPlayerConfig::positionY),
        ConfigField(MediaPlayerConfig::scale),
    ),
    HUD(
        ConfigField(MediaPlayerConfig::enabled),
        ConfigField(MediaPlayerConfig::layout),
        ConfigField(MediaPlayerConfig::width),
    ),
    VISIBILITY(
        ConfigField(MediaPlayerConfig::hideWhenNothingPlaying),
        ConfigField(MediaPlayerConfig::hideWhenPaused),
        ConfigField(MediaPlayerConfig::hideWhenPausedDelaySeconds),
    ),
    ELEMENTS(
        ConfigField(MediaPlayerConfig::showCover),
        ConfigField(MediaPlayerConfig::showTitle),
        ConfigField(MediaPlayerConfig::showArtist),
        ConfigField(MediaPlayerConfig::showAlbum),
        ConfigField(MediaPlayerConfig::showProgressBar),
        ConfigField(MediaPlayerConfig::showTimestamps),
        ConfigField(MediaPlayerConfig::showControls),
        ConfigField(MediaPlayerConfig::showVolume),
    ),
    SHAPE(
        ConfigField(MediaPlayerConfig::showBackground),
        ConfigField(MediaPlayerConfig::roundedCorners),
        ConfigField(MediaPlayerConfig::cornerRadius),
    ),
    COLORS(
        ConfigField(MediaPlayerConfig::theme),
        ConfigField(MediaPlayerConfig::backgroundColor),
        ConfigField(MediaPlayerConfig::textColor),
        ConfigField(MediaPlayerConfig::secondaryTextColor),
        ConfigField(MediaPlayerConfig::accentColor),
        ConfigField(MediaPlayerConfig::trackColor),
        ConfigField(MediaPlayerConfig::iconColor),
    ),
    TEXT(
        ConfigField(MediaPlayerConfig::textShadow),
        ConfigField(MediaPlayerConfig::scrollLongText),
    ),
}
