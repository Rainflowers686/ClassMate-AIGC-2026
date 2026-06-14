package com.classmate.app.ondevice

import android.graphics.Bitmap
import com.classmate.core.ondevice.ImageRgbConverter
import com.classmate.core.ondevice.RgbImage

/**
 * Android glue that turns a [Bitmap] into the packed RGB buffer the vivo VIT encoder expects. The
 * actual ARGB→RGB math lives in the pure-core [ImageRgbConverter] (unit-tested without Android); this
 * object only extracts ARGB_8888 pixels via `Bitmap.getPixels`.
 *
 * We never read images from the gallery here and request no media permission — the multimodal probe
 * uses a tiny built-in [diagnosticTestImage].
 */
object BitmapToRgb {

    fun toRgb(bitmap: Bitmap): RgbImage {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        // ARGB_8888 ints (0xAARRGGBB); ImageRgbConverter drops alpha and emits R,G,B.
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        return ImageRgbConverter.toRgbImage(pixels, w, h)
    }

    /**
     * Downscale (never upscale) so the longest edge is ≤ [maxEdge], then convert to RGB. Keeps the
     * RGB buffer bounded for real photos (memory + callVit cost) without changing the converter math.
     */
    fun toRgbScaled(bitmap: Bitmap, maxEdge: Int = DEFAULT_MAX_EDGE): RgbImage {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge || longest == 0) return toRgb(bitmap)
        val scale = maxEdge.toFloat() / longest
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        return toRgb(scaled)
    }

    const val DEFAULT_MAX_EDGE = 512

    /**
     * A deterministic 2x2 ARGB test image used by the multimodal diagnostic (P4). Built directly as
     * ARGB ints so it needs no Android Bitmap — keeps the probe testable. 4 px → 12 RGB bytes.
     */
    fun diagnosticTestImage(): RgbImage {
        val argb = intArrayOf(
            0xFFFF0000.toInt(), // red
            0xFF00FF00.toInt(), // green
            0xFF0000FF.toInt(), // blue
            0xFFFFFFFF.toInt(), // white
        )
        return ImageRgbConverter.toRgbImage(argb, width = 2, height = 2)
    }
}
