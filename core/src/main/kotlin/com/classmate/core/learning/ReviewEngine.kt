package com.classmate.core.learning

import com.classmate.core.model.CourseAnalysisResult

/**
 * Pure, deterministic adaptive-review rules. Every function takes a [LearningSnapshot] plus an
 * explicit `now` and returns a new snapshot, so the whole policy is unit-testable without Android,
 * clocks, or I/O. [LearningStore] wraps these with persistence.
 *
 * Ordering authority for the due list is `manuallyPinned desc, sortIndex asc` — priority is a
 * label that, when it changes, repositions a task via [sortIndex] (so it visibly moves up/down).
 */
object ReviewEngine {

    private const val MINUTE_MS = 60_000L
    private const val DAY_MS = 24L * 60L * MINUTE_MS
    private const val TRACEBACK_THRESHOLD = 3

    // --- queries ---

    fun listDueTasks(s: LearningSnapshot, now: Long): List<ReviewTask> =
        s.tasks.filter { it.active() && it.effectiveDue(now) }.sortedWith(DUE_ORDER)

    fun listUpcomingTasks(s: LearningSnapshot, now: Long): List<ReviewTask> =
        s.tasks.filter { it.active() && it.effectiveUpcoming(now) }.sortedWith(DUE_ORDER)

    fun dueCount(s: LearningSnapshot, now: Long): Int = listDueTasks(s, now).size
    fun totalDueMinutes(s: LearningSnapshot, now: Long): Int = listDueTasks(s, now).sumOf { it.estimatedMinutes }
    fun weakCount(s: LearningSnapshot): Int =
        s.tasks.count { it.active() && (it.counters.wrongAnswer > 0 || it.counters.tooHard > 0) }
    fun needsHumanReviewCount(s: LearningSnapshot): Int = s.tasks.count { it.active() && it.needsHumanReview }

    // --- task generation (idempotent per courseSessionId+knowledgePointId) ---

    fun generateTasks(
        s: LearningSnapshot,
        result: CourseAnalysisResult,
        courseTitle: String,
        sourceProvider: String,
        sourceProfile: String,
        sourceModel: String,
        now: Long,
    ): LearningSnapshot {
        val existing = s.tasks.map { it.courseSessionId + "|" + it.knowledgePointId }.toSet()
        val newKps = result.knowledgePoints
            .filter { (result.sessionId + "|" + it.id) !in existing }
            .sortedByDescending { it.importance.ordinal * 2 + it.difficulty.ordinal }
        if (newKps.isEmpty()) return s
        val base = (s.tasks.maxOfOrNull { it.sortIndex } ?: 0L)
        val newTasks = newKps.mapIndexed { i, kp ->
            val priority = kp.importance.ordinal * 2 + kp.difficulty.ordinal
            ReviewTask(
                taskId = "task_${result.sessionId}_${kp.id}",
                knowledgePointId = kp.id,
                courseSessionId = result.sessionId,
                courseTitle = courseTitle,
                title = kp.title,
                reason = "新学知识点，建议尽快复习",
                priority = priority,
                difficulty = kp.difficulty.name,
                estimatedMinutes = 3 + kp.difficulty.ordinal,
                createdAt = now,
                dueAt = now,
                nextReviewAt = now,
                status = ReviewTaskStatus.DUE,
                sourceProvider = sourceProvider,
                sourceProfile = sourceProfile,
                sourceModel = sourceModel,
                sortIndex = base + 1 + i,
            )
        }
        return s.copy(tasks = s.tasks + newTasks)
    }

    // --- quiz + feedback ---

    fun recordQuizAttempt(
        s: LearningSnapshot,
        courseSessionId: String,
        knowledgePointId: String,
        quizId: String,
        selectedAnswer: String,
        correctAnswer: String,
        isCorrect: Boolean,
        now: Long,
    ): LearningSnapshot {
        val taskId = s.tasks.firstOrNull { it.courseSessionId == courseSessionId && it.knowledgePointId == knowledgePointId }?.taskId
        val attempt = LearnerQuizAttempt(
            attemptId = "att_${s.attempts.size + 1}_$now",
            taskId = taskId,
            courseSessionId = courseSessionId,
            knowledgePointId = knowledgePointId,
            quizId = quizId,
            selectedAnswer = selectedAnswer,
            correctAnswer = correctAnswer,
            isCorrect = isCorrect,
            createdAt = now,
        )
        val withAttempt = s.copy(attempts = s.attempts + attempt)
        val type = if (isCorrect) ReviewEventType.CORRECT_ANSWER else ReviewEventType.WRONG_ANSWER
        return recordFeedback(withAttempt, courseSessionId, knowledgePointId, type, now)
    }

    /** Feedback addressed by (courseSessionId, knowledgePointId) — used by the timeline/quiz flow. */
    fun recordFeedback(
        s: LearningSnapshot,
        courseSessionId: String,
        knowledgePointId: String,
        type: ReviewEventType,
        now: Long,
        note: String? = null,
    ): LearningSnapshot {
        val task = s.tasks.firstOrNull { it.courseSessionId == courseSessionId && it.knowledgePointId == knowledgePointId }
        return applyFeedback(s, task?.taskId, courseSessionId, knowledgePointId, type, now, note)
    }

    /** Feedback addressed by taskId — used by the Review tab task cards. */
    fun recordFeedbackForTask(s: LearningSnapshot, taskId: String, type: ReviewEventType, now: Long, note: String? = null): LearningSnapshot {
        val task = s.tasks.firstOrNull { it.taskId == taskId } ?: return s
        return applyFeedback(s, taskId, task.courseSessionId, task.knowledgePointId, type, now, note)
    }

    fun recordTracebackOpen(s: LearningSnapshot, courseSessionId: String, knowledgePointId: String, now: Long): LearningSnapshot =
        recordFeedback(s, courseSessionId, knowledgePointId, ReviewEventType.TRACEBACK_OPENED, now)

    private fun applyFeedback(
        s: LearningSnapshot,
        taskId: String?,
        courseSessionId: String,
        knowledgePointId: String,
        type: ReviewEventType,
        now: Long,
        note: String?,
    ): LearningSnapshot {
        val event = ReviewFeedbackEvent(
            eventId = "evt_${s.events.size + 1}_$now",
            taskId = taskId,
            courseSessionId = courseSessionId,
            knowledgePointId = knowledgePointId,
            type = type,
            createdAt = now,
            note = note,
        )
        val front = frontSortIndex(s)
        val tasks = if (taskId == null) s.tasks else s.tasks.map { t ->
            if (t.taskId == taskId) applyRule(t, type, now, front) else t
        }
        return s.copy(tasks = tasks, events = s.events + event)
    }

    private fun applyRule(t: ReviewTask, type: ReviewEventType, now: Long, front: Long): ReviewTask = when (type) {
        ReviewEventType.WRONG_ANSWER -> t.copy(
            counters = t.counters.copy(wrongAnswer = t.counters.wrongAnswer + 1),
            priority = t.priority + 2,
            nextReviewAt = now + 10 * MINUTE_MS,
            status = ReviewTaskStatus.DUE,
            reason = "刚答错，优先复习",
            sortIndex = front,
        )
        ReviewEventType.CORRECT_ANSWER -> t.copy(
            counters = t.counters.copy(correctAnswer = t.counters.correctAnswer + 1),
            priority = maxOf(0, t.priority - 1),
            nextReviewAt = now + DAY_MS,
            status = ReviewTaskStatus.UPCOMING,
            reason = "答对了，明天再巩固",
        )
        ReviewEventType.MASTERED -> t.copy(
            counters = t.counters.copy(mastered = t.counters.mastered + 1),
            priority = maxOf(0, t.priority - 2),
            nextReviewAt = now + 3 * DAY_MS,
            status = ReviewTaskStatus.UPCOMING,
            reason = "你标记已掌握，延后复习",
        )
        ReviewEventType.TOO_HARD -> t.copy(
            counters = t.counters.copy(tooHard = t.counters.tooHard + 1),
            priority = t.priority + 2,
            nextReviewAt = now,
            status = ReviewTaskStatus.DUE,
            reason = "你觉得太难，需要拆解复习",
            sortIndex = front,
        )
        ReviewEventType.NEED_EXAMPLE -> t.copy(
            counters = t.counters.copy(needExample = t.counters.needExample + 1),
            priority = t.priority + 1,
            nextReviewAt = now,
            status = ReviewTaskStatus.DUE,
            reason = "这个知识点需要多练巩固",
            sortIndex = front,
        )
        ReviewEventType.EVIDENCE_WRONG -> t.copy(
            counters = t.counters.copy(evidenceWrong = t.counters.evidenceWrong + 1),
            needsHumanReview = true,
            status = ReviewTaskStatus.DUE,
            reason = "你反馈证据不对，需要复核",
        )
        ReviewEventType.TRACEBACK_OPENED -> {
            val counters = t.counters.copy(tracebackOpened = t.counters.tracebackOpened + 1)
            if (counters.tracebackOpened >= TRACEBACK_THRESHOLD) {
                t.copy(counters = counters, priority = t.priority + 1, reason = "你反复查看证据，建议复习")
            } else {
                t.copy(counters = counters)
            }
        }
        // The remaining types are driven by their dedicated editing methods below.
        else -> t
    }

    // --- markers ---

    fun markTaskDone(s: LearningSnapshot, taskId: String, now: Long): LearningSnapshot =
        s.copy(
            tasks = s.tasks.map {
                if (it.taskId == taskId) it.copy(status = ReviewTaskStatus.DONE, lastReviewedAt = now, reviewCount = it.reviewCount + 1) else it
            },
        )

    // --- editing ---

    fun updateTaskPriority(s: LearningSnapshot, taskId: String, level: ReviewPriorityLevel, now: Long): LearningSnapshot {
        val priority = when (level) {
            ReviewPriorityLevel.HIGH -> 10
            ReviewPriorityLevel.MEDIUM -> 5
            ReviewPriorityLevel.LOW -> 0
        }
        val front = frontSortIndex(s)
        val back = backSortIndex(s)
        val tasks = s.tasks.map { t ->
            if (t.taskId != taskId) {
                t
            } else {
                val sortIndex = when (level) {
                    ReviewPriorityLevel.HIGH -> front
                    ReviewPriorityLevel.LOW -> back
                    ReviewPriorityLevel.MEDIUM -> t.sortIndex
                }
                t.copy(priority = priority, sortIndex = sortIndex)
            }
        }
        return appendEvent(s.copy(tasks = tasks), taskId, ReviewEventType.PRIORITY_CHANGED, now, level.name)
    }

    fun addManualTask(
        s: LearningSnapshot,
        courseSessionId: String,
        courseTitle: String,
        knowledgePointId: String,
        title: String,
        difficultyName: String,
        sourceProvider: String,
        sourceProfile: String,
        sourceModel: String,
        now: Long,
    ): LearningSnapshot {
        val front = frontSortIndex(s)
        val existing = s.tasks.firstOrNull { it.courseSessionId == courseSessionId && it.knowledgePointId == knowledgePointId }
        val updated = if (existing != null) {
            s.copy(
                tasks = s.tasks.map {
                    if (it.taskId == existing.taskId) {
                        it.copy(
                            manuallyRemoved = false,
                            status = ReviewTaskStatus.DUE,
                            sortIndex = front,
                            counters = it.counters.copy(userAdded = it.counters.userAdded + 1),
                            reason = "你手动加入复习",
                        )
                    } else {
                        it
                    }
                },
            )
        } else {
            val task = ReviewTask(
                taskId = "manual_${courseSessionId}_${knowledgePointId}_$now",
                knowledgePointId = knowledgePointId,
                courseSessionId = courseSessionId,
                courseTitle = courseTitle,
                title = title,
                reason = "你手动加入复习",
                priority = 6,
                difficulty = difficultyName,
                estimatedMinutes = 4,
                createdAt = now,
                dueAt = now,
                nextReviewAt = now,
                status = ReviewTaskStatus.DUE,
                sourceProvider = sourceProvider,
                sourceProfile = sourceProfile,
                sourceModel = sourceModel,
                counters = FeedbackCounters(userAdded = 1),
                sortIndex = front,
            )
            s.copy(tasks = s.tasks + task)
        }
        val targetId = existing?.taskId ?: updated.tasks.last().taskId
        return appendEvent(updated, targetId, ReviewEventType.USER_ADDED, now, null, courseSessionId, knowledgePointId)
    }

    fun removeTaskFromPlan(s: LearningSnapshot, taskId: String, now: Long): LearningSnapshot {
        val tasks = s.tasks.map {
            if (it.taskId == taskId) {
                it.copy(manuallyRemoved = true, priority = maxOf(0, it.priority - 2), counters = it.counters.copy(userRemoved = it.counters.userRemoved + 1))
            } else {
                it
            }
        }
        return appendEvent(s.copy(tasks = tasks), taskId, ReviewEventType.USER_REMOVED, now)
    }

    fun restoreRemovedTask(s: LearningSnapshot, taskId: String, now: Long): LearningSnapshot {
        val front = frontSortIndex(s)
        val tasks = s.tasks.map {
            if (it.taskId == taskId) it.copy(manuallyRemoved = false, status = ReviewTaskStatus.DUE, sortIndex = front) else it
        }
        return appendEvent(s.copy(tasks = tasks), taskId, ReviewEventType.USER_ADDED, now, "restore")
    }

    fun moveTaskUp(s: LearningSnapshot, taskId: String, now: Long): LearningSnapshot = move(s, taskId, now, up = true)
    fun moveTaskDown(s: LearningSnapshot, taskId: String, now: Long): LearningSnapshot = move(s, taskId, now, up = false)

    fun deleteTasksForCourseSession(s: LearningSnapshot, courseSessionId: String): LearningSnapshot =
        s.copy(
            tasks = s.tasks.filterNot { it.courseSessionId == courseSessionId },
            attempts = s.attempts.filterNot { it.courseSessionId == courseSessionId },
            events = s.events.filterNot { it.courseSessionId == courseSessionId },
        )

    // --- internals ---

    private fun move(s: LearningSnapshot, taskId: String, now: Long, up: Boolean): LearningSnapshot {
        val ordered = listDueTasks(s, now)
        val idx = ordered.indexOfFirst { it.taskId == taskId }
        if (idx < 0) return s
        val neighborIdx = if (up) idx - 1 else idx + 1
        if (neighborIdx !in ordered.indices) return s
        val a = ordered[idx]
        val b = ordered[neighborIdx]
        val tasks = s.tasks.map {
            when (it.taskId) {
                a.taskId -> it.copy(sortIndex = b.sortIndex)
                b.taskId -> it.copy(sortIndex = a.sortIndex)
                else -> it
            }
        }
        return appendEvent(s.copy(tasks = tasks), taskId, ReviewEventType.REORDERED, now)
    }

    private fun appendEvent(
        s: LearningSnapshot,
        taskId: String?,
        type: ReviewEventType,
        now: Long,
        note: String? = null,
        courseSessionId: String? = null,
        knowledgePointId: String? = null,
    ): LearningSnapshot {
        val task = taskId?.let { id -> s.tasks.firstOrNull { it.taskId == id } }
        val event = ReviewFeedbackEvent(
            eventId = "evt_${s.events.size + 1}_$now",
            taskId = taskId,
            courseSessionId = courseSessionId ?: task?.courseSessionId ?: "",
            knowledgePointId = knowledgePointId ?: task?.knowledgePointId ?: "",
            type = type,
            createdAt = now,
            note = note,
        )
        return s.copy(events = s.events + event)
    }

    private fun frontSortIndex(s: LearningSnapshot): Long = (s.tasks.minOfOrNull { it.sortIndex } ?: 0L) - 1
    private fun backSortIndex(s: LearningSnapshot): Long = (s.tasks.maxOfOrNull { it.sortIndex } ?: 0L) + 1

    private fun ReviewTask.active(): Boolean = !manuallyRemoved && status != ReviewTaskStatus.DONE
    private fun ReviewTask.effectiveDue(now: Long): Boolean =
        status == ReviewTaskStatus.DUE || (status == ReviewTaskStatus.UPCOMING && nextReviewAt <= now)
    private fun ReviewTask.effectiveUpcoming(now: Long): Boolean =
        status == ReviewTaskStatus.UPCOMING && nextReviewAt > now

    private val DUE_ORDER: Comparator<ReviewTask> =
        compareByDescending<ReviewTask> { it.manuallyPinned }.thenBy { it.sortIndex }.thenBy { it.createdAt }
}
