package com.classmate.core.library

import com.classmate.core.learning.LearningSnapshot

data class CourseRecordSnapshot(
    val id: String,
    val title: String,
    val createdAtEpochMs: Long,
    val providerName: String,
    val profileLabel: String,
    val knowledgePointCount: Int,
    val quizCount: Int,
    val fallbackUsed: Boolean,
)

data class CourseSummary(
    val courseKey: String,
    val courseName: String,
    val subject: String,
    val latestLearningTime: Long,
    val lessonCount: Int,
    val knowledgePointTotal: Int,
    val quizTotal: Int,
    val dueReviewTaskCount: Int,
    val recentProvider: String,
    val recentFallbackUsed: Boolean,
)

object CourseLibraryBuilder {
    fun build(records: List<CourseRecordSnapshot>, learningSnapshot: LearningSnapshot = LearningSnapshot()): List<CourseSummary> =
        records
            .groupBy { normalizeCourseName(it.title) }
            .map { (courseName, group) ->
                val latest = group.maxBy { it.createdAtEpochMs }
                val dueTasks = learningSnapshot.tasks.count {
                    normalizeCourseName(it.courseTitle) == courseName && !it.manuallyRemoved
                }
                CourseSummary(
                    courseKey = courseName.lowercase(),
                    courseName = courseName,
                    subject = subjectFromTitle(courseName),
                    latestLearningTime = latest.createdAtEpochMs,
                    lessonCount = group.size,
                    knowledgePointTotal = group.sumOf { it.knowledgePointCount },
                    quizTotal = group.sumOf { it.quizCount },
                    dueReviewTaskCount = dueTasks,
                    recentProvider = providerLabel(latest.providerName, latest.profileLabel),
                    recentFallbackUsed = latest.fallbackUsed,
                )
            }
            .sortedByDescending { it.latestLearningTime }

    fun normalizeCourseName(title: String): String {
        val clean = title.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return "Untitled Course"
        val delimiters = listOf(" - ", " | ", ":", " / ", " -- ", "\u00B7", "\u8def")
        val first = delimiters.fold(clean) { acc, delimiter -> acc.substringBefore(delimiter) }.trim()
        return first.takeIf { it.isNotBlank() } ?: "Untitled Course"
    }

    fun subjectFromTitle(title: String): String {
        val lower = title.lowercase()
        return when {
            listOf("math", "calculus", "series", "algebra", "geometry").any { it in lower } -> "Math"
            listOf("physics", "mechanics", "electricity").any { it in lower } -> "Physics"
            listOf("chemistry", "organic").any { it in lower } -> "Chemistry"
            else -> title.take(12)
        }
    }

    private fun providerLabel(providerName: String, profileLabel: String): String =
        when {
            providerName.equals("BLUELM", ignoreCase = true) -> "云端蓝心"
            providerName.equals("COMPATIBLE", ignoreCase = true) -> "云端兼容模型"
            providerName.equals("LOCAL_FALLBACK", ignoreCase = true) -> "安全占位"
            profileLabel.isNotBlank() -> profileLabel
            else -> providerName
        }
}
