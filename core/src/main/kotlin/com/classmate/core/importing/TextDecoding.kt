package com.classmate.core.importing

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Best-effort text decoding for imported files. The real-device bug was that .md/.txt files were read
 * as fixed UTF-8: a UTF-8 BOM leaked a stray char, and GBK/GB2312 Chinese files became mojibake.
 *
 * Strategy (no heavy charset detector): strip a UTF-8 BOM, try STRICT UTF-8 (reject malformed bytes),
 * then fall back to GB18030 (a superset of GBK/GB2312). Pure JVM, no Android, fully unit-testable.
 */
object TextDecoding {

    data class Decoded(val text: String, val charsetLabel: String)

    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun decodeBestEffort(bytes: ByteArray): Decoded {
        if (bytes.isEmpty()) return Decoded("", "empty")

        if (bytes.size >= 3 && bytes[0] == UTF8_BOM[0] && bytes[1] == UTF8_BOM[1] && bytes[2] == UTF8_BOM[2]) {
            val body = bytes.copyOfRange(3, bytes.size)
            return Decoded(strictUtf8(body) ?: lenientUtf8(body), "UTF-8 (BOM)")
        }
        strictUtf8(bytes)?.let { return Decoded(it, "UTF-8") }
        gb18030(bytes)?.let { return Decoded(it, "GB18030") }
        return Decoded(lenientUtf8(bytes), "UTF-8 (lossy)")
    }

    private fun strictUtf8(bytes: ByteArray): String? = try {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: Exception) {
        null
    }

    private fun gb18030(bytes: ByteArray): String? = try {
        val decoder = charset("GB18030").newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: Exception) {
        null
    }

    private fun lenientUtf8(bytes: ByteArray): String = String(bytes, Charsets.UTF_8)
}
