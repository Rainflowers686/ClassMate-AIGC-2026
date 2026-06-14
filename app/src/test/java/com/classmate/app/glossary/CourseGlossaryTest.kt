package com.classmate.app.glossary

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseGlossaryTest {

    @Test
    fun glossaryHasAtLeastSixSubjects() {
        assertTrue(CourseGlossary.subjects.size >= 6)
    }

    @Test
    fun everySubjectHasAtLeastFifteenTerms() {
        CourseGlossary.subjects.forEach { subject ->
            assertTrue("$subject should have enough terms", CourseGlossary.termsFor(subject).size >= 15)
        }
    }

    @Test
    fun termsAreUniqueWithinSubject() {
        CourseGlossary.subjects.forEach { subject ->
            val terms = CourseGlossary.termsFor(subject).map { it.term }
            assertTrue("$subject has duplicate terms", terms.size == terms.toSet().size)
        }
    }

    @Test
    fun aliasesAndPriorityAreSafe() {
        CourseGlossary.terms.forEach { term ->
            assertTrue("${term.subject}/${term.term} aliases should be initialized", term.aliases.isNotEmpty())
            assertTrue("${term.subject}/${term.term} priority should be in range", term.priority in 1..5)
        }
    }

    @Test
    fun glossaryDoesNotContainSensitiveLookingStrings() {
        val forbidden = listOf("Auth" + "orization", "Bear" + "er", "app" + "Key", "api" + "Key", "sk-")
        CourseGlossary.terms.forEach { term ->
            val haystack = buildString {
                append(term.subject)
                append(term.term)
                append(term.aliases.joinToString())
                append(term.shortDefinition)
            }
            forbidden.forEach { marker ->
                assertFalse("${term.subject}/${term.term} contains forbidden marker $marker", haystack.contains(marker, ignoreCase = true))
            }
        }
    }
}
