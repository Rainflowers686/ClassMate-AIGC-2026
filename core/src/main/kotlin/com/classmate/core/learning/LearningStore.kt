package com.classmate.core.learning

import com.classmate.core.model.CourseAnalysisResult

/**
 * Persistence seam. Core stays Android-free: the app supplies a file-backed implementation,
 * tests use [InMemorySnapshotIo]. Only business data is ever read/written — never secrets.
 */
interface SnapshotIo {
    fun read(): LearningSnapshot
    fun write(snapshot: LearningSnapshot)
}

class InMemorySnapshotIo(initial: LearningSnapshot = LearningSnapshot()) : SnapshotIo {
    private var current: LearningSnapshot = initial
    override fun read(): LearningSnapshot = current
    override fun write(snapshot: LearningSnapshot) { current = snapshot }
}

/**
 * The single source of truth for the cross-course review queue. Wraps [ReviewEngine] (pure rules)
 * with persistence via [SnapshotIo], so Home / Review / History all read one consistent state.
 * Every mutation commits to the IO immediately (best-effort).
 */
class LearningStore(
    private val io: SnapshotIo = InMemorySnapshotIo(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var snapshot: LearningSnapshot = io.read()

    fun snapshot(): LearningSnapshot = snapshot
    fun load(): LearningSnapshot { snapshot = io.read(); return snapshot }
    fun save() { io.write(snapshot) }

    private fun commit(next: LearningSnapshot): LearningSnapshot {
        snapshot = next
        io.write(next)
        return next
    }

    fun addTasksFromAnalysis(
        result: CourseAnalysisResult,
        courseTitle: String,
        sourceProvider: String,
        sourceProfile: String,
        sourceModel: String,
    ): LearningSnapshot = commit(
        ReviewEngine.generateTasks(snapshot, result, courseTitle, sourceProvider, sourceProfile, sourceModel, clock()),
    )

    fun recordQuizAttempt(
        courseSessionId: String,
        knowledgePointId: String,
        quizId: String,
        selectedAnswer: String,
        correctAnswer: String,
        isCorrect: Boolean,
    ): LearningSnapshot = commit(
        ReviewEngine.recordQuizAttempt(snapshot, courseSessionId, knowledgePointId, quizId, selectedAnswer, correctAnswer, isCorrect, clock()),
    )

    fun recordFeedback(courseSessionId: String, knowledgePointId: String, type: ReviewEventType, note: String? = null): LearningSnapshot =
        commit(ReviewEngine.recordFeedback(snapshot, courseSessionId, knowledgePointId, type, clock(), note))

    fun recordFeedbackForTask(taskId: String, type: ReviewEventType, note: String? = null): LearningSnapshot =
        commit(ReviewEngine.recordFeedbackForTask(snapshot, taskId, type, clock(), note))

    fun recordTracebackOpen(courseSessionId: String, knowledgePointId: String): LearningSnapshot =
        commit(ReviewEngine.recordTracebackOpen(snapshot, courseSessionId, knowledgePointId, clock()))

    fun markTaskDone(taskId: String): LearningSnapshot = commit(ReviewEngine.markTaskDone(snapshot, taskId, clock()))

    fun updateTaskPriority(taskId: String, level: ReviewPriorityLevel): LearningSnapshot =
        commit(ReviewEngine.updateTaskPriority(snapshot, taskId, level, clock()))

    fun addManualTask(
        courseSessionId: String,
        courseTitle: String,
        knowledgePointId: String,
        title: String,
        difficultyName: String,
        sourceProvider: String,
        sourceProfile: String,
        sourceModel: String,
    ): LearningSnapshot = commit(
        ReviewEngine.addManualTask(snapshot, courseSessionId, courseTitle, knowledgePointId, title, difficultyName, sourceProvider, sourceProfile, sourceModel, clock()),
    )

    fun removeTaskFromPlan(taskId: String): LearningSnapshot = commit(ReviewEngine.removeTaskFromPlan(snapshot, taskId, clock()))
    fun restoreRemovedTask(taskId: String): LearningSnapshot = commit(ReviewEngine.restoreRemovedTask(snapshot, taskId, clock()))
    fun moveTaskUp(taskId: String): LearningSnapshot = commit(ReviewEngine.moveTaskUp(snapshot, taskId, clock()))
    fun moveTaskDown(taskId: String): LearningSnapshot = commit(ReviewEngine.moveTaskDown(snapshot, taskId, clock()))

    fun setPinned(taskId: String, pinned: Boolean): LearningSnapshot =
        commit(snapshot.copy(tasks = snapshot.tasks.map { if (it.taskId == taskId) it.copy(manuallyPinned = pinned) else it }))

    fun deleteTasksForCourseSession(courseSessionId: String): LearningSnapshot =
        commit(ReviewEngine.deleteTasksForCourseSession(snapshot, courseSessionId))

    fun deleteCourseSessions(courseSessionIds: Set<String>): LearningSnapshot {
        if (courseSessionIds.isEmpty()) return snapshot
        return commit(
            snapshot.copy(
                tasks = snapshot.tasks.filterNot { it.courseSessionId in courseSessionIds },
                attempts = snapshot.attempts.filterNot { it.courseSessionId in courseSessionIds },
                events = snapshot.events.filterNot { it.courseSessionId in courseSessionIds },
                practiceHistory = snapshot.practiceHistory.filterNot { it.courseSessionId in courseSessionIds },
            ),
        )
    }

    fun listDueTasks(now: Long = clock()): List<ReviewTask> = ReviewEngine.listDueTasks(snapshot, now)
    fun listUpcomingTasks(now: Long = clock()): List<ReviewTask> = ReviewEngine.listUpcomingTasks(snapshot, now)

    /**
     * Commit a practice session: [updated] is the snapshot AFTER the practice write-back (computed by
     * the pure PracticeSessionEngine from this store's current snapshot), and [record] is the summary
     * to append to the 错题本/练习记录 (capped at the most recent [MAX_PRACTICE_HISTORY]). Takes only
     * learning-package types, so core.learning never depends on core.practice.
     */
    fun recordPracticeSession(updated: LearningSnapshot, record: PracticeHistoryRecord): LearningSnapshot =
        commit(updated.copy(practiceHistory = (updated.practiceHistory + record).takeLast(MAX_PRACTICE_HISTORY)))

    private companion object {
        const val MAX_PRACTICE_HISTORY = 100
    }
}

/** Convenience for tests / no-context use. */
fun InMemoryLearningStore(clock: () -> Long = System::currentTimeMillis): LearningStore =
    LearningStore(InMemorySnapshotIo(), clock)
