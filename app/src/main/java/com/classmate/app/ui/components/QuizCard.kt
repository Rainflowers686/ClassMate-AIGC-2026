package com.classmate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.core.model.Quiz

/**
 * Single multiple-choice quiz. Once submitted, the card flips into a feedback
 * mode showing correctness + explanation + the in-text evidence quote.
 *
 * The persisted answer comes from the caller via [submittedAnswerIndex] —
 * the local mutableState only holds the pre-submit selection.
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
                val color = when {
                    submittedAnswerIndex == null -> colors.fgPrimary
                    isCorrect -> colors.statusSuccess
                    isChosen -> colors.statusError
                    else -> colors.fgSecondary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isChosen,
                            enabled = submittedAnswerIndex == null,
                            onClick = { pending = index },
                            role = Role.RadioButton
                        )
                        .padding(vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(
                        selected = isChosen,
                        onClick = null,
                        enabled = submittedAnswerIndex == null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.brandPrimary,
                            unselectedColor = colors.fgMuted
                        )
                    )
                    Spacer(Modifier.width(spacing.sm))
                    Text(
                        option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
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
                val correct = submittedAnswerIndex == quiz.answerIndex
                Text(
                    if (correct) "✓ 正确" else "✗ 错误",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (correct) colors.statusSuccess else colors.statusError,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(spacing.xs))
                Text(
                    quiz.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.fgPrimary
                )
                Spacer(Modifier.height(spacing.xs))
                Text(
                    "证据：${quiz.evidenceSpan}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.fgMuted
                )
                Spacer(Modifier.height(spacing.xs))
                TextButton(onClick = onShowEvidence) {
                    Text("查看依据段落", color = colors.brandPrimary)
                }
            }
        }
    }
}
