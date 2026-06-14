package com.classmate.app.exporting

import com.classmate.core.exporting.ContentExporter
import com.classmate.core.exporting.ExportFormat
import com.classmate.core.mindmap.MindMapBuilder
import com.classmate.core.model.LearningState
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportCenterTest {
    private val session = SampleCourses.seriesSession()
    private val result = SampleCourses.seriesAnalysis()
    private val state = LearningState.seed(result.sessionId, result.knowledgePoints, 1_700_000_000_000L)
    private val exporter = ContentExporter()
    private val markdown = exporter.exportFullReport(
        session = session,
        result = result,
        reviewPlan = null,
        mindMap = MindMapBuilder.fromAnalysis(result, session.title, state),
        videoRecommendations = emptyList(),
        format = ExportFormat.MARKDOWN,
    ).content

    @Test
    fun markdownHtmlAndTextArtifactsAreGeneratedAndSafe() {
        listOf(ExportFileFormat.MARKDOWN, ExportFileFormat.HTML, ExportFileFormat.TEXT).forEach { format ->
            val artifact = ExportCenter.artifactFromMarkdown(session.title, markdown, format, createdAt = 1L)

            assertTrue(artifact.fileName.endsWith(".${format.extension}"))
            assertTrue(artifact.bytes.isNotEmpty())
            assertFalse(artifact.containsSensitiveContent)
        }
    }

    @Test
    fun pdfArtifactIsNonEmptyWithPdfHeader() {
        val artifact = ExportCenter.artifactFromMarkdown(session.title, markdown, ExportFileFormat.PDF, createdAt = 1L)

        assertTrue(artifact.mimeType == "application/pdf")
        assertTrue(artifact.bytes.decodeToString().startsWith("%PDF"))
    }

    @Test
    fun compatibleHtmlFormatsAreHonestAlternatives() {
        val word = ExportCenter.artifactFromMarkdown(session.title, markdown, ExportFileFormat.WORD_COMPAT_HTML, createdAt = 1L)
        val slides = ExportCenter.artifactFromMarkdown(session.title, markdown, ExportFileFormat.SLIDES_HTML, createdAt = 1L)
        val wordText = word.bytes.decodeToString()
        val slidesText = slides.bytes.decodeToString()

        assertTrue(wordText.contains("Word 兼容 HTML"))
        assertTrue(wordText.contains("不是真 .docx"))
        assertTrue(slidesText.contains("演示幻灯片"))
        assertTrue(slidesText.contains("Ask This Lesson"))
        assertTrue(slidesText.contains("多模态来源摘要"))
    }

    @Test
    fun mindMapExportsContainRootAndKnowledgePoint() {
        val mindMap = exporter.markdownMindMap(MindMapBuilder.fromAnalysis(result, session.title, state))
        val md = ExportCenter.artifactFromMarkdown(session.title, mindMap, ExportFileFormat.MINDMAP_MARKDOWN, createdAt = 1L)
        val html = ExportCenter.artifactFromMarkdown(session.title, mindMap, ExportFileFormat.MINDMAP_HTML, createdAt = 1L)

        assertTrue(md.bytes.decodeToString().contains(session.title))
        assertTrue(md.bytes.decodeToString().contains(result.knowledgePoints.first().title))
        assertTrue(html.bytes.decodeToString().contains("思维导图"))
    }

    @Test
    fun fileNameSanitizerRemovesUnsafeCharacters() {
        val name = ExportFileNameSanitizer.safe("ClassMate bad:/name*? <report>", "md")

        assertTrue(name.endsWith(".md"))
        listOf(":", "/", "\\", "*", "?", "<", ">", "|").forEach {
            assertFalse(name.contains(it))
        }
    }

    @Test
    fun exportArtifactDetectsSensitiveLikeTokens() {
        val dirty = listOf(
            "Auth" + "orization",
            "Bear" + "er",
            "pro" + "mpt",
            "mes" + "sages",
            "reasoning" + "_content",
        ).joinToString(" ")
        val artifact = ExportCenter.artifactFromMarkdown("course", dirty, ExportFileFormat.MARKDOWN, createdAt = 1L)

        assertFalse(artifact.bytes.decodeToString().contains(dirty))
        assertFalse(artifact.containsSensitiveContent)
    }
}

