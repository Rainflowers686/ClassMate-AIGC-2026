package com.classmate.core.exporting

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal OpenXML renderer for a real .docx package. It intentionally avoids external dependencies:
 * the output is a normal ZIP with the required WordprocessingML parts, not HTML renamed to .docx.
 */
object StudyReportDocxRenderer {
    fun render(report: StudyReport): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.writeEntry("[Content_Types].xml", contentTypesXml())
            zip.writeEntry("_rels/.rels", relsXml())
            zip.writeEntry("docProps/core.xml", corePropsXml(report))
            zip.writeEntry("docProps/app.xml", appPropsXml())
            zip.writeEntry("word/_rels/document.xml.rels", documentRelsXml())
            zip.writeEntry("word/styles.xml", stylesXml())
            zip.writeEntry("word/document.xml", documentXml(report))
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypesXml(): String = xmlHeader() + """
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
          <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
          <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
          <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
        </Types>
    """.trimIndent()

    private fun relsXml(): String = xmlHeader() + """
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
        </Relationships>
    """.trimIndent()

    private fun documentRelsXml(): String = xmlHeader() + """
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun appPropsXml(): String = xmlHeader() + """
        <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
          <Application>ClassMate</Application>
          <DocSecurity>0</DocSecurity>
          <ScaleCrop>false</ScaleCrop>
          <Company>ClassMate</Company>
          <LinksUpToDate>false</LinksUpToDate>
          <SharedDoc>false</SharedDoc>
          <HyperlinksChanged>false</HyperlinksChanged>
          <AppVersion>1.0</AppVersion>
        </Properties>
    """.trimIndent()

    private fun corePropsXml(report: StudyReport): String = xmlHeader() + """
        <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <dc:title>${x(report.courseTitle)} - ClassMate 学习报告</dc:title>
          <dc:creator>ClassMate</dc:creator>
          <cp:lastModifiedBy>ClassMate</cp:lastModifiedBy>
          <dc:description>ClassMate DOCX study report generated from user-confirmed lesson materials.</dc:description>
        </cp:coreProperties>
    """.trimIndent()

    private fun stylesXml(): String = xmlHeader() + """
        <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
          <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
            <w:name w:val="Normal"/>
            <w:rPr><w:rFonts w:ascii="Microsoft YaHei" w:hAnsi="Microsoft YaHei" w:eastAsia="Microsoft YaHei"/><w:sz w:val="22"/></w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Title">
            <w:name w:val="Title"/>
            <w:basedOn w:val="Normal"/>
            <w:pPr><w:spacing w:after="240"/></w:pPr>
            <w:rPr><w:b/><w:sz w:val="36"/></w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Heading1">
            <w:name w:val="heading 1"/>
            <w:basedOn w:val="Normal"/>
            <w:pPr><w:spacing w:before="260" w:after="120"/></w:pPr>
            <w:rPr><w:b/><w:sz w:val="28"/></w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Heading2">
            <w:name w:val="heading 2"/>
            <w:basedOn w:val="Normal"/>
            <w:pPr><w:spacing w:before="180" w:after="80"/></w:pPr>
            <w:rPr><w:b/><w:sz w:val="24"/></w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Quote">
            <w:name w:val="Quote"/>
            <w:basedOn w:val="Normal"/>
            <w:pPr><w:ind w:left="420"/><w:spacing w:before="80" w:after="80"/></w:pPr>
            <w:rPr><w:i/><w:color w:val="44546A"/></w:rPr>
          </w:style>
        </w:styles>
    """.trimIndent()

    private fun documentXml(report: StudyReport): String {
        val body = buildString {
            p("ClassMate 学习报告", "Title")
            p("课程：${report.courseTitle}")
            p("生成时间：${report.generatedAtLabel}")
            p("AI 来源说明：${report.providerLabel}")
            report.sourceSummaryLine?.let { p(it) }
            report.transcriptSummaryLine?.let { p(it) }

            h1("一、课程概览")
            bullets(report.overview.ifEmpty { listOf("暂无课程概览。") })
            if (report.reviewTopics.isNotEmpty()) p("建议复习主题：${report.reviewTopics.joinToString("、")}")

            report.localSuggestion?.takeIf { it.isNotBlank() }?.let {
                h1("二、学习建议")
                p(it)
            }

            h1("三、知识地图与关键知识点")
            if (report.knowledgePoints.isEmpty()) p("暂无知识点。")
            report.knowledgePoints.forEach { kp ->
                h2("${kp.index}. ${kp.title}")
                p("解释：${kp.summary}")
                p("重要性 / 难度：${kp.importanceZh} / ${kp.difficultyZh}")
                kp.evidence.forEach { ev -> quote("Evidence：${ev.quote}（来源：${ev.sourceLabel}）") }
                p("学习提示：${kp.studyTip}")
            }

            h1("四、微测与 Practice 结果")
            if (report.quizzes.isEmpty()) p("暂无微测题。")
            report.quizzes.forEach { q ->
                h2("第 ${q.index} 题：${q.typeZh}")
                p(q.stem)
                q.options.forEach { opt -> p("${opt.label}. ${opt.text}${if (opt.correct) "（正确）" else ""}") }
                p("正确答案：${q.correctLabels.joinToString("、")}")
                if (q.explanation.isNotBlank()) p("解析：${q.explanation}")
                q.evidenceQuotes.forEach { quote("Evidence：$it") }
            }
            report.practice?.let { practice ->
                h2("专项练习结果")
                p("模式：${practice.modeZh}")
                p("结果：共 ${practice.itemCount} 题，正确 ${practice.correctCount}，错误 ${practice.wrongCount}，已掌握 ${practice.masteredCount}，需要多练 ${practice.needMorePracticeCount}")
                if (practice.practicedTopics.isNotEmpty()) p("练习知识点：${practice.practicedTopics.joinToString("、")}")
                if (practice.needMoreTopics.isNotEmpty()) p("需要多练：${practice.needMoreTopics.joinToString("、")}")
                p("下一步：${practice.nextSuggestion}")
            }

            h1("五、薄弱点与复习计划")
            if (report.weaknesses.isEmpty()) p("暂无薄弱点。")
            report.weaknesses.forEach { item ->
                p("${item.title}（${item.courseTitle}）：错 ${item.wrongCount} / 对 ${item.correctCount}；原因：${item.reason}；建议：${item.recommendedAction}")
            }
            p("预计复习时间：${report.review.estimatedMinutes} 分钟")
            sectionList("今日待复习", report.review.dueToday)
            sectionList("近期复习", report.review.upcoming)
            sectionList("已掌握", report.review.mastered)
            sectionList("需要复核", report.review.needsRecheck)

            h1("六、问这节课")
            if (report.askItems.isEmpty()) p("暂无 Ask This Lesson 记录。")
            report.askItems.forEachIndexed { index, ask ->
                h2("问题 ${index + 1}：${ask.topic}")
                p("状态：${ask.statusZh}")
                p("摘要：${ask.answerSummary}")
                ask.evidenceQuotes.forEach { quote("Evidence：$it") }
            }

            if (report.translationNotes.isNotEmpty()) {
                h1("七、双语学习注记")
                report.translationNotes.forEach { note ->
                    p("${note.targetTitle}：${note.sourceLanguage} -> ${note.targetLanguage}：${note.translatedText}")
                }
            }

            report.courseEssenceScript?.let { script ->
                h1("八、课程精华音频脚本")
                script.toPlainText().lineSequence().filter { it.isNotBlank() }.forEach { p(it) }
            }

            h1("九、用户确认与隐私说明")
            p("本报告基于用户确认后的课堂资料、证据链、练习反馈和复习状态生成。")
            p("报告不包含密钥、原始请求、模型原始响应或推理过程。")
            p("AI 生成内容仅供学习参考，请结合课程要求核对。")
            report.safetySummary?.let { p("文本安全检查：${it.status} / ${it.source} / ${it.note}") }
        }
        return xmlHeader() + """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                $body
                <w:sectPr>
                  <w:pgSz w:w="11906" w:h="16838"/>
                  <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/>
                </w:sectPr>
              </w:body>
            </w:document>
        """.trimIndent()
    }

    private fun StringBuilder.h1(text: String) = p(text, "Heading1")
    private fun StringBuilder.h2(text: String) = p(text, "Heading2")
    private fun StringBuilder.quote(text: String) = p(text, "Quote")

    private fun StringBuilder.bullets(items: List<String>) {
        items.forEach { p("• $it") }
    }

    private fun StringBuilder.sectionList(title: String, items: List<String>) {
        h2(title)
        if (items.isEmpty()) p("无") else bullets(items)
    }

    private fun StringBuilder.p(text: String, style: String? = null) {
        append("<w:p>")
        if (style != null) append("<w:pPr><w:pStyle w:val=\"").append(style).append("\"/></w:pPr>")
        append("<w:r><w:t xml:space=\"preserve\">").append(x(text)).append("</w:t></w:r>")
        append("</w:p>\n")
    }

    private fun xmlHeader(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" + "\n"

    private fun x(value: String): String =
        SafeExportText.redact(value)
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
