package com.classmate.core.exporting

/**
 * Renders a [StudyReport] into each printable format. ALL formats come from this one source, so they
 * never drift. Output is a structured study handout (封面 + nine sections), never a UI/JSON dump.
 */
object StudyReportRenderer {

    private const val PRIVACY_LINE_1 = "本报告由 ClassMate 根据用户提供的课堂资料生成。"
    private const val PRIVACY_LINE_2 = "不包含密钥、原始请求、模型原始响应或推理过程内容。"
    private const val PRIVACY_LINE_3 = "AI 生成内容仅供参考，请结合课程要求核对。"

    // ---- Markdown ----------------------------------------------------------------------------

    fun renderMarkdown(report: StudyReport): String = buildString {
        appendLine("# ClassMate 学习报告")
        appendLine()
        appendLine("- 课程：${report.courseTitle}")
        appendLine("- 生成时间：${report.generatedAtLabel}")
        appendLine("- 模型路径：${report.providerLabel}")
        if (report.sourceTypeLabels.isNotEmpty()) appendLine("- 资料来源：${report.sourceTypeLabels.joinToString("、")}")
        appendLine()

        appendLine("## 一、课程概要")
        appendLine()
        if (report.overview.isEmpty()) appendLine("（暂无概要）") else report.overview.forEach { appendLine("- $it") }
        if (report.reviewTopics.isNotEmpty()) {
            appendLine()
            appendLine("适合复习的主题：${report.reviewTopics.joinToString("、")}")
        }
        appendLine()

        report.localSuggestion?.takeIf { it.isNotBlank() }?.let {
            appendLine("## 学习建议（端侧本地智能）")
            appendLine()
            appendLine(it)
            appendLine()
        }

        appendLine("## 二、核心知识点")
        appendLine()
        if (report.knowledgePoints.isEmpty()) appendLine("（暂无知识点）")
        report.knowledgePoints.forEach { kp ->
            appendLine("### ${kp.index}. ${kp.title}")
            appendLine("- 解释：${kp.summary}")
            appendLine("- 重要性 / 难度：${kp.importanceZh} / ${kp.difficultyZh}")
            kp.evidence.firstOrNull()?.let { appendLine("- 原文证据：「${it.quote}」（${it.sourceLabel}）") }
            appendLine("- 学习提示：${kp.studyTip}")
            appendLine()
        }

        appendLine("## 三、证据链")
        appendLine()
        if (report.knowledgePoints.none { it.evidence.isNotEmpty() }) appendLine("（暂无证据）")
        report.knowledgePoints.filter { it.evidence.isNotEmpty() }.forEach { kp ->
            appendLine("- ${kp.title}")
            kp.evidence.forEach { ev -> appendLine("  - 「${ev.quote}」（来源：${ev.sourceLabel}）") }
        }
        appendLine()

        appendLine("## 四、微测题")
        appendLine()
        if (report.quizzes.isEmpty()) appendLine("（暂无微测题）")
        report.quizzes.forEach { q ->
            appendLine("### 第 ${q.index} 题（${q.typeZh}）")
            appendLine(q.stem)
            q.options.forEach { opt -> appendLine("- ${opt.label}. ${opt.text}${if (opt.correct) "  ✅" else ""}") }
            appendLine("- 正确答案：${q.correctLabels.joinToString("、")}")
            if (q.explanation.isNotBlank()) appendLine("- 解析：${q.explanation}")
            if (q.relatedKnowledgePoints.isNotEmpty()) appendLine("- 关联知识点：${q.relatedKnowledgePoints.joinToString("、")}")
            q.evidenceQuotes.firstOrNull()?.let { appendLine("- 关联证据：「$it」") }
            appendLine()
        }

        appendLine("## 五、需要多练清单")
        appendLine()
        if (report.needPractice.isEmpty()) appendLine("暂无需要多练的知识点。继续保持！")
        report.needPractice.forEach { item ->
            appendLine("### ${item.title}")
            appendLine("- 为什么需要多练：${item.reason}")
            appendLine("- 建议练习方向：${item.direction}")
            appendLine("- 推荐搜索关键词：${item.keywords}")
            item.searchLinks.forEach { appendLine("  - $it") }
            appendLine()
        }

        appendLine("## 六、复习计划")
        appendLine()
        appendLine("- 预计用时：${report.review.estimatedMinutes} 分钟")
        appendBucket("今日待复习", report.review.dueToday)
        appendBucket("近期复习", report.review.upcoming)
        appendBucket("已掌握", report.review.mastered)
        appendBucket("需要复核", report.review.needsRecheck)
        appendLine()

        appendLine("## 七、问这节课")
        appendLine()
        if (report.askItems.isEmpty()) appendLine("本次会话未提问。")
        report.askItems.forEachIndexed { i, ask ->
            appendLine("### 提问 ${i + 1}：${ask.topic}")
            appendLine("- 回答状态：${ask.statusZh}")
            appendLine("- 答案摘要：${ask.answerSummary}")
            ask.evidenceQuotes.firstOrNull()?.let { appendLine("- 引用证据：「$it」") }
            appendLine()
        }

        appendLine("## 八、资料来源摘要")
        appendLine()
        appendLine(report.sourceSummaryLine ?: "资料来源：课堂文本")
        report.transcriptSummaryLine?.let { appendLine(it) }
        appendLine()

        appendLine("## 九、隐私与说明")
        appendLine()
        appendLine("- $PRIVACY_LINE_1")
        appendLine("- $PRIVACY_LINE_2")
        appendLine("- $PRIVACY_LINE_3")

        report.practice?.let { p ->
            appendLine()
            appendLine("## 十、专项练习与错题本")
            appendLine()
            appendLine("- 本轮练习模式：${p.modeZh}")
            appendLine("- 结果：共 ${p.itemCount} 题 · 正确 ${p.correctCount} · 错误 ${p.wrongCount} · 已掌握 ${p.masteredCount} · 需要多练 ${p.needMorePracticeCount}")
            if (p.practicedTopics.isNotEmpty()) appendLine("- 练习知识点：${p.practicedTopics.joinToString("、")}")
            if (p.needMoreTopics.isNotEmpty()) appendLine("- 需要多练项：${p.needMoreTopics.joinToString("、")}")
            appendLine("- 下一步建议：${p.nextSuggestion}")
            if (p.searchQueries.isNotEmpty()) {
                appendLine("- 推荐搜索词：")
                p.searchQueries.forEach { appendLine("  - $it") }
            }
            if (p.recentRecords.isNotEmpty()) {
                appendLine("- 最近练习记录：")
                p.recentRecords.forEach { appendLine("  - $it") }
            }
        }
    }.trim()

    private fun StringBuilder.appendBucket(label: String, items: List<String>) {
        if (items.isEmpty()) {
            appendLine("- $label：无")
        } else {
            appendLine("- $label：")
            items.forEach { appendLine("  - $it") }
        }
    }

    // ---- Plain text --------------------------------------------------------------------------

    fun renderPlainText(report: StudyReport): String =
        renderMarkdown(report)
            .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
            .replace("**", "")
            .replace("`", "")

    // ---- HTML (A4 print friendly) ------------------------------------------------------------

    fun renderHtml(report: StudyReport, note: String = "浏览器可打开并打印（建议 A4）"): String {
        val body = StringBuilder()
        body.append("<header><h1>ClassMate 学习报告</h1>")
        body.append("<p class=\"meta\">课程：${esc(report.courseTitle)}　·　生成时间：${esc(report.generatedAtLabel)}　·　模型路径：${esc(report.providerLabel)}</p>")
        if (report.sourceTypeLabels.isNotEmpty()) body.append("<p class=\"meta\">资料来源：${esc(report.sourceTypeLabels.joinToString("、"))}</p>")
        body.append("<p class=\"note\">${esc(note)}</p></header>")

        body.section("一、课程概要") {
            ul(report.overview)
            if (report.reviewTopics.isNotEmpty()) append("<p>适合复习的主题：${esc(report.reviewTopics.joinToString("、"))}</p>")
        }
        report.localSuggestion?.takeIf { it.isNotBlank() }?.let {
            body.section("学习建议（端侧本地智能）") { append("<p>${esc(it)}</p>") }
        }
        body.section("二、核心知识点") {
            report.knowledgePoints.forEach { kp ->
                append("<h3>${kp.index}. ${esc(kp.title)}</h3>")
                append("<ul>")
                append("<li>解释：${esc(kp.summary)}</li>")
                append("<li>重要性 / 难度：${esc(kp.importanceZh)} / ${esc(kp.difficultyZh)}</li>")
                kp.evidence.firstOrNull()?.let { append("<li>原文证据：「${esc(it.quote)}」（${esc(it.sourceLabel)}）</li>") }
                append("<li>学习提示：${esc(kp.studyTip)}</li>")
                append("</ul>")
            }
        }
        body.section("三、证据链") {
            report.knowledgePoints.filter { it.evidence.isNotEmpty() }.forEach { kp ->
                append("<p><strong>${esc(kp.title)}</strong></p><ul>")
                kp.evidence.forEach { ev -> append("<li>「${esc(ev.quote)}」（来源：${esc(ev.sourceLabel)}）</li>") }
                append("</ul>")
            }
        }
        body.section("四、微测题") {
            report.quizzes.forEach { q ->
                append("<h3>第 ${q.index} 题（${esc(q.typeZh)}）</h3>")
                append("<p>${esc(q.stem)}</p><ul>")
                q.options.forEach { opt -> append("<li>${esc(opt.label)}. ${esc(opt.text)}${if (opt.correct) " ✅" else ""}</li>") }
                append("</ul>")
                append("<p>正确答案：${esc(q.correctLabels.joinToString("、"))}</p>")
                if (q.explanation.isNotBlank()) append("<p>解析：${esc(q.explanation)}</p>")
                if (q.relatedKnowledgePoints.isNotEmpty()) append("<p>关联知识点：${esc(q.relatedKnowledgePoints.joinToString("、"))}</p>")
            }
        }
        body.section("五、需要多练清单") {
            if (report.needPractice.isEmpty()) append("<p>暂无需要多练的知识点。继续保持！</p>")
            report.needPractice.forEach { item ->
                append("<h3>${esc(item.title)}</h3><ul>")
                append("<li>为什么需要多练：${esc(item.reason)}</li>")
                append("<li>建议练习方向：${esc(item.direction)}</li>")
                append("<li>推荐搜索关键词：${esc(item.keywords)}</li>")
                item.searchLinks.forEach { append("<li>${esc(it)}</li>") }
                append("</ul>")
            }
        }
        body.section("六、复习计划") {
            append("<p>预计用时：${report.review.estimatedMinutes} 分钟</p>")
            bucket("今日待复习", report.review.dueToday)
            bucket("近期复习", report.review.upcoming)
            bucket("已掌握", report.review.mastered)
            bucket("需要复核", report.review.needsRecheck)
        }
        body.section("七、问这节课") {
            if (report.askItems.isEmpty()) append("<p>本次会话未提问。</p>")
            report.askItems.forEachIndexed { i, ask ->
                append("<h3>提问 ${i + 1}：${esc(ask.topic)}</h3><ul>")
                append("<li>回答状态：${esc(ask.statusZh)}</li>")
                append("<li>答案摘要：${esc(ask.answerSummary)}</li>")
                ask.evidenceQuotes.firstOrNull()?.let { append("<li>引用证据：「${esc(it)}」</li>") }
                append("</ul>")
            }
        }
        body.section("八、资料来源摘要") {
            append("<p>${esc(report.sourceSummaryLine ?: "资料来源：课堂文本")}</p>")
            report.transcriptSummaryLine?.let { append("<p>${esc(it)}</p>") }
        }
        body.section("九、隐私与说明") {
            append("<ul><li>${esc(PRIVACY_LINE_1)}</li><li>${esc(PRIVACY_LINE_2)}</li><li>${esc(PRIVACY_LINE_3)}</li></ul>")
        }
        report.practice?.let { p ->
            body.section("十、专项练习与错题本") {
                append("<ul>")
                append("<li>本轮练习模式：${esc(p.modeZh)}</li>")
                append("<li>结果：共 ${p.itemCount} 题 · 正确 ${p.correctCount} · 错误 ${p.wrongCount} · 已掌握 ${p.masteredCount} · 需要多练 ${p.needMorePracticeCount}</li>")
                if (p.practicedTopics.isNotEmpty()) append("<li>练习知识点：${esc(p.practicedTopics.joinToString("、"))}</li>")
                if (p.needMoreTopics.isNotEmpty()) append("<li>需要多练项：${esc(p.needMoreTopics.joinToString("、"))}</li>")
                append("<li>下一步建议：${esc(p.nextSuggestion)}</li>")
                append("</ul>")
                if (p.searchQueries.isNotEmpty()) {
                    append("<p>推荐搜索词：</p><ul>")
                    p.searchQueries.forEach { append("<li>${esc(it)}</li>") }
                    append("</ul>")
                }
                if (p.recentRecords.isNotEmpty()) {
                    append("<p>最近练习记录：</p><ul>")
                    p.recentRecords.forEach { append("<li>${esc(it)}</li>") }
                    append("</ul>")
                }
            }
        }

        return """
            <!doctype html>
            <html lang="zh-CN"><head><meta charset="utf-8">
            <title>${esc(report.courseTitle)} · ClassMate 学习报告</title>
            <style>
            @page { size: A4; margin: 18mm; }
            body { font-family: "Noto Sans CJK SC","Microsoft YaHei",system-ui,sans-serif; line-height:1.7; color:#1f2933; max-width:820px; margin:auto; padding:24px; }
            h1 { font-size:24px; margin:0 0 8px; } h2 { font-size:19px; margin:26px 0 10px; border-left:4px solid #2563eb; padding-left:10px; page-break-after:avoid; }
            h3 { font-size:16px; margin:16px 0 6px; } ul { margin:6px 0 12px 22px; } li { margin:3px 0; }
            .meta,.note { color:#52616b; font-size:13px; margin:2px 0; } .note { background:#f3f6f8; padding:8px 12px; border-radius:8px; }
            section { page-break-inside:avoid; }
            </style></head><body>$body</body></html>
        """.trimIndent()
    }

    // ---- Slides (supplemental) ---------------------------------------------------------------

    fun renderSlidesHtml(report: StudyReport): String {
        val slides = buildList {
            add("封面" to "ClassMate 学习报告<br>${esc(report.courseTitle)}<br><small>${esc(report.generatedAtLabel)} · ${esc(report.providerLabel)}</small>")
            add("课程概要" to report.overview.joinToString("<br>") { "• ${esc(it)}" }.ifBlank { "暂无概要" })
            report.localSuggestion?.takeIf { it.isNotBlank() }?.let { add("学习建议（端侧本地智能）" to esc(it)) }
            add("核心知识点" to report.knowledgePoints.joinToString("<br>") { "${it.index}. ${esc(it.title)}（${esc(it.importanceZh)}/${esc(it.difficultyZh)}）" }.ifBlank { "暂无知识点" })
            add("需要多练" to report.needPractice.joinToString("<br>") { "• ${esc(it.title)} — ${esc(it.keywords)}" }.ifBlank { "暂无需要多练项" })
            add("复习计划" to "今日待复习 ${report.review.dueToday.size} · 预计 ${report.review.estimatedMinutes} 分钟")
            add("资料来源摘要" to esc(report.sourceSummaryLine ?: "资料来源：课堂文本"))
        }
        val sections = slides.joinToString("\n") { (h, c) -> "<section class=\"slide\"><h2>${esc(h)}</h2><div>$c</div></section>" }
        return """
            <!doctype html>
            <html lang="zh-CN"><head><meta charset="utf-8"><title>${esc(report.courseTitle)} 演示幻灯片</title>
            <style>body{margin:0;font-family:"Microsoft YaHei",system-ui,sans-serif;color:#111827;} .slide{box-sizing:border-box;min-height:100vh;padding:48px;background:#fff;border-bottom:8px solid #e5e7eb;} h2{font-size:30px;margin-top:0;color:#2563eb;} div{font-size:18px;line-height:1.7;}</style>
            </head><body>$sections</body></html>
        """.trimIndent()
    }

    // ---- helpers -----------------------------------------------------------------------------

    private inline fun StringBuilder.section(title: String, block: StringBuilder.() -> Unit) {
        append("<section><h2>").append(esc(title)).append("</h2>")
        block()
        append("</section>")
    }

    private fun StringBuilder.ul(items: List<String>) {
        if (items.isEmpty()) { append("<p>（暂无）</p>"); return }
        append("<ul>")
        items.forEach { append("<li>").append(esc(it)).append("</li>") }
        append("</ul>")
    }

    private fun StringBuilder.bucket(label: String, items: List<String>) {
        if (items.isEmpty()) { append("<p>$label：无</p>"); return }
        append("<p>$label：</p><ul>")
        items.forEach { append("<li>").append(esc(it)).append("</li>") }
        append("</ul>")
    }

    private fun esc(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
