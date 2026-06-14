package com.classmate.core.importing

/**
 * Converts Markdown into clean, analysis-ready text. The real-device bug was that raw `.md` markup
 * (`#`, `|`, fenced code, image/link syntax) was fed straight into the course text, polluting analysis
 * and sometimes reading like garbage. This normalizes structure to plain/semi-structured text:
 *
 *  - headings              -> the heading text on its own line
 *  - bullet lists          -> "• text"; ordered lists kept
 *  - tables                -> cells joined with a full-width separator; separator rows dropped
 *  - fenced code            -> kept verbatim under a "[代码]" marker (fences removed, no pollution)
 *  - math `$...$` / `$$..$$`-> kept verbatim (NOT stripped)
 *  - images                -> "[图片: alt]" (or "[图片]")
 *  - links                 -> the link text only
 *  - emphasis markers       -> stripped, text kept
 *
 * Pure, deterministic, no network, no credentials.
 */
object MarkdownPlainText {

    private val HEADING = Regex("""^\s{0,3}#{1,6}\s+""")
    private val BULLET = Regex("""^(\s*)[-*+]\s+""")
    private val BLOCKQUOTE = Regex("""^\s*>+\s?""")
    private val HRULE = Regex("""^\s*([-*_])\s*(\1\s*){2,}$""")
    private val TABLE_SEP = Regex("""^\s*\|?\s*:?-{1,}:?\s*(\|\s*:?-{1,}:?\s*)+\|?\s*$""")
    private val IMAGE = Regex("""!\[([^\]]*)]\([^)]*\)""")
    private val LINK = Regex("""\[([^\]]+)]\((?:[^)]*)\)""")
    private val MATH_BLOCK = Regex("""\$\$.*?\$\$""", RegexOption.DOT_MATCHES_ALL)
    private val MATH_INLINE = Regex("""\$[^\$\n]+\$""")

    fun toPlainText(markdown: String): String {
        val protectedMath = ArrayList<String>()
        var src = markdown.replace("﻿", "").replace("\r\n", "\n").replace("\r", "\n")

        // Protect math spans first so later emphasis/marker cleanup never touches a formula.
        src = MATH_BLOCK.replace(src) { m -> protect(protectedMath, m.value) }
        src = MATH_INLINE.replace(src) { m -> protect(protectedMath, m.value) }

        val out = StringBuilder()
        var inFence = false
        for (raw in src.split("\n")) {
            val trimmedStart = raw.trimStart()
            if (trimmedStart.startsWith("```") || trimmedStart.startsWith("~~~")) {
                inFence = !inFence
                if (inFence) out.append("[代码]\n")
                continue
            }
            if (inFence) {
                out.append(raw).append('\n') // code content kept verbatim, fences removed
                continue
            }
            if (raw.isBlank()) { out.append('\n'); continue }
            if (HRULE.matches(raw)) { out.append('\n'); continue }

            if (raw.contains("|")) {
                if (TABLE_SEP.matches(raw)) continue
                val cells = raw.trim().trim('|').split("|").map { cleanInline(it.trim()) }.filter { it.isNotEmpty() }
                if (cells.isNotEmpty()) { out.append(cells.joinToString("　")).append('\n'); continue }
            }

            var line = raw
            line = HEADING.replaceFirst(line, "")
            line = BLOCKQUOTE.replaceFirst(line, "")
            line = BULLET.replace(line) { mr -> mr.groupValues[1] + "• " } // anchored ^, single match
            out.append(cleanInline(line)).append('\n')
        }

        var result = out.toString()
        protectedMath.forEachIndexed { i, original -> result = result.replace(token(i), original) }
        return result
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun cleanInline(text: String): String {
        var t = text
        t = IMAGE.replace(t) { m -> val alt = m.groupValues[1].trim(); if (alt.isEmpty()) "[图片]" else "[图片: $alt]" }
        t = LINK.replace(t) { m -> m.groupValues[1] }
        t = t.replace("**", "").replace("__", "").replace("~~", "").replace("`", "")
        return t.trim()
    }

    private fun protect(store: ArrayList<String>, value: String): String {
        store.add(value)
        return token(store.size - 1)
    }

    // Plain-ASCII sentinel that markdown text never contains and inline cleanup never alters.
    private fun token(i: Int): String = "@@CMATH" + i + "@@"
}
