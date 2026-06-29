package com.classmate.app.ui.screens.knowledge

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeTimelineP2TextTest {

    private val source: String = listOf(
        File("app/src/main/java/com/classmate/app/ui/screens/knowledge/KnowledgeTimelineScreen.kt"),
        File("src/main/java/com/classmate/app/ui/screens/knowledge/KnowledgeTimelineScreen.kt"),
    ).first { it.exists() }.readText()

    @Test
    fun askThisLessonEntryIsRemoved() {
        // Real-device #10/#17: the "问这节课" Q&A box is removed from the timeline; the page leads with
        // knowledge points + evidence + 微测.
        listOf("问这节课", "AskThisLessonCard", "updateAskLessonQuestion").forEach {
            assertFalse("ask-this-lesson UI should be removed: $it", source.contains(it))
        }
    }

    /**
     * F0-7: the Knowledge page is the entry behind the home "知识点" button and must be fully localized.
     * Its user-visible copy must come from appStrings(...), with no hardcoded English page labels left.
     */
    @Test
    fun knowledgePageReadsLocalizedCopyAndDropsHardcodedEnglish() {
        assertTrue("Knowledge page must read appStrings(...)", source.contains("appStrings("))
        listOf(
            "s.knowledgeTitle",
            "s.knowledgeStartQuiz(",
            "s.knowledgeOpenEvidence",
        ).forEach { assertTrue("Knowledge page should wire $it", source.contains(it)) }
        // The previously hardcoded English literals must be gone from the source.
        listOf(
            "\"Knowledge Timeline\"",
            "\"Start quiz\"",
            "\"Open evidence\"",
            "\"Original text\"",
            "\"Segment \$index\"",
        ).forEach { assertFalse("Knowledge page still hardcodes English: $it", source.contains(it)) }
    }
}
