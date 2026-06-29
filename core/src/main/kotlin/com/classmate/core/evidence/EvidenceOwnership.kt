package com.classmate.core.evidence

/**
 * Ownership check for evidence shown from the current learning snapshot.
 *
 * The app does not have a global courseId on every legacy record, so this deliberately uses the
 * strongest local signals that are already persisted: current lesson/source id, current evidence
 * ids, asset ids, source metadata, and a retraceable excerpt. It is conservative: missing current
 * evidence, cross-course evidence, sample evidence bound to a real course, missing assets, and clear
 * metadata conflicts are MISSING. Incomplete but non-conflicting metadata is WEAK. Only in-snapshot,
 * source-matching evidence with a non-empty excerpt is STRONG.
 */
object EvidenceOwnership {

    data class Snapshot(
        val snapshotId: String,
        val lessonSourceId: String,
        val lessonTitle: String = "",
        val lessonSourceType: String = "",
        val isSampleCourse: Boolean = false,
        val evidence: List<EvidenceRecord> = emptyList(),
        val assets: List<AssetRecord> = emptyList(),
    )

    data class EvidenceRecord(
        val id: String,
        val sourceId: String,
        val sourceType: String,
        val assetId: String? = null,
        val sourceLabel: String = "",
        val fileName: String = "",
        val imageRef: String = "",
        val audioRef: String = "",
        val excerpt: String = "",
    )

    data class AssetRecord(
        val id: String,
        val sourceType: String,
        val sourceLabel: String = "",
        val fileName: String = "",
        val imageRef: String = "",
        val audioRef: String = "",
        val createdAt: Long = 0L,
    )

    fun assess(
        snapshot: Snapshot,
        evidenceId: String?,
        expectedLessonId: String? = null,
        expectedSourceLessonId: String? = null,
    ): EvidenceRelationLevel {
        val id = evidenceId?.trim().orEmpty()
        if (id.isBlank()) return EvidenceRelationLevel.MISSING
        val evidence = snapshot.evidence.firstOrNull { it.id == id }
            ?: return EvidenceRelationLevel.MISSING

        if (evidence.excerpt.isBlank()) return EvidenceRelationLevel.MISSING
        if (!snapshot.isSampleCourse && evidence.looksLikeSample()) return EvidenceRelationLevel.MISSING

        val expected = listOfNotNull(expectedLessonId, expectedSourceLessonId)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        if (expected.isNotEmpty() && evidence.sourceId.isNotBlank() && evidence.sourceId !in expected) {
            return EvidenceRelationLevel.MISSING
        }

        val currentSource = snapshot.lessonSourceId.ifBlank { snapshot.snapshotId }
        if (currentSource.isNotBlank() && evidence.sourceId.isNotBlank() && evidence.sourceId != currentSource) {
            return EvidenceRelationLevel.MISSING
        }

        val assetId = evidence.assetId?.trim().orEmpty()
        if (assetId.isNotBlank()) {
            val asset = snapshot.assets.firstOrNull { it.id == assetId }
                ?: return EvidenceRelationLevel.MISSING
            if (evidence.sourceType.isNotBlank() && asset.sourceType.isNotBlank() && evidence.sourceType != asset.sourceType) {
                return EvidenceRelationLevel.MISSING
            }
            if (mismatched(evidence.sourceLabel, asset.sourceLabel)) return EvidenceRelationLevel.MISSING
            if (mismatched(evidence.fileName, asset.fileName)) return EvidenceRelationLevel.MISSING
            if (mismatched(evidence.imageRef, asset.imageRef)) return EvidenceRelationLevel.MISSING
            if (mismatched(evidence.audioRef, asset.audioRef)) return EvidenceRelationLevel.MISSING
            if (asset.hasIncompleteMetadataFor(evidence.sourceType)) return EvidenceRelationLevel.WEAK
        }

        return EvidenceRelationLevel.STRONG
    }

    private fun EvidenceRecord.looksLikeSample(): Boolean {
        val haystack = listOf(id, sourceId, sourceType, sourceLabel, fileName)
            .joinToString(" ")
            .lowercase()
        return "sample" in haystack || "示例" in haystack
    }

    private fun mismatched(left: String, right: String): Boolean {
        val a = normalize(left)
        val b = normalize(right)
        return a.isNotBlank() && b.isNotBlank() && a != b
    }

    private fun normalize(value: String): String =
        value.trim().replace('\\', '/').lowercase()

    private fun AssetRecord.hasIncompleteMetadataFor(sourceType: String): Boolean {
        val type = sourceType.uppercase()
        val hasAnyRef = listOf(sourceLabel, fileName, imageRef, audioRef).any { it.isNotBlank() }
        return type in setOf("OCR_IMAGE", "DOCUMENT", "AUDIO_TRANSCRIPT", "RECORDING_ARTIFACT", "MANUAL_TRANSCRIPT") && !hasAnyRef
    }
}
