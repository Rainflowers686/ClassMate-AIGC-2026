package com.classmate.core.adapter

/**
 * Pulls the first complete JSON object out of a model response.
 *
 * Real model output is messy:
 *   - pure JSON (best case)
 *   - JSON wrapped in ```json … ``` fences
 *   - JSON preceded / followed by chatty prose
 *   - JSON with trailing commentary like "Hope this helps!"
 *
 * This extractor handles all three by scanning for the first `{` and walking
 * forward with a brace-depth counter, ignoring braces that appear inside
 * string literals (so `"text":"a{b}c"` doesn't fool it).
 *
 * Out of scope: JSON arrays at the top level (CourseAnalysisResult is always
 * an object), comments, unbalanced inputs.
 */
object JsonExtractor {

    /**
     * Returns the first balanced JSON object substring of [raw], or throws
     * [ModelCallException] with reason=JSON_EXTRACTION_FAILED if none is found.
     */
    fun extract(raw: String): String {
        if (raw.isBlank()) {
            throw ModelCallException(
                ModelCallException.Reason.JSON_EXTRACTION_FAILED,
                "model response was blank"
            )
        }

        // Strip ```json fences first — cheap, handles the common case directly.
        val stripped = stripCodeFence(raw)

        val start = stripped.indexOf('{')
        if (start < 0) {
            throw ModelCallException(
                ModelCallException.Reason.JSON_EXTRACTION_FAILED,
                "no '{' found in model response (length=${raw.length})"
            )
        }

        var depth = 0
        var inString = false
        var escape = false
        for (i in start until stripped.length) {
            val c = stripped[i]
            if (escape) {
                escape = false
                continue
            }
            if (inString) {
                when (c) {
                    '\\' -> escape = true
                    '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return stripped.substring(start, i + 1)
                    }
                }
            }
        }

        throw ModelCallException(
            ModelCallException.Reason.JSON_EXTRACTION_FAILED,
            "unterminated JSON object starting at offset $start (depth=$depth at end)"
        )
    }

    /**
     * Removes the ```json … ``` (or plain ``` … ```) wrapper if present.
     * Only strips the FIRST fence — the inner brace walker handles the rest.
     */
    private fun stripCodeFence(raw: String): String {
        val fenceStart = raw.indexOf("```")
        if (fenceStart < 0) return raw
        val afterFence = raw.indexOf('\n', fenceStart)
        if (afterFence < 0) return raw
        val end = raw.indexOf("```", afterFence + 1)
        return if (end < 0) raw.substring(afterFence + 1) else raw.substring(afterFence + 1, end)
    }
}
