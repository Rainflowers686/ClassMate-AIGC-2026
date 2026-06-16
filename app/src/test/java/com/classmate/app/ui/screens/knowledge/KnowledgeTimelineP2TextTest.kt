package com.classmate.app.ui.screens.knowledge

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeTimelineP2TextTest {
    @Test
    fun askCardShowsFollowUpsAndAddToReviewAction() {
        val source = listOf(
            File("app/src/main/java/com/classmate/app/ui/screens/knowledge/KnowledgeTimelineScreen.kt"),
            File("src/main/java/com/classmate/app/ui/screens/knowledge/KnowledgeTimelineScreen.kt"),
        ).first { it.exists() }.readText()

        listOf(
            "建议追问",
            "加入复习",
            "suggestedFollowUps",
            "addAskAnswerToReview",
        ).forEach { assertTrue("missing Ask learning-loop copy: $it", source.contains(it)) }
    }
}
