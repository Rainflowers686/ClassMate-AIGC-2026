package com.classmate.core.ondevice

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageRgbConverterTest {

    @Test
    fun outputLengthIsWidthTimesHeightTimesThree() {
        val argb = IntArray(2 * 3) { 0xFF000000.toInt() }
        val rgb = ImageRgbConverter.argbToRgb(argb, width = 2, height = 3)
        assertEquals(2 * 3 * 3, rgb.size)
    }

    @Test
    fun channelOrderIsRgbAndAlphaIsDropped() {
        // One pixel: A=0x12 (ignored), R=0x34, G=0x56, B=0x78  -> 0x12345678
        val rgb = ImageRgbConverter.argbToRgb(intArrayOf(0x12345678), width = 1, height = 1)
        assertArrayEquals(byteArrayOf(0x34, 0x56, 0x78), rgb)
    }

    @Test
    fun pureColorsMapToExpectedBytes() {
        val argb = intArrayOf(
            0xFFFF0000.toInt(), // red
            0xFF00FF00.toInt(), // green
            0xFF0000FF.toInt(), // blue
            0xFFFFFFFF.toInt(), // white
        )
        val rgb = ImageRgbConverter.argbToRgb(argb, width = 2, height = 2)
        assertArrayEquals(
            byteArrayOf(
                0xFF.toByte(), 0x00, 0x00, // red   -> R,G,B
                0x00, 0xFF.toByte(), 0x00, // green
                0x00, 0x00, 0xFF.toByte(), // blue
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
            ),
            rgb,
        )
    }

    @Test
    fun rgbImageEnforcesByteLengthInvariant() {
        assertThrows(IllegalArgumentException::class.java) {
            RgbImage(width = 2, height = 2, bytes = ByteArray(5))
        }
        val ok = ImageRgbConverter.toRgbImage(IntArray(4) { 0 }, 2, 2)
        assertEquals(12, ok.bytes.size)
        assertEquals(RgbImage(2, 2, ByteArray(12)), ok)
    }

    @Test
    fun mismatchedDimensionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageRgbConverter.argbToRgb(IntArray(3), width = 2, height = 2)
        }
    }
}
