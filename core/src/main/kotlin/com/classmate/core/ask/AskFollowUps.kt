package com.classmate.core.ask

import com.classmate.core.ai.AiCapability
import com.classmate.core.ai.AiCapabilityRouter
import com.classmate.core.ai.AiExecutionMode
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ai.AiExecutionStatus
import com.classmate.core.ai.AiStage
import com.classmate.core.ai.StageOutcome

data class AskFollowUpResult(
    val questions: List<String>,
    val source: AiExecutionSource,
)

object AskFollowUpGenerator {
    fun generate(
        answer: LessonAnswer,
        candidates: List<AskCandidate>,
        cloud: () -> StageOutcome<List<String>> = { StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING) },
        onDevice: () -> StageOutcome<List<String>> = { StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING) },
        router: AiCapabilityRouter = AiCapabilityRouter(),
    ): AskFollowUpResult {
        val routed = router.route(
            capability = AiCapability.ASK,
            stages = listOf(
                AiStage(AiExecutionSource.CLOUD, cloud),
                AiStage(AiExecutionSource.ON_DEVICE, onDevice),
            ),
            mode = AiExecutionMode.CLOUD_FIRST,
            terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) {
                StageOutcome.Produced(localFollowUps(answer, candidates))
            },
        )
        return AskFollowUpResult(
            questions = routed.value.orEmpty().filter { it.isNotBlank() }.distinct().take(4),
            source = routed.source,
        )
    }

    private fun localFollowUps(answer: LessonAnswer, candidates: List<AskCandidate>): List<String> {
        val titles = (answer.relatedKnowledgePointTitles + candidates.map { it.knowledgePointTitle })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)
        if (titles.isEmpty()) {
            return listOf("Which lesson evidence should I review first?")
        }
        return titles.flatMap { title ->
            listOf(
                "What is the key evidence for $title?",
                "Can you turn $title into one practice question?",
            )
        }.take(4)
    }
}
