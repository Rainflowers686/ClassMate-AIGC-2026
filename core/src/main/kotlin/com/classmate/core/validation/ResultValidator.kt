package com.classmate.core.validation

import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession

/**
 * The gate every analysis must pass before it reaches the UI. Enforces the product's
 * non-negotiables:
 *  - at least one knowledge point, with unique ids;
 *  - every knowledge point binds an existing segment and >=1 locatable evidence span;
 *  - every question binds >=1 existing knowledge point (reference closure), has a correct
 *    option, and cites locatable evidence.
 * A non-ok report makes the resolver fall back to the next provider.
 */
class ResultValidator(
    private val evidenceValidator: EvidenceValidator = EvidenceValidator(),
) {
    fun validate(result: CourseAnalysisResult, session: CourseSession): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()

        if (result.knowledgePoints.isEmpty()) {
            issues += ValidationIssue("EMPTY_ANALYSIS", "未提炼出任何知识点")
        }

        val kpIds = result.knowledgePoints.map { it.id }
        if (kpIds.size != kpIds.toSet().size) {
            issues += ValidationIssue("DUPLICATE_KP_ID", "知识点 id 存在重复")
        }
        val kpIdSet = kpIds.toSet()

        result.knowledgePoints.forEach { kp ->
            if (session.segment(kp.sourceSegmentId) == null) {
                issues += ValidationIssue("KP_SEGMENT_MISSING", "知识点引用的原文段不存在", kp.id)
            }
            if (!kp.hasEvidence) {
                issues += ValidationIssue("KP_NO_EVIDENCE", "知识点「${kp.title}」缺少可定位证据", kp.id)
            }
            kp.evidence.forEach { span ->
                evidenceValidator.validate(session, span)?.let { problem ->
                    issues += ValidationIssue("EVIDENCE_${problem.name}", "知识点证据无法在原文定位", kp.id)
                }
            }
            kp.relatedPointIds.forEach { related ->
                if (related !in kpIdSet) {
                    issues += ValidationIssue("KP_RELATED_UNKNOWN", "关联知识点 $related 不存在", kp.id)
                }
            }
        }

        result.quizQuestions.forEach { q ->
            if (q.testedKnowledgePointIds.isEmpty()) {
                issues += ValidationIssue("Q_NO_KP", "题目未绑定任何知识点", q.id)
            }
            q.testedKnowledgePointIds.forEach { id ->
                if (id !in kpIdSet) {
                    issues += ValidationIssue("Q_KP_UNKNOWN", "题目引用了不存在的知识点 $id", q.id)
                }
            }
            if (q.options.isEmpty()) {
                issues += ValidationIssue("Q_NO_OPTIONS", "题目没有选项", q.id)
            }
            if (q.correctOptionIds.isEmpty()) {
                issues += ValidationIssue("Q_NO_CORRECT", "题目没有正确选项", q.id)
            }
            if (!q.hasEvidence) {
                issues += ValidationIssue("Q_NO_EVIDENCE", "题目缺少可定位证据", q.id)
            }
            q.evidence.forEach { span ->
                evidenceValidator.validate(session, span)?.let { problem ->
                    issues += ValidationIssue("EVIDENCE_${problem.name}", "题目证据无法在原文定位", q.id)
                }
            }
        }

        return ValidationReport.of(issues)
    }
}
