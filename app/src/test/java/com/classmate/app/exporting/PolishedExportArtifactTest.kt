package com.classmate.app.exporting

import com.classmate.core.exporting.PolishedStudyPack
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolishedExportArtifactTest {

    private fun pack(markdown: String) = PolishedStudyPack(
        courseTitle = "高等数学",
        sourceLabel = "蓝心精修版",
        generatedAtLabel = "2026-06-29 10:00",
        markdown = markdown,
    )

    private fun docxText(bytes: ByteArray): String {
        val sb = StringBuilder()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") sb.append(zip.readBytes().toString(Charsets.UTF_8))
                entry = zip.nextEntry
            }
        }
        return sb.toString()
    }

    @Test
    fun pdfHtmlWordAllRenderTheSamePolishedContent() {
        val marker = "数项级数的比值判别法收敛条件"
        val p = pack("## 核心知识结构\n- $marker\n\n## 复习计划\n- 重做错题")

        val md = ExportCenter.artifactFromPolishedPack(p, ExportFileFormat.MARKDOWN).bytes.toString(Charsets.UTF_8)
        val html = ExportCenter.artifactFromPolishedPack(p, ExportFileFormat.HTML).bytes.toString(Charsets.UTF_8)
        val text = ExportCenter.artifactFromPolishedPack(p, ExportFileFormat.TEXT).bytes.toString(Charsets.UTF_8)
        val docx = docxText(ExportCenter.artifactFromPolishedPack(p, ExportFileFormat.DOCX).bytes)
        val pdfBytes = ExportCenter.artifactFromPolishedPack(p, ExportFileFormat.PDF).bytes

        // Every format derives from the SAME pack markdown — the marker appears in all text-bearing formats.
        assertTrue("markdown", md.contains(marker))
        assertTrue("html", html.contains(marker))
        assertTrue("text", text.contains(marker))
        assertTrue("docx", docx.contains(marker))
        assertTrue("pdf is a real non-empty document", pdfBytes.size > 100)
        // HTML renders structure, not a raw <pre> dump.
        assertTrue("html is structured", html.contains("<h2>") || html.contains("<li>"))
    }

    @Test
    fun polishedExportRedactsIdsAndCredentials() {
        val p = pack("## 知识点\n- 收敛判别（kp_abc123）Authorization: Bearer sk-secret-987 q_55")
        val md = ExportCenter.artifactFromPolishedPack(p, ExportFileFormat.MARKDOWN).bytes.toString(Charsets.UTF_8)
        assertFalse(md.contains("kp_abc123"))
        assertFalse(md.contains("q_55"))
        assertFalse(md.contains("Bearer"))
        assertFalse(md.contains("sk-secret-987"))
    }

    @Test
    fun fileNameCarriesPolishedMarker() {
        val artifact = ExportCenter.artifactFromPolishedPack(pack("- x"), ExportFileFormat.PDF)
        assertTrue(artifact.fileName.contains("polished"))
    }
}
