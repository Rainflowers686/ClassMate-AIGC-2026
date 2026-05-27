package com.classmate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.core.model.Quiz

/**
 * One multiple-choice question with submit/feedback.
 *
 * The card owns the *transient* "which radio is selected before submit"
 * state. The *durable* answer (after Submit is tapped) is owned by the
 * ViewModel — that's why [submittedAnswerIndex] is a parameter rather than
 * derived locally: once you submit, navigating away and back shows the
 * same outcome.
 */
@Composable
fun QuizCard(
    quiz: Quiz,
    submittedAnswerIndex: Int?,
    onSubmit: (Int) -> Unit,
    onShowEvidence: () -> Unit
) {
    var pending by remember(quiz.quizId) { mutableStateOf(submittedAnswerIndex) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                quiz.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            quiz.options.forEachIndexed { index, option ->
                val isCorrect = index == quiz.answerIndex
                val isChosen = pending == index
                // Tint after submission so feedback is immediate; before that
                // the radio just looks neutral.
                val tint = when {
                    submittedAnswerIndex == null -> null
                    isCorrect -> MaterialTheme.colorScheme.primary
                    isChosen -> MaterialTheme.colorScheme.error
                    else -> null
                }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isChosen,
                            enabled = submittedAnswerIndex == null,
                            onClick = { pending = index },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(
                        selected = isChosen,
                        onClick = null,
                        enabled = submittedAnswerIndex == null
                    )
                    Spacer(Modifier.height(0.dp))
                    Text(
                        option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tint ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (submittedAnswerIndex == null) {
                androidx.compose.material3.Button(
                    onClick = { pending?.let(onSubmit) },
                    enabled = pending != null
                ) { Text("提交答案") }
            } else {
                val correct = submittedAnswerIndex == quiz.answerIndex
                Text(
                    if (correct) "✓ 正确" else "✗ 错误",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(quiz.explanation, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "证据：${quiz.evidenceSpan}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.TextButton(onClick = onShowEvidence) {
                    Text("查看依据段落")
                }
            }
        }
    }
}
