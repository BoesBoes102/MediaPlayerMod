package dev.boes.mediaplayer.config

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.awt.Color
import java.lang.reflect.Type

data class HudColor(
    val argb: Int = 0xFFFFFFFF.toInt(),
    val chroma: Boolean = false,
    val chromaSpeed: Float = 1f,
    val gradient: Boolean = false,
    val gradientArgb: Int = argb,
) {
    val start: Int get() = resolve(argb)
    val end: Int get() = if (gradient) resolve(gradientArgb) else start

    fun at(fraction: Float): Int = if (gradient) lerp(start, end, fraction) else start

    private fun resolve(color: Int): Int = if (chroma) rotateHue(color, chromaTurn(chromaSpeed)) else color

    companion object {
        private const val CHROMA_CYCLE_SECONDS = 10.0
        private const val GRAY_SATURATION = 0.05f

        fun gradient(from: Int, to: Int) = HudColor(from, gradient = true, gradientArgb = to)

        fun chromaTurn(speed: Float): Float =
            (System.nanoTime() / 1_000_000_000.0 * speed.coerceAtLeast(0f) / CHROMA_CYCLE_SECONDS % 1.0).toFloat()

        fun lerp(from: Int, to: Int, fraction: Float): Int {
            val t = fraction.coerceIn(0f, 1f)
            fun channel(shift: Int): Int {
                val a = (from ushr shift) and 0xFF
                val b = (to ushr shift) and 0xFF
                return (a + (b - a) * t).toInt().coerceIn(0, 255) shl shift
            }
            return channel(24) or channel(16) or channel(8) or channel(0)
        }

        private fun rotateHue(color: Int, turn: Float): Int {
            val hsb = Color.RGBtoHSB((color ushr 16) and 0xFF, (color ushr 8) and 0xFF, color and 0xFF, null)
            val saturation = if (hsb[1] < GRAY_SATURATION) 1f else hsb[1]
            val rgb = Color.HSBtoRGB((hsb[0] + turn) % 1f, saturation, hsb[2])
            return (color and 0xFF000000.toInt()) or (rgb and 0xFFFFFF)
        }
    }

    object Deserializer : JsonDeserializer<HudColor> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): HudColor {
            if (json.isJsonPrimitive) return HudColor(json.asLong.toInt())
            val obj = json.asJsonObject
            val argb = obj.int("argb") ?: 0xFFFFFFFF.toInt()
            return HudColor(
                argb = argb,
                chroma = obj.get("chroma")?.asBoolean ?: false,
                chromaSpeed = obj.get("chromaSpeed")?.asFloat ?: 1f,
                gradient = obj.get("gradient")?.asBoolean ?: false,
                gradientArgb = obj.int("gradientArgb") ?: argb,
            )
        }

        private fun JsonObject.int(key: String): Int? = get(key)?.asLong?.toInt()
    }
}
