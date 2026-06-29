package com.classmate.core.evidence

import org.junit.Assert.assertEquals
import org.junit.Test

class EvidenceRelationTest {

    @Test
    fun sharedKeywordsAreStrong() {
        val excerpt = "法拉第电磁感应定律说明磁通量变化产生感应电动势。"
        val context = "电磁感应与感应电动势"
        assertEquals(EvidenceRelationLevel.STRONG, EvidenceRelation.assess(excerpt, context))
    }

    @Test
    fun zeroOverlapIsWeak() {
        val excerpt = "光合作用把二氧化碳和水转化为葡萄糖和氧气。"
        val context = "法拉第电磁感应定律与磁通量"
        assertEquals(EvidenceRelationLevel.WEAK, EvidenceRelation.assess(excerpt, context))
    }

    @Test
    fun asciiKeywordsAlsoMatch() {
        assertEquals(
            EvidenceRelationLevel.STRONG,
            EvidenceRelation.assess("TCP provides reliable ordered delivery", "Which protocol is reliable: TCP or UDP"),
        )
        assertEquals(
            EvidenceRelationLevel.WEAK,
            EvidenceRelation.assess("Photosynthesis converts sunlight into glucose", "Which transport protocol is reliable"),
        )
    }

    @Test
    fun insufficientSignalIsNotFlagged() {
        // A 1-token context can't be judged -> stay STRONG rather than cry wolf.
        assertEquals(EvidenceRelationLevel.STRONG, EvidenceRelation.assess("任何内容", "光"))
    }
}
