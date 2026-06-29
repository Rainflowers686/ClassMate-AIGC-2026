package com.classmate.core.evidence

import org.junit.Assert.assertEquals
import org.junit.Test

class EvidenceOwnershipTest {

    @Test
    fun evidenceMustExistInCurrentSnapshot() {
        val snapshot = snapshot(
            evidence = listOf(record(id = "ev_current", sourceId = "lesson_a")),
        )

        assertEquals(EvidenceRelationLevel.STRONG, EvidenceOwnership.assess(snapshot, "ev_current"))
        assertEquals(EvidenceRelationLevel.MISSING, EvidenceOwnership.assess(snapshot, "ev_other_course"))
    }

    @Test
    fun crossCourseSourceIdIsMissing() {
        val snapshot = snapshot(
            lessonSourceId = "lesson_a",
            evidence = listOf(record(id = "ev_cross", sourceId = "lesson_b")),
        )

        assertEquals(EvidenceRelationLevel.MISSING, EvidenceOwnership.assess(snapshot, "ev_cross"))
    }

    @Test
    fun realCourseRejectsSampleEvidenceAsMissing() {
        val snapshot = snapshot(
            isSampleCourse = false,
            evidence = listOf(record(id = "ev_sample", sourceId = "sample_lesson", sourceLabel = "示例课程")),
        )

        assertEquals(EvidenceRelationLevel.MISSING, EvidenceOwnership.assess(snapshot, "ev_sample"))
    }

    @Test
    fun missingAssetIsMissing() {
        val snapshot = snapshot(
            evidence = listOf(record(id = "ev_image", sourceId = "lesson_a", assetId = "asset_missing")),
            assets = emptyList(),
        )

        assertEquals(EvidenceRelationLevel.MISSING, EvidenceOwnership.assess(snapshot, "ev_image"))
    }

    @Test
    fun assetMetadataMismatchIsMissing() {
        val snapshot = snapshot(
            evidence = listOf(record(id = "ev_image", sourceId = "lesson_a", assetId = "asset_1", sourceType = "OCR_IMAGE", imageRef = "image-a.jpg")),
            assets = listOf(EvidenceOwnership.AssetRecord(id = "asset_1", sourceType = "OCR_IMAGE", imageRef = "image-b.jpg")),
        )

        assertEquals(EvidenceRelationLevel.MISSING, EvidenceOwnership.assess(snapshot, "ev_image"))
    }

    @Test
    fun emptyExcerptIsMissing() {
        val snapshot = snapshot(
            evidence = listOf(record(id = "ev_empty", sourceId = "lesson_a", excerpt = "")),
        )

        assertEquals(EvidenceRelationLevel.MISSING, EvidenceOwnership.assess(snapshot, "ev_empty"))
    }

    @Test
    fun incompleteAssetMetadataIsWeak() {
        val snapshot = snapshot(
            evidence = listOf(record(id = "ev_legacy", sourceId = "lesson_a", assetId = "asset_1", sourceType = "OCR_IMAGE")),
            assets = listOf(EvidenceOwnership.AssetRecord(id = "asset_1", sourceType = "OCR_IMAGE")),
        )

        assertEquals(EvidenceRelationLevel.WEAK, EvidenceOwnership.assess(snapshot, "ev_legacy"))
    }

    private fun snapshot(
        lessonSourceId: String = "lesson_a",
        isSampleCourse: Boolean = false,
        evidence: List<EvidenceOwnership.EvidenceRecord>,
        assets: List<EvidenceOwnership.AssetRecord> = emptyList(),
    ) = EvidenceOwnership.Snapshot(
        snapshotId = lessonSourceId,
        lessonSourceId = lessonSourceId,
        isSampleCourse = isSampleCourse,
        evidence = evidence,
        assets = assets,
    )

    private fun record(
        id: String,
        sourceId: String,
        sourceType: String = "TEXT",
        assetId: String? = null,
        sourceLabel: String = "",
        imageRef: String = "",
        excerpt: String = "class evidence excerpt",
    ) = EvidenceOwnership.EvidenceRecord(
        id = id,
        sourceId = sourceId,
        sourceType = sourceType,
        assetId = assetId,
        sourceLabel = sourceLabel,
        imageRef = imageRef,
        excerpt = excerpt,
    )
}
