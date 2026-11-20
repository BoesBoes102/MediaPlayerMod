package dev.boes.mediaplayer.hud

import dev.boes.mediaplayer.config.HudLayoutPreset
import dev.boes.mediaplayer.config.MediaPlayerConfig

data class Rect(val x: Int, val y: Int, val width: Int, val height: Int) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height
    val centerX: Int get() = x + width / 2

    fun contains(px: Double, py: Double): Boolean = px >= x && px < right && py >= y && py < bottom

    fun expand(amount: Int): Rect = Rect(x - amount, y - amount, width + amount * 2, height + amount * 2)
}

enum class HudControl {
    PREVIOUS,
    PLAY_PAUSE,
    NEXT,
    VOLUME,
}

class HudLayout(
    val width: Int,
    val height: Int,
    val compact: Boolean,
    val cover: Rect?,
    val title: Rect?,
    val artist: Rect?,
    val album: Rect?,
    val progressBar: Rect?,
    val elapsed: Rect?,
    val duration: Rect?,
    val controls: Map<HudControl, Rect>,
    val volumeSlider: Rect?,
) {
    companion object {
        private const val FULL_PADDING = 6
        private const val FULL_COVER = 40
        private const val LINE_HEIGHT = 11
        private const val SECTION_GAP = 6
        private const val BUTTON = 14
        private const val BUTTON_GAP = 6
        private const val VOLUME_ICON = 12
        private const val VOLUME_SLIDER = 34

        private const val COMPACT_PADDING = 4
        private const val COMPACT_ROW = 14
        private const val COMPACT_BUTTON = 12
        private const val COMPACT_GAP = 2
        private const val COMPACT_MIN_TEXT = 70

        fun compute(config: MediaPlayerConfig, timestampWidth: Int, compactTimestampWidth: Int): HudLayout =
            when (config.layout) {
                HudLayoutPreset.FULL -> full(config, timestampWidth)
                HudLayoutPreset.COMPACT -> compact(config, compactTimestampWidth)
            }

        private fun full(config: MediaPlayerConfig, timestampWidth: Int): HudLayout {
            val p = FULL_PADDING
            val w = config.width
            var y = p

            val lineCount = listOf(config.showTitle, config.showArtist, config.showAlbum).count { it }
            val textBlock = if (lineCount > 0) lineCount * LINE_HEIGHT - 2 else 0
            val coverSize = if (config.showCover) FULL_COVER else 0
            val top = maxOf(coverSize, textBlock)
            val textX = if (config.showCover) p + coverSize + 6 else p
            val textWidth = w - p - textX

            val cover = if (config.showCover) Rect(p, y + (top - coverSize) / 2, coverSize, coverSize) else null
            var lineY = y + (top - textBlock) / 2
            fun nextLine(enabled: Boolean): Rect? {
                if (!enabled) return null
                return Rect(textX, lineY, textWidth, 9).also { lineY += LINE_HEIGHT }
            }
            val title = nextLine(config.showTitle)
            val artist = nextLine(config.showArtist)
            val album = nextLine(config.showAlbum)
            y += top

            var progress: Rect? = null
            var elapsed: Rect? = null
            var duration: Rect? = null
            if (config.showProgressBar || config.showTimestamps) {
                if (top > 0) y += SECTION_GAP
                if (config.showTimestamps) {
                    elapsed = Rect(p, y, timestampWidth, 9)
                    duration = Rect(w - p - timestampWidth, y, timestampWidth, 9)
                }
                if (config.showProgressBar) {
                    progress = if (config.showTimestamps) {
                        Rect(p + timestampWidth + 5, y + 3, w - 2 * p - 2 * timestampWidth - 10, 3)
                    } else {
                        Rect(p, y, w - 2 * p, 3)
                    }
                }
                y += if (config.showTimestamps) 9 else 3
            }

            val controls = linkedMapOf<HudControl, Rect>()
            var slider: Rect? = null
            if (config.showControls || config.showVolume) {
                if (y > p) y += SECTION_GAP
                var areaEnd = w - p
                if (config.showVolume) {
                    val groupX = w - p - VOLUME_ICON - 4 - VOLUME_SLIDER
                    controls[HudControl.VOLUME] = Rect(groupX, y + 1, VOLUME_ICON, VOLUME_ICON)
                    slider = Rect(groupX + VOLUME_ICON + 4, y + 5, VOLUME_SLIDER, 4)
                    areaEnd = groupX - 4
                }
                if (config.showControls) {
                    val controlsWidth = BUTTON * 3 + BUTTON_GAP * 2
                    val centered = (w - controlsWidth) / 2
                    val start = if (centered + controlsWidth <= areaEnd) centered else p + (areaEnd - p - controlsWidth) / 2
                    controls[HudControl.PREVIOUS] = Rect(start, y, BUTTON, BUTTON)
                    controls[HudControl.PLAY_PAUSE] = Rect(start + BUTTON + BUTTON_GAP, y, BUTTON, BUTTON)
                    controls[HudControl.NEXT] = Rect(start + 2 * (BUTTON + BUTTON_GAP), y, BUTTON, BUTTON)
                }
                y += BUTTON
            }

            return HudLayout(w, y + p, false, cover, title, artist, album, progress, elapsed, duration, controls, slider)
        }

        private fun compact(config: MediaPlayerConfig, timestampWidth: Int): HudLayout {
            val p = COMPACT_PADDING
            val w = config.width
            val rowY = p
            var x = p

            val cover = if (config.showCover) Rect(x, rowY, COMPACT_ROW, COMPACT_ROW).also { x += COMPACT_ROW + 4 } else null

            val controls = linkedMapOf<HudControl, Rect>()
            var right = w - p
            val buttonY = rowY + (COMPACT_ROW - COMPACT_BUTTON) / 2
            if (config.showVolume) {
                right -= COMPACT_BUTTON
                controls[HudControl.VOLUME] = Rect(right, buttonY, COMPACT_BUTTON, COMPACT_BUTTON)
                right -= 4
            }
            if (config.showControls) {
                right -= COMPACT_BUTTON
                controls[HudControl.NEXT] = Rect(right, buttonY, COMPACT_BUTTON, COMPACT_BUTTON)
                right -= COMPACT_GAP + COMPACT_BUTTON
                controls[HudControl.PLAY_PAUSE] = Rect(right, buttonY, COMPACT_BUTTON, COMPACT_BUTTON)
                right -= COMPACT_GAP + COMPACT_BUTTON
                controls[HudControl.PREVIOUS] = Rect(right, buttonY, COMPACT_BUTTON, COMPACT_BUTTON)
                right -= 4
            }

            val textY = rowY + (COMPACT_ROW - 8) / 2
            var elapsed: Rect? = null
            if (config.showTimestamps && right - timestampWidth - 4 - x >= COMPACT_MIN_TEXT) {
                right -= timestampWidth
                elapsed = Rect(right, textY, timestampWidth, 9)
                right -= 4
            }

            val title = if (config.showTitle || config.showArtist) Rect(x, textY, (right - x).coerceAtLeast(0), 9) else null

            var y = rowY + COMPACT_ROW
            var progress: Rect? = null
            if (config.showProgressBar) {
                y += 3
                progress = Rect(p, y, w - 2 * p, 2)
                y += 2
            }

            return HudLayout(w, y + p, true, cover, title, null, null, progress, elapsed, null, controls, null)
        }
    }
}
