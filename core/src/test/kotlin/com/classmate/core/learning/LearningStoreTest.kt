package com.classmate.core.learning

import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningStoreTest {

    private val now = 1_700_000_000_000L
    private val result = SampleCourses.seriesAnalysis(now)
    private val sessionId = result.sessionId
    private fun firstKpId() = result.knowledgePoints[0].id
    private fun secondKpId() = result.knowledgePoints[1].id

    private fun store(io: SnapshotIo = InMemorySnapshotIo()) = LearningStore(io) { now }

    private fun seeded(io: SnapshotIo = InMemorySnapshotIo()): LearningStore {
        val s = store(io)
        s.addTasksFromAnalysis(result, "无穷级数", "BLUELM", "官方 BlueLM", "qwen3.5-plus")
        return s
    }

    private fun LearningStore.task(kpId: String) =
        snapshot().tasks.first { it.courseSessionId == sessionId && it.knowledgePointId == kpId }

    @Test
    fun analysisGeneratesOneTaskPerKnowledgePoint() {
        val s = seeded()
        assertEquals(result.knowledgePoints.size, s.snapshot().tasks.size)
        assertTrue(s.listDueTasks(now).isNotEmpty())
        assertTrue(s.snapshot().tasks.all { it.sourceModel == "qwen3.5-plus" && it.sourceProfile == "官方 BlueLM" })
    }

    @Test
    fun sameCourseSessionDoesNotDuplicateTasks() {
        val s = seeded()
        val before = s.snapshot().tasks.size
        s.addTasksFromAnalysis(result, "无穷级数", "BLUELM", "官方 BlueLM", "qwen3.5-plus")
        assertEquals(before, s.snapshot().tasks.size)
    }

    @Test
    fun writeThenRebuildReadsBackTasksAttemptsEvents() {
        val io = InMemorySnapshotIo()
        val s = seeded(io)
        s.recordQuizAttempt(sessionId, firstKpId(), "q_1", "B", "A", isCorrect = false)
        s.recordFeedbackForTask(s.task(secondKpId()).taskId, ReviewEventType.MASTERED)

        val rebuilt = store(io)
        rebuilt.load()
        assertEquals(s.snapshot().tasks.size, rebuilt.snapshot().tasks.size)
        assertEquals(1, rebuilt.snapshot().attempts.size)
        assertTrue(rebuilt.snapshot().events.any { it.type == ReviewEventType.WRONG_ANSWER })
        assertTrue(rebuilt.snapshot().events.any { it.type == ReviewEventType.MASTERED })
    }

    @Test
    fun wrongAnswerRaisesPriorityAndPullsForward() {
        val s = seeded()
        val before = s.task(firstKpId())
        s.recordQuizAttempt(sessionId, firstKpId(), "q_1", "B", "A", isCorrect = false)
        val after = s.task(firstKpId())
        assertEquals(before.priority + 2, after.priority)
        assertEquals(now + 10 * 60_000L, after.nextReviewAt)
        assertEquals(ReviewTaskStatus.DUE, after.status)
        assertEquals(1, after.counters.wrongAnswer)
        assertEquals(after.taskId, s.listDueTasks(now).first().taskId)
    }

    @Test
    fun correctAnswerLowersAndDefers() {
        val s = seeded()
        val before = s.task(firstKpId())
        s.recordQuizAttempt(sessionId, firstKpId(), "q_1", "A", "A", isCorrect = true)
        val after = s.task(firstKpId())
        assertTrue(after.priority <= before.priority)
        assertEquals(now + 24L * 3600_000L, after.nextReviewAt)
        assertEquals(ReviewTaskStatus.UPCOMING, after.status)
        assertFalse(s.listDueTasks(now).any { it.taskId == after.taskId })
    }

    @Test
    fun masteredDefersAboutThreeDays() {
        val s = seeded()
        s.recordFeedbackForTask(s.task(firstKpId()).taskId, ReviewEventType.MASTERED)
        val after = s.task(firstKpId())
        assertEquals(now + 3L * 24L * 3600_000L, after.nextReviewAt)
        assertEquals(ReviewTaskStatus.UPCOMING, after.status)
        assertEquals(1, after.counters.mastered)
    }

    @Test
    fun tooHardPullsForwardAndRaises() {
        val s = seeded()
        val before = s.task(firstKpId())
        s.recordFeedbackForTask(before.taskId, ReviewEventType.TOO_HARD)
        val after = s.task(firstKpId())
        assertEquals(before.priority + 2, after.priority)
        assertEquals(ReviewTaskStatus.DUE, after.status)
        assertEquals(after.taskId, s.listDueTasks(now).first().taskId)
    }

    @Test
    fun needExamplePullsForward() {
        val s = seeded()
        val before = s.task(firstKpId())
        s.recordFeedbackForTask(before.taskId, ReviewEventType.NEED_EXAMPLE)
        val after = s.task(firstKpId())
        assertEquals(before.priority + 1, after.priority)
        assertEquals(now, after.nextReviewAt)
        assertEquals(1, after.counters.needExample)
    }

    @Test
    fun evidenceWrongFlagsHumanReviewWithoutMastery() {
        val s = seeded()
        s.recordFeedbackForTask(s.task(firstKpId()).taskId, ReviewEventType.EVIDENCE_WRONG)
        val after = s.task(firstKpId())
        assertTrue(after.needsHumanReview)
        assertEquals(0, after.counters.mastered)
        assertEquals(1, after.counters.evidenceWrong)
    }

    @Test
    fun tracebackThresholdRaisesPriority() {
        val s = seeded()
        val before = s.task(firstKpId())
        repeat(3) { s.recordTracebackOpen(sessionId, firstKpId()) }
        val after = s.task(firstKpId())
        assertEquals(3, after.counters.tracebackOpened)
        assertEquals(before.priority + 1, after.priority)
    }

    @Test
    fun manualAddCreatesUserAddedEventAndDueTask() {
        val s = seeded()
        s.addManualTask(sessionId, "无穷级数", "kp_manual", "我自己加的点", "MEDIUM", "BLUELM", "官方 BlueLM", "qwen3.5-plus")
        assertTrue(s.snapshot().events.any { it.type == ReviewEventType.USER_ADDED })
        val task = s.snapshot().tasks.first { it.knowledgePointId == "kp_manual" }
        assertEquals(1, task.counters.userAdded)
        assertTrue(s.listDueTasks(now).any { it.taskId == task.taskId })
    }

    @Test
    fun removeCreatesUserRemovedAndHidesFromDue() {
        val s = seeded()
        val taskId = s.task(firstKpId()).taskId
        s.removeTaskFromPlan(taskId)
        assertTrue(s.task(firstKpId()).manuallyRemoved)
        assertTrue(s.snapshot().events.any { it.type == ReviewEventType.USER_REMOVED })
        assertFalse(s.listDueTasks(now).any { it.taskId == taskId })
        s.restoreRemovedTask(taskId)
        assertFalse(s.task(firstKpId()).manuallyRemoved)
        assertTrue(s.listDueTasks(now).any { it.taskId == taskId })
    }

    @Test
    fun deleteCourseSessionsRemovesTasksAttemptsEventsAndPracticeHistory() {
        val s = seeded()
        s.recordQuizAttempt(sessionId, firstKpId(), "q_1", "B", "A", isCorrect = false)
        assertTrue(s.snapshot().tasks.isNotEmpty())
        assertTrue(s.snapshot().attempts.isNotEmpty())
        assertTrue(s.snapshot().events.isNotEmpty())

        val after = s.deleteCourseSessions(setOf(sessionId))

        assertEquals(0, after.tasks.size)
        assertEquals(0, after.attempts.size)
        assertEquals(0, after.events.size)
        assertEquals(0, after.practiceHistory.size)
    }

    @Test
    fun updatePriorityCreatesEventAndReorders() {
        val s = seeded()
        val taskId = s.task(secondKpId()).taskId
        s.updateTaskPriority(taskId, ReviewPriorityLevel.HIGH)
        assertEquals(10, s.task(secondKpId()).priority)
        assertTrue(s.snapshot().events.any { it.type == ReviewEventType.PRIORITY_CHANGED })
        assertEquals(taskId, s.listDueTasks(now).first().taskId)
    }

    @Test
    fun moveUpReordersAndCreatesReorderedEvent() {
        val s = seeded()
        val due = s.listDueTasks(now)
        val secondTaskId = due[1].taskId
        s.moveTaskUp(secondTaskId)
        assertEquals(secondTaskId, s.listDueTasks(now).first().taskId)
        assertTrue(s.snapshot().events.any { it.type == ReviewEventType.REORDERED })
    }
}
