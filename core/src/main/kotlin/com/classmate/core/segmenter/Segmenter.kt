package com.classmate.core.segmenter

import com.classmate.core.model.InputSegment

/**
 * Rule-based segmenter — spec §9.
 *
 * v0.2.5 status: STUB. The probe doesn't need real segmentation; the demo
 * pipeline ships pre-segmented data. This file exists so v0.3 has a known
 * landing spot rather than scattering segmentation logic across UI code.
 *
 * Planned priorities (do NOT call from production code yet):
 *   1. If the text carries timestamps, split by timestamp.
 *   2. Else split by paragraph, merging to 150-300 characters.
 *   3. Else fall back to punctuation + length.
 */
object Segmenter {

    /**
     * For v0.2.5, returns a single segment wrapping the whole text. Sufficient
     * for the home-screen smoke test; not sufficient for any real run.
     */
    fun segment(rawText: String): List<InputSegment> {
        if (rawText.isBlank()) return emptyList()
        return listOf(
            InputSegment(
                segment_id = "seg_001",
                time_range = "00:00-??",
                text = rawText.trim()
            )
        )
    }
}
