package com.classmate.core.evidence

/**
 * Ownership check for evidence shown from the current learning snapshot.
 *
 * The app does not have a global courseId on every legacy record, so this deliberately uses the
 * strongest local signals that are already persisted: current lesson/source id, current evidence
 * ids, asset ids, and source metadata. It is conservative: missing current evidence is MISSING,
 * stale/cross-course/sample evidence is WEAK, and only in-snapshot, source-matching evidence is STRONG.
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

        if (!snapshot.isSampleCourse && evidence.looksLikeSample()) return EvidenceRelationLevel.WEAK

        val expected = listOfNotNull(expectedLessonId, expectedSourceLessonId)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        if (expected.isNotEmpty() && evidence.sourceId.isNotBlank() && evidence.sourceId !in expected) {
            return EvidenceRelationLevel.WEAK
        }

        val currentSource = snapshot.lessonSourceId.ifBlank { snapshot.snapshotId }
        if (currentSource.isNotBlank() && evidence.sourceId.isNotBlank() && evidence.sourceId != currentSource) {
            return EvidenceRelationLevel.WEAK
        }

        val assetId = evidence.assetId?.trim().orEmpty()
        if (assetId.isNotBlank()) {
            val asset = snapshot.assets.firstOrNull { it.id == assetId }
                ?: return EvidenceRelationLevel.WEAK
            if (evidence.sourceType.isNotBlank() && asset.sourceType.isNotBlank() && evidence.sourceType != asset.sourceType) {
                return EvidenceRelationLevel.WEAK
            }
            if (mismatched(evidence.sourceLabel, asset.sourceLabel)) return EvidenceRelationLevel.WEAK
            if (mismatched(evidence.fileName, asset.fileName)) return EvidenceRelationLevel.WEAK
            if (mismatched(evidence.imageRef, asset.imageRef)) return EvidenceRelationLevel.WEAK
            if (mismatched(evidence.audioRef, asset.audioRef)) return EvidenceRelationLevel.WEAK
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
}
