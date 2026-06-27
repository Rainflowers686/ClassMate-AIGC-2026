package com.classmate.app.state

import com.classmate.app.data.L3PersistenceRepository
import com.classmate.app.l3.Evidence
import com.classmate.app.l3.EvidenceAsset
import com.classmate.app.l3.EvidenceAssetType
import com.classmate.app.l3.L3PipelineSnapshot
import com.classmate.app.l3.L3SourceType
import com.classmate.app.l3.LessonSource
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.evidence.EvidenceRelationLevel
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "查看证据" everywhere (review, course, import, diagnosis) is gated on [AppViewModel.hasRetraceableEvidence].
 * This locks the detection that keeps mis-bound / stale / cross-course / empty evidence from masquerading
 * as a real, traceable source: an id is only retraceable when it actually resolves to evidence present in
 * the CURRENT pipeline with a non-empty excerpt — merely existing somewhere else is not enough.
 */
class EvidenceRetraceabilityTest {

    private fun vm() = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-ev").resolve("config.local.json").toFile()),
    )

    @Test
    fun idsNotBackedByPresentEvidenceAreRejected() {
        val viewModel = vm()
        assertTrue("a fresh course has no bound evidence", viewModel.ui.l3Pipeline.evidence.isEmpty())
        // Null / blank / and any id that does not resolve in this pipeline (stale, cross-course, demo) -> false.
        assertFalse(viewModel.hasRetraceableEvidence(null))
        assertFalse(viewModel.hasRetraceableEvidence(""))
        assertFalse("an id from another course/snapshot is not retraceable here", viewModel.hasRetraceableEvidence("ev_other_course"))
        assertFalse(viewModel.hasRetraceableEvidence("kp_sample_demo"))
    }

    @Test
    fun relationLevelIsMissingWhenThereIsNoExcerpt() {
        val viewModel = vm()
        assertEquals(EvidenceRelationLevel.MISSING, viewModel.evidenceRelationLevel(null, "电磁感应"))
        assertEquals(EvidenceRelationLevel.MISSING, viewModel.evidenceRelationLevel("ev_unknown", "电磁感应"))
    }

    @Test
    fun sourceMismatchedEvidenceIsWeakNotStrong() {
        val file = Files.createTempDirectory("cm-ev-owner").resolve("classmate_l3_store.json").toFile()
        L3PersistenceRepository(file).saveSnapshot(
            L3PipelineSnapshot(
                lessonSource = LessonSource("lesson_current", "Physics", L3SourceType.TEXT, 1L, "电磁感应", "READY"),
                evidence = listOf(
                    Evidence(
                        id = "ev_stale",
                        sourceId = "lesson_other",
                        sourceType = L3SourceType.TEXT,
                        text = "电磁感应是磁通量变化时产生感应电流。",
                    ),
                ),
            ),
        )
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-ev-config").resolve("config.local.json").toFile()),
            l3PersistenceRepository = L3PersistenceRepository(file),
        )

        assertEquals(EvidenceRelationLevel.WEAK, viewModel.evidenceRelationLevel("ev_stale", "电磁感应"))
    }

    @Test
    fun missingEvidenceAssetIsWeakNotStrong() {
        val file = Files.createTempDirectory("cm-ev-asset").resolve("classmate_l3_store.json").toFile()
        L3PersistenceRepository(file).saveSnapshot(
            L3PipelineSnapshot(
                lessonSource = LessonSource("lesson_current", "Photo", L3SourceType.OCR_IMAGE, 1L, "OCR text", "READY"),
                evidence = listOf(
                    Evidence(
                        id = "ev_photo",
                        sourceId = "lesson_current",
                        sourceType = L3SourceType.OCR_IMAGE,
                        text = "OCR text",
                        assetId = "asset_missing",
                    ),
                ),
                evidenceAssets = listOf(
                    EvidenceAsset(
                        id = "asset_other",
                        type = EvidenceAssetType.OCR_IMAGE,
                        sourceType = L3SourceType.OCR_IMAGE,
                        text = "OCR text",
                    ),
                ),
            ),
        )
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-ev-config2").resolve("config.local.json").toFile()),
            l3PersistenceRepository = L3PersistenceRepository(file),
        )

        assertEquals(EvidenceRelationLevel.WEAK, viewModel.evidenceRelationLevel("ev_photo", "OCR text"))
    }
}
