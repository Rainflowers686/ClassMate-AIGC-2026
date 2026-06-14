package com.classmate.app.glossary

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptGlossaryHintTest {

    @Test
    fun matchingTermsAreBoundedAndContainNoSecretLikeTokens() {
        val subject = CourseGlossary.subjects.first()
        val text = CourseGlossary.termsFor(subject).joinToString(" ") { it.term }
        val hints = CourseGlossary.matchingTerms(subject, text, max = 20)

        assertTrue(hints.size <= 20)
        listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content").forEach {
            assertFalse(hints.any { h -> h.contains(it, ignoreCase = true) })
        }
    }

    @Test
    fun onlyTermsPresentInTextAreSuggested() {
        val hints = CourseGlossary.matchingTerms("大学物理", "今天复习磁通量与楞次定律。")
        assertTrue(hints.contains("磁通量"))
        assertTrue(hints.contains("楞次定律"))
        assertFalse(hints.contains("互感"))
    }

    @Test
    fun blankTextYieldsNoHints() {
        assertTrue(CourseGlossary.matchingTerms("大学物理", "   ").isEmpty())
    }
}
