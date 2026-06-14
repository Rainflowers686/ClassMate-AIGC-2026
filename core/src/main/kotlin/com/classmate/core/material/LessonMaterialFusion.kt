package com.classmate.core.material

/**
 * Pure, deterministic fusion of multiple [MaterialSource]s into one [LessonMaterialBundle].
 *
 * Guarantees:
 *  - Stable order: sources keep their input order; segments keep their per-source order.
 *  - No evidence loss: every fused segment keeps its [MaterialEvidenceRef] (source type + id, and
 *    where available a transcript time range, OCR page/block, or note id).
 *  - Analyzer-ready: [LessonMaterialBundle.plainText] yields clean classroom text (with safe source
 *    markers) suitable for the EXISTING CourseAnalyzer — fusion never calls a model or a network.
 *
 * It does not modify the analysis pipeline; it only prepares its input.
 */
object LessonMaterialFusionEngine {

    fun fuse(
        id: String,
        courseTitle: String,
        sources: List<MaterialSource>,
        subject: String = "",
        glossary: TermGlossary = TermGlossary(),
        now: Long = 0L,
    ): LessonMaterialBundle {
        // Keep only sources that actually carry text, preserving input order.
        val usable = sources.filter { src -> src.segments.any { it.text.isNotBlank() } }
        val warnings = buildList {
            if (sources.isEmpty()) add("没有任何课堂材料来源。")
            else if (usable.isEmpty()) add("所有材料来源都为空，没有可分析文本。")
        }
        return LessonMaterialBundle(
            id = id,
            courseTitle = courseTitle.ifBlank { "未命名课程" },
            subject = subject,
            createdAt = now,
            sources = usable,
            glossary = glossary,
            fusionWarnings = warnings,
        )
    }

    /**
     * Convenience bridge: the fused [LessonMaterialBundle.plainText] is exactly the `rawText` the app
     * already feeds to `CourseSegmenter.buildSession(...)` → `CourseAnalyzer`. This keeps the existing
     * analysis path unchanged; multimodal input simply produces its classroom text a new way.
     */
    fun analyzerPlainText(bundle: LessonMaterialBundle): String = bundle.plainText()
}
