package com.classmate.core.parser

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

    fun extract(text: String): String? {
        val t = text.trim()
        if (t.isEmpty()) return null

        extractFenced(t)?.let { return it }
        if (t.startsWith("{") && t.endsWith("}")) return t
        return firstBalancedObject(t)
    }

    private fun extractFenced(text: String): String? {
        val inner = fence.find(text)?.groupValues?.get(1)?.trim() ?: return null
        return if (inner.startsWith("{")) firstBalancedObject(inner) ?: inner else null
    }

    private fun firstBalancedObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
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
