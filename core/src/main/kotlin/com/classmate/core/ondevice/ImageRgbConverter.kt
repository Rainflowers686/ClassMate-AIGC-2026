package com.classmate.core.ondevice

/** A decoded RGB image ready for the on-device VIT encoder (callVit). [bytes] is width*height*3. */
data class RgbImage(val width: Int, val height: Int, val bytes: ByteArray) {
    init {
        require(bytes.size == width * height * 3) {
            "RgbImage bytes must be width*height*3"
        }
    }

    // ByteArray needs structural equals/hashCode for value semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RgbImage) return false
        return width == other.width && height == other.height && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = (width * 31 + height) * 31 + bytes.contentHashCode()
}

/**
 * Pure ARGB_8888 → packed RGB conversion for the vivo on-device multimodal path (P4). The Android
 * glue (`Bitmap.getPixels`) lives app-side in `BitmapToRgb`; the actual math is here so it is fully
 * unit-testable without an Android runtime.
 *
 * Contract (vendor): output length = width*height*3, byte order R, G, B per pixel, alpha discarded.
 * Android packs ARGB_8888 pixels as 0xAARRGGBB.
 */
object ImageRgbConverter {

    fun argbToRgb(argb: IntArray, width: Int, height: Int): ByteArray {
        require(width > 0 && height > 0) { "width/height must be positive" }
        require(argb.size == width * height) { "argb length must equal width*height" }
        val out = ByteArray(width * height * 3)
        var o = 0
        for (pixel in argb) {
            out[o++] = ((pixel ushr 16) and 0xFF).toByte() // R
            out[o++] = ((pixel ushr 8) and 0xFF).toByte()  // G
            out[o++] = (pixel and 0xFF).toByte()           // B
            // alpha (pixel ushr 24) intentionally dropped
        }
        return out
    }

    fun toRgbImage(argb: IntArray, width: Int, height: Int): RgbImage =
        RgbImage(width, height, argbToRgb(argb, width, height))
}
