package dev.boes.mediaplayer.config

class ThemeColors(
    val background: HudColor,
    val text: HudColor,
    val secondaryText: HudColor,
    val accent: HudColor,
    val track: HudColor,
    val icon: HudColor,
)

enum class HudTheme(val translationKey: String, val colors: ThemeColors?) {
    CLASSIC(
        "mediaplayer.theme.classic",
        ThemeColors(
            background = HudColor(0xC0101018.toInt()),
            text = HudColor(0xFFFFFFFF.toInt()),
            secondaryText = HudColor(0xFFB4B4C0.toInt()),
            accent = HudColor(0xFF1DB954.toInt()),
            track = HudColor(0x50FFFFFF),
            icon = HudColor(0xFFFFFFFF.toInt()),
        ),
    ),
    SUNSET(
        "mediaplayer.theme.sunset",
        ThemeColors(
            background = HudColor.gradient(0xD03A1C5C.toInt(), 0xD0A33A3A.toInt()),
            text = HudColor(0xFFFFF4EC.toInt()),
            secondaryText = HudColor(0xFFFFC9A8.toInt()),
            accent = HudColor.gradient(0xFFFF5F6D.toInt(), 0xFFFFC371.toInt()),
            track = HudColor(0x50FFE0D0),
            icon = HudColor(0xFFFFE6D6.toInt()),
        ),
    ),
    PINK(
        "mediaplayer.theme.pink",
        ThemeColors(
            background = HudColor.gradient(0xD04A1240.toInt(), 0xD08C2A6E.toInt()),
            text = HudColor(0xFFFFF0FA.toInt()),
            secondaryText = HudColor(0xFFF7B8E0.toInt()),
            accent = HudColor.gradient(0xFFFF6FD8.toInt(), 0xFFFFB6E1.toInt()),
            track = HudColor(0x50FFD6F0),
            icon = HudColor(0xFFFFE0F4.toInt()),
        ),
    ),
    RED(
        "mediaplayer.theme.red",
        ThemeColors(
            background = HudColor.gradient(0xD0300808.toInt(), 0xD0701414.toInt()),
            text = HudColor(0xFFFFF0F0.toInt()),
            secondaryText = HudColor(0xFFFFB0B0.toInt()),
            accent = HudColor.gradient(0xFFFF416C.toInt(), 0xFFFF4B2B.toInt()),
            track = HudColor(0x50FFD0D0),
            icon = HudColor(0xFFFFE0E0.toInt()),
        ),
    ),
    ORANGE(
        "mediaplayer.theme.orange",
        ThemeColors(
            background = HudColor.gradient(0xD0331804.toInt(), 0xD0703808.toInt()),
            text = HudColor(0xFFFFF6E8.toInt()),
            secondaryText = HudColor(0xFFFFD39A.toInt()),
            accent = HudColor.gradient(0xFFF7971E.toInt(), 0xFFFFD200.toInt()),
            track = HudColor(0x50FFE8C8),
            icon = HudColor(0xFFFFEBD0.toInt()),
        ),
    ),
    OCEAN(
        "mediaplayer.theme.ocean",
        ThemeColors(
            background = HudColor.gradient(0xD0061A33.toInt(), 0xD00E4766.toInt()),
            text = HudColor(0xFFEFFAFF.toInt()),
            secondaryText = HudColor(0xFFA8D8EE.toInt()),
            accent = HudColor.gradient(0xFF2193B0.toInt(), 0xFF6DD5ED.toInt()),
            track = HudColor(0x50D0F0FF),
            icon = HudColor(0xFFE0F6FF.toInt()),
        ),
    ),
    PURPLE(
        "mediaplayer.theme.purple",
        ThemeColors(
            background = HudColor.gradient(0xD01E0B3D.toInt(), 0xD04B1C7A.toInt()),
            text = HudColor(0xFFF6F0FF.toInt()),
            secondaryText = HudColor(0xFFC9B4F2.toInt()),
            accent = HudColor.gradient(0xFF8E2DE2.toInt(), 0xFFC471F5.toInt()),
            track = HudColor(0x50E0D0FF),
            icon = HudColor(0xFFEDE0FF.toInt()),
        ),
    ),
    AURORA(
        "mediaplayer.theme.aurora",
        ThemeColors(
            background = HudColor.gradient(0xD0062A2A.toInt(), 0xD0102850.toInt()),
            text = HudColor(0xFFF0FFF8.toInt()),
            secondaryText = HudColor(0xFFA8EBD3.toInt()),
            accent = HudColor.gradient(0xFF00F260.toInt(), 0xFF0575E6.toInt()),
            track = HudColor(0x50D0FFF0),
            icon = HudColor(0xFFE0FFF4.toInt()),
        ),
    ),
    RAINBOW(
        "mediaplayer.theme.rainbow",
        ThemeColors(
            background = HudColor(0xC0101018.toInt()),
            text = HudColor(0xFFFFFFFF.toInt()),
            secondaryText = HudColor(0xFFB4B4C0.toInt()),
            accent = HudColor(0xFFFF4040.toInt(), chroma = true, gradient = true, gradientArgb = 0xFF40A0FF.toInt()),
            track = HudColor(0x50FFFFFF),
            icon = HudColor(0xFFFFFFFF.toInt()),
        ),
    ),
    CUSTOM("mediaplayer.theme.custom", null);

    val selectable: Boolean get() = colors != null

    companion object {
        val selectableEntries: List<HudTheme> get() = entries.filter { it.selectable }
    }
}
