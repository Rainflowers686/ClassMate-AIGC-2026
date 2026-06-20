package com.classmate.app.l3

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputSuperhubTest {
    private val now = 1_700_000_000_000L

    @Test
    fun textMarkdownAndCsvAreReady() {
        val txt = InputSuperhub.parseFile("课堂文本".toByteArray(), "lesson.txt", now = now)
        val md = InputSuperhub.parseFile("# 标题\n正文".toByteArray(), "lesson.md", now = now)
        val csv = InputSuperhub.parseFile("stem,a,b,c,d,answer,explanation\n题,A,B,C,D,A,解析".toByteArray(), "bank.csv", now = now)

        assertEquals(InputFileKind.TXT, txt.kind)
        assertEquals(InputArtifactStatus.READY, txt.status)
        assertEquals(InputFileKind.MARKDOWN, md.kind)
        assertEquals(InputArtifactStatus.READY, md.status)
        assertEquals(InputFileKind.CSV, csv.kind)
        assertEquals(InputArtifactStatus.READY, csv.status)
    }

    @Test
    fun docxPptxAndXlsxUseBestEffortExtraction() {
        val docx = InputSuperhub.parseFile(
            zip("word/document.xml" to "<w:document><w:body><w:p><w:t>法拉第定律说明感应电动势与磁通量变化率相关</w:t></w:p></w:body></w:document>"),
            "lesson.docx",
            now = now,
        )
        val pptx = InputSuperhub.parseFile(
            zip("ppt/slides/slide1.xml" to "<p:sld><a:t>电磁感应课件</a:t></p:sld>"),
            "slides.pptx",
            now = now,
        )
        val xlsx = InputSuperhub.parseFile(
            zip(
                "xl/sharedStrings.xml" to "<sst><si><t>stem</t></si><si><t>a</t></si><si><t>b</t></si><si><t>c</t></si><si><t>d</t></si><si><t>answer</t></si><si><t>explanation</t></si></sst>",
                "xl/worksheets/sheet1.xml" to "<worksheet><sheetData><row><c t=\"s\"><v>0</v></c><c t=\"s\"><v>1</v></c><c t=\"s\"><v>2</v></c><c t=\"s\"><v>3</v></c><c t=\"s\"><v>4</v></c><c t=\"s\"><v>5</v></c><c t=\"s\"><v>6</v></c></row></sheetData></worksheet>",
            ),
            "bank.xlsx",
            now = now,
        )

        assertEquals(InputArtifactStatus.BEST_EFFORT, docx.status)
        assertTrue(docx.extractedText.contains("法拉第"))
        assertEquals(ExtractedTextRecommendedStatus.COMPLETE, docx.qualityReport!!.recommendedStatus)
        assertEquals(InputArtifactStatus.BEST_EFFORT, pptx.status)
        assertTrue(pptx.extractedText.contains("课件"))
        assertEquals(InputArtifactStatus.BEST_EFFORT, xlsx.status)
        assertTrue(xlsx.extractedText.contains("stem,a,b,c,d,answer,explanation"))
    }

    @Test
    fun officeExtractionQualityGuardFlagsEmptyPartialAndSuspiciousText() {
        val emptyDocx = InputSuperhub.parseFile(
            zip("word/document.xml" to "<w:document><w:body><w:p><w:t></w:t></w:p></w:body></w:document>"),
            "empty.docx",
            now = now,
        )
        val shortPptx = InputSuperhub.parseFile(
            zip("ppt/slides/slide1.xml" to "<p:sld><a:t>短</a:t></p:sld>"),
            "short.pptx",
            now = now,
        )
        val suspicious = InputSuperhub.qualityFor("������@@@@@%%%%%")

        assertEquals(InputArtifactStatus.EMPTY_FILE, emptyDocx.status)
        assertEquals(ExtractedTextRecommendedStatus.EMPTY_FILE, emptyDocx.qualityReport!!.recommendedStatus)
        assertEquals(InputArtifactStatus.BEST_EFFORT, shortPptx.status)
        assertEquals(ExtractedTextRecommendedStatus.PARTIAL, shortPptx.qualityReport!!.recommendedStatus)
        assertEquals(ExtractedTextRecommendedStatus.TEMPLATE_REQUIRED, suspicious.recommendedStatus)
        assertTrue(suspicious.suspiciousCharRatio > 0.0)
    }

    @Test
    fun pdfImageAndAudioReportHonestSeamStates() {
        val pdf = InputSuperhub.parseFile("%PDF".toByteArray(), "handout.pdf", now = now)
        val image = InputSuperhub.parseFile(byteArrayOf(1, 2), "board.png", "image/png", now)
        val audio = InputSuperhub.parseFile(byteArrayOf(1, 2), "lecture.m4a", "audio/mp4", now)

        assertEquals(InputArtifactStatus.PARSER_PENDING, pdf.status)
        assertEquals(ExtractedTextRecommendedStatus.PARSER_PENDING, pdf.qualityReport!!.recommendedStatus)
        assertEquals(InputArtifactStatus.OCR_READY_SEAM, image.status)
        assertEquals(InputArtifactStatus.ASR_NOT_CONFIGURED, audio.status)
    }

    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
