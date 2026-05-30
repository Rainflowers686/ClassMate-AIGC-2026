package com.classmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * Single multiple-choice quiz. Once submitted, the card flips into a feedback
 * mode showing correctness + explanation + the in-text evidence quote.
 *
 * v0.4 visual QA fix:
 *  - selected option gets a soft brand-tinted pill background pre-submit,
 *    so the user can see their pick at a glance
 *  - post-submit, the correct row gets a soft success tint and a chosen-wrong
 *    row gets a soft error tint; the rest stay neutral (no large red wash)
 *  - feedback block is separated from options by a hairline divider with
 *    extra vertical spacing
 *  - chip-style "证据" line so the original quote reads as data, not body text
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
                val (rowBg, textColor) = optionTones(
                    submittedAnswerIndex = submittedAnswerIndex,
                    isCorrect = isCorrect,
                    isChosen = isChosen,
                    colors = colors
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(rowBg, shapes.medium)
                        .selectable(
                            selected = isChosen,
                            enabled = submittedAnswerIndex == null,
                            onClick = { pending = index },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = spacing.sm, vertical = spacing.xs)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RadioButton(
                            selected = isChosen,
                            onClick = null,
                            enabled = submittedAnswerIndex == null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.brandPrimary,
                                unselectedColor = colors.fgMuted,
                                disabledSelectedColor = if (isCorrect) colors.statusSuccess else colors.statusError,
                                disabledUnselectedColor = colors.fgMuted
                            )
                        )
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.outline)
                )
                Spacer(Modifier.height(spacing.md))
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
                Spacer(Modifier.height(spacing.sm))
                Box(
                    modifier = Modifier
                        .background(colors.evidenceHighlight.copy(alpha = 0.45f), shapes.medium)
                        .padding(horizontal = spacing.sm, vertical = spacing.xs)
                ) {
                    Text(
                        "证据：${quiz.evidenceSpan}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.evidenceHighlightFg
                    )
                }
                Spacer(Modifier.height(spacing.xs))
                TextButton(onClick = onShowEvidence) {
                    Text("查看依据段落 →", color = colors.brandPrimary)
                }
            }
        }
    }
}

/**
 * Returns (rowBackgroundColor, optionTextColor) for one option row, based on
 * whether the quiz is submitted and which row this is. We tint backgrounds
 * very softly so the row doesn't dominate the card.
 */
private fun optionTones(
    submittedAnswerIndex: Int?,
    isCorrect: Boolean,
    isChosen: Boolean,
    colors: com.classmate.app.ui.theme.ClassMateColors
): Pair<Color, Color> {
    return when {
        submittedAnswerIndex == null && isChosen ->
            colors.brandPrimary.copy(alpha = 0.08f) to colors.fgPrimary
        submittedAnswerIndex == null ->
            Color.Transparent to colors.fgPrimary
        isCorrect ->
            colors.statusSuccess.copy(alpha = 0.10f) to colors.statusSuccess
        isChosen ->
            colors.statusError.copy(alpha = 0.10f) to colors.statusError
        else ->
            Color.Transparent to colors.fgSecondary
    }
}
