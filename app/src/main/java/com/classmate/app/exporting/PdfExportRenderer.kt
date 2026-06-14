package com.classmate.app.exporting

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

object PdfExportRenderer {
    fun render(title: String, plainText: String): ByteArray =
        try {
            renderWithAndroidPdf(title, plainText)
        } catch (_: Throwable) {
            fallbackPdf(title, plainText)
        }

    private fun renderWithAndroidPdf(title: String, plainText: String): ByteArray {
        val document = PdfDocument()
        val width = 595
        val height = 842
        val margin = 42f
        val lineHeight = 18f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30)
            textSize = 12f
        }

        val lines = wrapLines("$title\n\n$plainText", 52)
        var pageNumber = 1
        var index = 0
        while (index < lines.size) {
            val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            var y = margin
            val canvas = page.canvas
            while (index < lines.size && y < height - margin) {
                val paint = if (index == 0) titlePaint else bodyPaint
                canvas.drawText(lines[index], margin, y, paint)
                y += lineHeight
                index += 1
            }
            document.finishPage(page)
            pageNumber += 1
        }

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    private fun fallbackPdf(title: String, plainText: String): ByteArray {
        val text = (title + "\n\n" + plainText)
            .replace(Regex("[^\\x20-\\x7E\\n]+"), " ")
            .lineSequence()
            .take(45)
            .joinToString("\\n")
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
        val stream = "BT /F1 12 Tf 50 780 Td ($text) Tj ET"
        val objects = listOf(
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n",
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n",
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n",
            "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n",
            "5 0 obj << /Length ${stream.toByteArray().size} >> stream\n$stream\nendstream endobj\n",
        )
        val header = "%PDF-1.4\n"
        val body = StringBuilder(header)
        val offsets = mutableListOf(0)
        objects.forEach {
            offsets += body.toString().toByteArray().size
            body.append(it)
        }
        val xrefOffset = body.toString().toByteArray().size
        body.append("xref\n0 ${objects.size + 1}\n")
        body.append("0000000000 65535 f \n")
        offsets.drop(1).forEach { body.append(String.format("%010d 00000 n \n", it)) }
        body.append("trailer << /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF")
        return body.toString().toByteArray(Charsets.UTF_8)
    }

    private fun wrapLines(value: String, maxChars: Int): List<String> =
        value.lineSequence().flatMap { line ->
            if (line.isBlank()) sequenceOf("")
            else line.chunked(maxChars).asSequence()
        }.toList().ifEmpty { listOf("ClassMate learning report") }
}

