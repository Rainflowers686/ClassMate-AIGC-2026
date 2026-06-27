package com.classmate.app.state

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
}
