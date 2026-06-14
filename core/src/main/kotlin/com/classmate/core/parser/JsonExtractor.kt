package com.classmate.core.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

enum class JsonParseStrategy {
    PURE_JSON,
    FENCED_JSON,
    BRACE_SLICE,
    FAILED,
}

data class JsonExtractionResult(
    val jsonText: String,
    val strategy: JsonParseStrategy,
)

/**
 * Pulls a single JSON object out of model text. Models often wrap JSON in ```json fences or
 * add a stray sentence; this recovers the object in three escalating ways:
 *  1. a fenced ```json ... ``` (or ``` ... ```) block,
 *  2. the whole string if it is already pure JSON,
 *  3. the first brace-balanced object found (string-/escape-aware).
 * Returns null when nothing JSON-like is present (the caller then triggers fallback).
 */
object JsonExtractor {

    private val fence = Regex("```(?:json|JSON)?\\s*([\\s\\S]*?)```")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun extract(text: String): String? = extractWithStrategy(text)?.jsonText

    fun extractWithStrategy(text: String): JsonExtractionResult? {
        val t = text.trim()
        if (t.isEmpty()) return null

        extractFenced(t)?.let { return JsonExtractionResult(it, JsonParseStrategy.FENCED_JSON) }
        if (t.startsWith("[") && t.endsWith("]")) return null
        if (t.startsWith("{") && t.endsWith("}") && parseObjectOrNull(t) != null) {
            return JsonExtractionResult(t, JsonParseStrategy.PURE_JSON)
        }
        return firstToLastBraceObject(t)?.let { JsonExtractionResult(it, JsonParseStrategy.BRACE_SLICE) }
    }

    private fun extractFenced(text: String): String? {
        val inner = fence.find(text)?.groupValues?.get(1)?.trim() ?: return null
        if (inner.startsWith("[") && inner.endsWith("]")) return null
        return firstToLastBraceObject(inner)
    }

    private fun firstToLastBraceObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val candidate = text.substring(start, end + 1).trim()
        return candidate.takeIf { parseObjectOrNull(it) != null }
    }

    private fun parseObjectOrNull(text: String): JsonObject? =
        try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: Exception) {
            null
        }

    @Suppress("unused")
    private fun firstBalancedObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0 || text.trimStart().startsWith("[")) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return text.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }
}
