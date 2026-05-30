package com.classmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.core.model.Quiz

/**
 * One multiple-choice question, mobile-quiz styled.
 *
 * v0.4.1 productization pass:
 *  - Each option is a pill-shaped row with a circular A/B/C/D badge
 *    instead of a Material RadioButton — feels closer to a mobile quiz app.
 *  - Selected (pre-submit) row: brand-tinted background, brand badge.
 *  - Submitted: correct row tinted success, wrong-chosen row tinted error.
 *  - Feedback block hides anything that looks like raw IDs ("kp_001",
 *    "seg_001"); it tells the user "本题依据来自第 N 段" instead.
 *  - The explanation chip explicitly says "为什么这个答案正确".
 */
@Composable
fun QuizCard(
    quiz: Quiz,
    submittedAnswerIndex: Int?,
    onSubmit: (Int) -> Unit,
    onShowEvidence: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current

    var pending by remember(quiz.quizId) { mutableStateOf(submittedAnswerIndex) }

    GlassCard(modifier = modifier) {
        Column {
            Text(
                quiz.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.fgPrimary
            )
            Spacer(Modifier.height(spacing.md))

            quiz.options.forEachIndexed { index, option ->
                val isCorrect = index == quiz.answerIndex
                val isChosen = pending == index
                val style = optionVisualStyle(
                    submittedAnswerIndex = submittedAnswerIndex,
                    isCorrect = isCorrect,
                    isChosen = isChosen,
                    colors = colors
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(style.rowBg, shapes.medium)
                        .border(1.dp, style.borderColor, shapes.medium)
                        .selectable(
                            selected = isChosen,
                            enabled = submittedAnswerIndex == null,
                            onClick = { pending = index },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = spacing.md, vertical = spacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // A / B / C / D badge.
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(style.badgeBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                ('A' + index).toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = style.badgeFg
                            )
                        }
                        Spacer(Modifier.width(spacing.md))
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = style.textColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (submittedAnswerIndex != null) {
                            val mark = when {
                                isCorrect -> "✓"
                                isChosen -> "✗"
                                else -> null
                            }
                            if (mark != null) {
                                Spacer(Modifier.width(spacing.sm))
                                Text(
                                    mark,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = style.markColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.md))

            if (submittedAnswerIndex == null) {
                PrimaryButton(
                    text = "提交答案",
                    onClick = { pending?.let(onSubmit) },
                    enabled = pending != null
                )
            } else {
                SubmittedFeedback(
                    correct = submittedAnswerIndex == quiz.answerIndex,
                    explanation = humanReadableExplanation(quiz.explanation),
                    evidenceSpan = quiz.evidenceSpan,
                    sourceSegmentId = quiz.sourceSegmentId,
                    onShowEvidence = onShowEvidence
                )
            }
        }
    }
}

@Composable
private fun SubmittedFeedback(
    correct: Boolean,
    explanation: String,
    evidenceSpan: String,
    sourceSegmentId: String,
    onShowEvidence: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    val segLabel = humanSegmentLabel(sourceSegmentId)
    // Hairline divider.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.outline)
    )
    Spacer(Modifier.height(spacing.md))

    Text(
        if (correct) "回答正确" else "再想想",
        style = MaterialTheme.typography.titleSmall,
        color = if (correct) colors.statusSuccess else colors.statusError,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(spacing.xs))
    Text(
        "为什么这个答案正确",
        style = MaterialTheme.typography.labelSmall,
        color = colors.fgMuted
    )
    Spacer(Modifier.height(2.dp))
    Text(
        explanation,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.fgPrimary
    )

    Spacer(Modifier.height(spacing.sm))
    Box(
        modifier = Modifier
            .background(colors.evidenceHighlight.copy(alpha = 0.45f), shapes.medium)
            .padding(horizontal = spacing.sm, vertical = spacing.xs)
    ) {
        Text(
            "原文依据 · 「$evidenceSpan」",
            style = MaterialTheme.typography.labelSmall,
            color = colors.evidenceHighlightFg
        )
    }
    Spacer(Modifier.height(spacing.xs))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "依据来自 $segLabel",
            style = MaterialTheme.typography.labelSmall,
            color = colors.fgMuted
        )
        Spacer(Modifier.width(spacing.sm))
        TextButton(
            onClick = onShowEvidence,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 0.dp,
                vertical = 0.dp
            )
        ) {
            Text("查看完整段落 →", color = colors.brandPrimary)
        }
    }
}

/**
 * Removes engineering-leftover text from the explanation so users never see
 * "正确选项对应知识点 kp_001，原文证据来自段落 seg_001" verbatim. Falls
 * back to a generic line when the explanation is essentially structural.
 */
private fun humanReadableExplanation(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "本题答案直接源自课程原文。"
    // Strip references like "kp_001", "seg_001"
    val cleaned = trimmed
        .replace(Regex("[，,]?\\s*正确选项对应知识点\\s*kp_[0-9]+[，,]?"), "")
        .replace(Regex("[，,]?\\s*知识点\\s*kp_[0-9]+[，,]?"), "")
        .replace(Regex("[，,]?\\s*原文证据来自段落\\s*seg_[0-9]+[，,]?"), "")
        .replace(Regex("[，,]?\\s*来源段落\\s*seg_[0-9]+[，,]?"), "")
        .replace(Regex("seg_[0-9]+"), "")
        .replace(Regex("kp_[0-9]+"), "")
        .replace(Regex("q_[0-9]+"), "")
        .replace(Regex("\\s*[，,。]\\s*[，,。]"), "。")
        .trim()
    return if (cleaned.isBlank()) "本题答案直接源自课程原文。" else cleaned
}

private data class OptionStyle(
    val rowBg: Color,
    val borderColor: Color,
    val badgeBg: Color,
    val badgeFg: Color,
    val textColor: Color,
    val markColor: Color
)

private fun optionVisualStyle(
    submittedAnswerIndex: Int?,
    isCorrect: Boolean,
    isChosen: Boolean,
    colors: com.classmate.app.ui.theme.ClassMateColors
): OptionStyle {
    val neutralBadge = colors.brandPrimary.copy(alpha = 0.08f)
    return when {
        submittedAnswerIndex == null && isChosen -> OptionStyle(
            rowBg = colors.brandPrimary.copy(alpha = 0.08f),
            borderColor = colors.brandPrimary.copy(alpha = 0.50f),
            badgeBg = colors.brandPrimary,
            badgeFg = colors.fgOnAccent,
            textColor = colors.fgPrimary,
            markColor = colors.brandPrimary
        )
        submittedAnswerIndex == null -> OptionStyle(
            rowBg = Color.Transparent,
            borderColor = colors.outline,
            badgeBg = neutralBadge,
            badgeFg = colors.brandPrimary,
            textColor = colors.fgPrimary,
            markColor = colors.fgPrimary
        )
        isCorrect -> OptionStyle(
            rowBg = colors.statusSuccess.copy(alpha = 0.10f),
            borderColor = colors.statusSuccess.copy(alpha = 0.50f),
            badgeBg = colors.statusSuccess,
            badgeFg = colors.fgOnAccent,
            textColor = colors.statusSuccess,
            markColor = colors.statusSuccess
        )
        isChosen -> OptionStyle(
            rowBg = colors.statusError.copy(alpha = 0.10f),
            borderColor = colors.statusError.copy(alpha = 0.50f),
            badgeBg = colors.statusError,
            badgeFg = colors.fgOnAccent,
            textColor = colors.statusError,
            markColor = colors.statusError
        )
        else -> OptionStyle(
            rowBg = Color.Transparent,
            borderColor = colors.outline,
            badgeBg = neutralBadge,
            badgeFg = colors.fgMuted,
            textColor = colors.fgSecondary,
            markColor = colors.fgMuted
        )
    }
}
