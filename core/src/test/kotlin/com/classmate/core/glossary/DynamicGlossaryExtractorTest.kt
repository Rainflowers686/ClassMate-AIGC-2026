package com.classmate.core.glossary

import com.classmate.core.analysis.CourseDomain
import com.classmate.core.analysis.CourseDomainDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicGlossaryExtractorTest {

    @Test
    fun extractsMechanicalTermsFromContentForANonBuiltInSubject() {
        val terms = DynamicGlossaryExtractor.extract(
            domain = "机械",
            sources = listOf(GlossarySource("ev1", "齿轮、轴承、连杆是机械设计的核心零件，扭矩通过齿轮传动。")),
            seedTerms = CourseDomainDetector.seedTermsFor(CourseDomain.MECHANICAL),
        )
        val words = terms.map { it.term }
        assertTrue(words.contains("齿轮"))
        assertTrue(words.contains("轴承"))
        assertTrue(terms.all { it.domain == "机械" })
    }

    @Test
    fun extractsDifferentTermsForPythonContent() {
        val terms = DynamicGlossaryExtractor.extract(
            domain = "计算机",
            sources = listOf(GlossarySource("ev1", "Python 的列表、字典、循环和异常处理。HashMap 也很常见。")),
            seedTerms = CourseDomainDetector.seedTermsFor(CourseDomain.COMPUTER_SCIENCE),
        )
        val words = terms.map { it.term.lowercase() }
        assertTrue(words.contains("python"))
        // No mechanical term leaks in from a different domain's content.
        assertFalse(words.contains("齿轮"))
    }

    @Test
    fun userPinnedTermsAlwaysWinAndComeFirst() {
        val pinned = listOf(
            DynamicGlossaryTerm(term = "自定义术语", normalizedTerm = "自定义术语", domain = "机械", confidence = 0.1, isUserPinned = true),
        )
        val terms = DynamicGlossaryExtractor.extract(
            domain = "机械",
            sources = listOf(GlossarySource("ev1", "齿轮、轴承、连杆。")),
            seedTerms = CourseDomainDetector.seedTermsFor(CourseDomain.MECHANICAL),
            pinned = pinned,
        )
        assertEquals("自定义术语", terms.first().term)
        assertTrue(terms.first().isUserPinned)
        assertEquals(1.0, terms.first().confidence, 0.0001)
    }

    @Test
    fun minesNovelAsciiTechTokens() {
        val terms = DynamicGlossaryExtractor.extract(
            domain = "通用课堂",
            sources = listOf(GlossarySource("ev1", "We compared QuickSort with MergeSort and used a HashMap.")),
        )
        val words = terms.map { it.term }
        assertTrue(words.any { it.equals("QuickSort", ignoreCase = true) })
        assertTrue(words.any { it.equals("HashMap", ignoreCase = true) })
    }

    @Test
    fun examplesAreShortAndNeverDumpWholeSource() {
        val long = "齿轮" + "这是一段很长很长很长很长很长很长很长很长很长很长很长很长的课堂记录文本".repeat(5)
        val terms = DynamicGlossaryExtractor.extract(
            domain = "机械",
            sources = listOf(GlossarySource("ev1", long)),
            seedTerms = listOf("齿轮"),
        )
        val gear = terms.first { it.term == "齿轮" }
        assertTrue(gear.examples.isNotEmpty())
        assertTrue("example must be capped", gear.examples.all { it.length <= 48 })
        assertEquals("ev1", gear.sourceEvidenceId)
    }

    @Test
    fun resultIsBoundedByMax() {
        // 50 distinct ASCII tech tokens (digits keep them distinct) → must be capped at max.
        val text = (1..50).joinToString(" ") { "Token$it" }
        val terms = DynamicGlossaryExtractor.extract(
            domain = "通用课堂",
            sources = listOf(GlossarySource("ev1", text)),
            max = 10,
        )
        assertTrue(terms.size <= 10)
    }
}
