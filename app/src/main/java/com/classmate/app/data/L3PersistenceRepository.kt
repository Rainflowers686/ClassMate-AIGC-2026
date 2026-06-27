package com.classmate.app.data

import com.classmate.app.l3.Evidence
import com.classmate.app.l3.EvidenceAsset
import com.classmate.app.l3.EvidenceAssetType
import com.classmate.app.l3.ExamResultReport
import com.classmate.app.l3.L3GeneratedQuestion
import com.classmate.app.l3.L3MasteryState
import com.classmate.app.l3.L3PipelineSnapshot
import com.classmate.app.l3.L3PracticeAttempt
import com.classmate.app.l3.L3SourceType
import com.classmate.app.l3.LessonSource
import com.classmate.app.l3.MasteryHistoryEvent
import com.classmate.app.l3.MasteryHistoryEventType
import com.classmate.app.l3.MasteryStat
import com.classmate.app.l3.PracticeQuestionMode
import com.classmate.app.l3.ReviewQueueItem
import com.classmate.app.l3.WrongQuestionRecord
import com.classmate.core.model.Difficulty
import java.io.File
import java.util.Base64

class L3PersistenceRepository(private val file: File? = null) {
    fun saveSnapshot(snapshot: L3PipelineSnapshot) {
        val target = file ?: return
        runCatching {
            target.parentFile?.mkdirs()
            target.writeText(encode(snapshot), Charsets.UTF_8)
        }
    }

    fun loadSnapshot(): L3PipelineSnapshot {
        val source = file ?: return L3PipelineSnapshot.Empty
        return runCatching {
            if (!source.exists()) L3PipelineSnapshot.Empty else decode(source.readText(Charsets.UTF_8))
        }.getOrDefault(L3PipelineSnapshot.Empty)
    }

    fun clearIfMatches(sessionIds: Set<String>, courseTitles: Set<String>): Boolean {
        val target = file ?: return true
        return runCatching {
            val snapshot = loadSnapshot()
            val source = snapshot.lessonSource
            val matchesSession = source?.id in sessionIds ||
                snapshot.reviewQueue.any { it.sourceLessonId in sessionIds } ||
                snapshot.masteryStats.any { it.sourceLessonId in sessionIds }
            val normalizedTitles = courseTitles.map { it.trim().lowercase() }.toSet()
            val matchesTitle = source?.title?.trim()?.lowercase() in normalizedTitles
            if (matchesSession || matchesTitle) {
                saveSnapshot(L3PipelineSnapshot.Empty)
            } else if (target.exists()) {
                true
            } else {
                true
            }
        }.isSuccess
    }

    companion object {
        fun disabled(): L3PersistenceRepository = L3PersistenceRepository(null)

        private fun encode(snapshot: L3PipelineSnapshot): String = buildString {
            appendLine("classmate_l3_store_v1")
            snapshot.lessonSource?.let { s ->
                appendLine(row("lesson", s.id, b64(s.title), s.type.name, s.createdAt.toString(), b64(s.rawText), s.status))
            }
            snapshot.evidence.forEach { ev ->
                appendLine(row(
                    "evidence",
                    ev.id,
                    ev.sourceId,
                    ev.sourceType.name,
                    b64(ev.text),
                    ev.segmentStartMs.s(),
                    ev.segmentEndMs.s(),
                    ev.page.s(),
                    ev.blockIndex.s(),
                    ev.providerProvenance,
                    ev.assetId.orEmpty(),
                    b64(ev.sourceLabel),
                    b64(ev.fileName),
                    b64(ev.fileExt),
                    b64(ev.mimeType),
                    b64(ev.localUri),
                    b64(ev.thumbnailRef),
                    b64(ev.imageRef),
                    b64(ev.audioRef),
                    b64(ev.pageHint),
                    b64(ev.segmentHint),
                    b64(ev.transcriptSegment),
                    b64(ev.snippet),
                ))
            }
            snapshot.evidenceAssets.forEach { asset ->
                appendLine(row(
                    "asset",
                    asset.id,
                    asset.type.name,
                    asset.sourceType.name,
                    b64(asset.text),
                    b64(asset.sourceLabel),
                    b64(asset.fileName),
                    b64(asset.fileExt),
                    b64(asset.mimeType),
                    b64(asset.localUri),
                    b64(asset.thumbnailRef),
                    b64(asset.imageRef),
                    b64(asset.audioRef),
                    b64(asset.pageHint),
                    b64(asset.segmentHint),
                    asset.startMs.s(),
                    asset.endMs.s(),
                    asset.createdAt.toString(),
                    asset.status,
                    b64(asset.transcriptSegment),
                    b64(asset.snippet),
                ))
            }
            snapshot.questions.forEach { q ->
                appendLine(row("question", q.id, q.lessonId, q.knowledgePointId, b64(q.stem), q.options.joinToString(",") { b64(it) }, b64(q.correctAnswer), b64(q.explanation), q.evidenceIds.joinToString(","), q.difficulty.name))
            }
            snapshot.attempts.forEach { attempt ->
                appendLine(row("attempt", attempt.id, attempt.questionId, b64(attempt.userAnswer), attempt.correct.toString(), attempt.createdAt.toString(), attempt.selectedAnswers.joinToString(",") { b64(it) }, b64(attempt.textAnswer.orEmpty()), attempt.elapsedMs.s(), attempt.mode.name))
            }
            snapshot.wrongBook.forEach { wrong ->
                appendLine(row(
                    "wrong",
                    wrong.id,
                    wrong.questionId,
                    b64(wrong.userAnswer),
                    b64(wrong.correctAnswer),
                    b64(wrong.explanation),
                    wrong.knowledgePointId,
                    wrong.evidenceIds.joinToString(","),
                    wrong.createdAt.toString(),
                    wrong.retryCount.toString(),
                    b64(wrong.mistakeReason),
                    b64(wrong.remediationHint),
                    wrong.relatedKnowledgePointIds.joinToString(","),
                ))
            }
            snapshot.reviewQueue.forEach { item ->
                appendLine(row(
                    "review",
                    item.id,
                    item.knowledgePointId,
                    item.dueAt.toString(),
                    item.masteryState.name,
                    item.sourceLessonId,
                    item.priority.toString(),
                    item.source,
                    b64(item.arrangementReason),
                    item.evidenceId.orEmpty(),
                    item.recommendedActions.joinToString(",") { b64(it) },
                ))
            }
            snapshot.masteryStats.forEach { stat ->
                appendLine(row("mastery", stat.knowledgePointId, stat.state.name, stat.correctCount.toString(), stat.wrongCount.toString(), stat.lastReviewedAt.s(), stat.nextReviewAt.s(), stat.sourceLessonId))
            }
            snapshot.masteryHistory.forEach { event ->
                appendLine(row("history", event.id, event.knowledgePointId, event.eventType.name, event.oldState.name, event.newState.name, event.createdAt.toString(), event.sourceQuestionId.orEmpty(), event.sourceLessonId.orEmpty()))
            }
            snapshot.examReports.forEach { report ->
                appendLine(row("exam", report.id, report.examSessionId, report.score.toString(), report.correctCount.toString(), report.wrongCount.toString(), report.elapsedMs.toString(), report.weakKnowledgePointIds.joinToString(","), report.wrongQuestionIds.joinToString(","), report.evidenceIds.joinToString(","), report.accuracy.toString(), b64(report.markdownReport), report.generatedAt.toString()))
            }
        }

        private fun decode(raw: String): L3PipelineSnapshot {
            if (!raw.startsWith("classmate_l3_store_v1")) return L3PipelineSnapshot.Empty
            var lesson: LessonSource? = null
            val evidence = mutableListOf<Evidence>()
            val evidenceAssets = mutableListOf<EvidenceAsset>()
            val questions = mutableListOf<L3GeneratedQuestion>()
            val attempts = mutableListOf<L3PracticeAttempt>()
            val wrongBook = mutableListOf<WrongQuestionRecord>()
            val reviewQueue = mutableListOf<ReviewQueueItem>()
            val masteryStats = mutableListOf<MasteryStat>()
            val masteryHistory = mutableListOf<MasteryHistoryEvent>()
            val examReports = mutableListOf<ExamResultReport>()
            raw.lineSequence().drop(1).filter { it.isNotBlank() }.forEach { line ->
                val p = line.split("\t")
                when (p.getOrNull(0)) {
                    "lesson" -> if (p.size >= 7) {
                        lesson = LessonSource(p[1], unb64(p[2]), enumOr(p[3], L3SourceType.TEXT), p[4].toLongOrNull() ?: 0L, unb64(p[5]), p[6])
                    }
                    "evidence" -> if (p.size >= 9) {
                        evidence += Evidence(
                            id = p[1],
                            sourceId = p[2],
                            sourceType = enumOr(p[3], L3SourceType.TEXT),
                            text = unb64(p[4]),
                            segmentStartMs = p[5].toLongOrNullOrNull(),
                            segmentEndMs = p[6].toLongOrNullOrNull(),
                            page = p[7].toIntOrNullOrNull(),
                            blockIndex = p[8].toIntOrNullOrNull(),
                            providerProvenance = p.getOrNull(9).orEmpty(),
                            assetId = p.getOrNull(10)?.ifBlank { null },
                            sourceLabel = p.getOrNull(11)?.let(::unb64).orEmpty(),
                            fileName = p.getOrNull(12)?.let(::unb64).orEmpty(),
                            fileExt = p.getOrNull(13)?.let(::unb64).orEmpty(),
                            mimeType = p.getOrNull(14)?.let(::unb64).orEmpty(),
                            localUri = p.getOrNull(15)?.let(::unb64).orEmpty(),
                            thumbnailRef = p.getOrNull(16)?.let(::unb64).orEmpty(),
                            imageRef = p.getOrNull(17)?.let(::unb64).orEmpty(),
                            audioRef = p.getOrNull(18)?.let(::unb64).orEmpty(),
                            pageHint = p.getOrNull(19)?.let(::unb64).orEmpty(),
                            segmentHint = p.getOrNull(20)?.let(::unb64).orEmpty(),
                            transcriptSegment = p.getOrNull(21)?.let(::unb64).orEmpty(),
                            snippet = p.getOrNull(22)?.let(::unb64).orEmpty(),
                        )
                    }
                    "asset" -> if (p.size >= 19) {
                        evidenceAssets += EvidenceAsset(
                            id = p[1],
                            type = enumOr(p[2], EvidenceAssetType.UNKNOWN),
                            sourceType = enumOr(p[3], L3SourceType.TEXT),
                            text = unb64(p[4]),
                            sourceLabel = unb64(p[5]),
                            fileName = unb64(p[6]),
                            fileExt = unb64(p[7]),
                            mimeType = unb64(p[8]),
                            localUri = unb64(p[9]),
                            thumbnailRef = unb64(p[10]),
                            imageRef = unb64(p[11]),
                            audioRef = unb64(p[12]),
                            pageHint = unb64(p[13]),
                            segmentHint = unb64(p[14]),
                            startMs = p[15].toLongOrNullOrNull(),
                            endMs = p[16].toLongOrNullOrNull(),
                            createdAt = p[17].toLongOrNull() ?: 0L,
                            status = p[18],
                            transcriptSegment = p.getOrNull(19)?.let(::unb64).orEmpty(),
                            snippet = p.getOrNull(20)?.let(::unb64).orEmpty(),
                        )
                    }
                    "question" -> if (p.size >= 10) {
                        questions += L3GeneratedQuestion(p[1], p[2], p[3], unb64(p[4]), p[5].splitList().map(::unb64), unb64(p[6]), unb64(p[7]), p[8].splitList(), enumOr(p[9], Difficulty.MEDIUM))
                    }
                    "attempt" -> if (p.size >= 10) {
                        attempts += L3PracticeAttempt(p[1], p[2], unb64(p[3]), p[4].toBoolean(), p[5].toLongOrNull() ?: 0L, p[6].splitList().map(::unb64), unb64(p[7]).ifBlank { null }, p[8].toLongOrNullOrNull(), enumOr(p[9], PracticeQuestionMode.REAL_QUIZ))
                    }
                    "wrong" -> if (p.size >= 10) {
                        wrongBook += WrongQuestionRecord(
                            p[1],
                            p[2],
                            unb64(p[3]),
                            unb64(p[4]),
                            unb64(p[5]),
                            p[6],
                            p[7].splitList(),
                            p[8].toLongOrNull() ?: 0L,
                            p[9].toIntOrNull() ?: 0,
                            p.getOrNull(10)?.let(::unb64).orEmpty(),
                            p.getOrNull(11)?.let(::unb64).orEmpty(),
                            p.getOrNull(12)?.splitList().orEmpty(),
                        )
                    }
                    "review" -> if (p.size >= 7) {
                        reviewQueue += ReviewQueueItem(
                            p[1],
                            p[2],
                            p[3].toLongOrNull() ?: 0L,
                            enumOr(p[4], L3MasteryState.UNKNOWN),
                            p[5],
                            p[6].toIntOrNull() ?: 1,
                            p.getOrElse(7) { "L3_PIPELINE" },
                            p.getOrNull(8)?.let(::unb64).orEmpty(),
                            p.getOrNull(9)?.ifBlank { null },
                            p.getOrNull(10)?.splitList()?.map(::unb64).orEmpty(),
                        )
                    }
                    "mastery" -> if (p.size >= 8) {
                        masteryStats += MasteryStat(p[1], enumOr(p[2], L3MasteryState.UNKNOWN), p[3].toIntOrNull() ?: 0, p[4].toIntOrNull() ?: 0, p[5].toLongOrNullOrNull(), p[6].toLongOrNullOrNull(), p[7])
                    }
                    "history" -> if (p.size >= 9) {
                        masteryHistory += MasteryHistoryEvent(p[1], p[2], enumOr(p[3], MasteryHistoryEventType.REVIEW_DONE), enumOr(p[4], L3MasteryState.UNKNOWN), enumOr(p[5], L3MasteryState.UNKNOWN), p[6].toLongOrNull() ?: 0L, p[7].ifBlank { null }, p[8].ifBlank { null })
                    }
                    "exam" -> if (p.size >= 13) {
                        examReports += ExamResultReport(p[1], p[2], p[3].toIntOrNull() ?: 0, p[4].toIntOrNull() ?: 0, p[5].toIntOrNull() ?: 0, p[6].toLongOrNull() ?: 0L, p[7].splitList(), p[8].splitList(), p[9].splitList(), p[10].toDoubleOrNull() ?: 0.0, markdownReport = unb64(p[11]), generatedAt = p[12].toLongOrNull() ?: 0L)
                    }
                }
            }
            return L3PipelineSnapshot(
                lessonSource = lesson,
                evidence = evidence,
                evidenceAssets = evidenceAssets,
                questions = questions,
                attempts = attempts,
                wrongBook = wrongBook,
                reviewQueue = reviewQueue,
                masteryStats = masteryStats,
                masteryHistory = masteryHistory,
                examReports = examReports,
            )
        }

        private fun row(vararg values: String): String = values.joinToString("\t")
        private fun b64(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
        private fun unb64(value: String): String = runCatching { String(Base64.getDecoder().decode(value), Charsets.UTF_8) }.getOrDefault("")
        private fun Long?.s(): String = this?.toString().orEmpty()
        private fun Int?.s(): String = this?.toString().orEmpty()
        private fun String.toLongOrNullOrNull(): Long? = takeIf { it.isNotBlank() }?.toLongOrNull()
        private fun String.toIntOrNullOrNull(): Int? = takeIf { it.isNotBlank() }?.toIntOrNull()
        private fun String.splitList(): List<String> = split(",").filter { it.isNotBlank() }
        private inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T =
            enumValues<T>().firstOrNull { it.name == value } ?: fallback
    }
}
