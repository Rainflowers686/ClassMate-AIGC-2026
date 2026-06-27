package com.classmate.core.evidence

import org.junit.Assert.assertEquals
import org.junit.Test

class EvidenceRelationConservativeTest {

    @Test
    fun chinesePhysicsEvidenceKeepsSharedConceptStrong() {
        assertEquals(
            EvidenceRelationLevel.STRONG,
            EvidenceRelation.assess(
                excerpt = "电磁感应是磁通量变化时在闭合回路中产生感应电流的现象。",
                context = "电磁感应与磁通量变化有什么关系？",
            ),
        )
    }

    @Test
    fun englishNetworkingEvidenceKeepsSharedConceptStrong() {
        assertEquals(
            EvidenceRelationLevel.STRONG,
            EvidenceRelation.assess(
                excerpt = "TCP retransmits lost packets and provides reliable ordered transport.",
                context = "Why does TCP support reliable transport for HTTP?",
            ),
        )
    }

    @Test
    fun mathEvidenceKeepsSharedConceptStrong() {
        assertEquals(
            EvidenceRelationLevel.STRONG,
            EvidenceRelation.assess(
                excerpt = "导数表示函数在某一点附近的瞬时变化率。",
                context = "导数和变化率如何对应？",
            ),
        )
    }
}
