package com.classmate.core.mindmap

import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.LearningState

data class MindMapModel(
    val root: String,
    val children: List<MindMapNode>,
)

data class MindMapNode(
    val knowledgePointId: String,
    val title: String,
    val importance: String,
    val difficulty: String,
    val evidenceSegmentId: String,
    val weakPoint: Boolean,
)

object MindMapBuilder {
    fun fromAnalysis(
        result: CourseAnalysisResult,
        courseTitle: String,
        learningState: LearningState? = null,
        learningSnapshot: LearningSnapshot? = null,
    ): MindMapModel {
        val tasksByKnowledgePoint = learningSnapshot?.tasks.orEmpty().groupBy { it.knowledgePointId }
        return MindMapModel(
            root = courseTitle.ifBlank { "ClassMate course" },
            children = result.knowledgePoints.map { kp ->
                val stateWeak = learningState?.stateOf(kp.id)?.needsReview == true
                val taskWeak = tasksByKnowledgePoint[kp.id].orEmpty().any { task ->
                    task.counters.wrongAnswer >= 2 ||
                        task.counters.needExample >= 1 ||
                        task.priority >= HIGH_PRIORITY ||
                        task.needsHumanReview
                }
                MindMapNode(
                    knowledgePointId = kp.id,
                    title = kp.title,
                    importance = kp.importance.name,
                    difficulty = kp.difficulty.name,
                    evidenceSegmentId = kp.sourceSegmentId,
                    weakPoint = stateWeak || taskWeak,
                )
            },
        )
    }

    private const val HIGH_PRIORITY = 8
}
