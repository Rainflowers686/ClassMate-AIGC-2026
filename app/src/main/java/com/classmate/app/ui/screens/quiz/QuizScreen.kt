package com.classmate.app.ui.screens.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.product.QuietCard
import com.classmate.app.ui.components.DifficultyBadge
import com.classmate.app.ui.components.EvidenceBlock
import com.classmate.app.ui.components.Pill
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.QuestionTypeChip
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.theme.ClassMateTheme
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType
import com.classmate.core.model.QuizOption
import com.classmate.core.model.QuizQuality
import com.classmate.core.model.QuizQuestion
import com.classmate.app.ui.i18n.Strings
import com.classmate.app.ui.i18n.appStrings

@Composable
fun QuizScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val s = appStrings(ui.language)
    val result = ui.result
    val session = ui.session
    // Only complete, answerable questions reach the user: every option gets a why-right/why-wrong line
    // and every question an explanation; questions with no correct option / too few options are dropped.
    val questions = result?.let { QuizQuality.repairAndFilter(it.quizQuestions) }.orEmpty()

    if (result == null || session == null || questions.isEmpty()) {
        ClassMateScaffold(title = s.quizLabel, onBack = { viewModel.goBackOrHome() }) { padding ->
            Box(Modifier.padding(padding).fillMaxWidth().padding(Dimens.screen)) {
                Text(s.quizEmpty, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }
    val index = ui.currentQuestionIndex.coerceIn(0, questions.lastIndex)
    val question = questions[index]
    val revealed = question.id in ui.revealedQuestionIds
    val selectedOptionId = ui.answers[question.id]
    val isLast = index == questions.lastIndex

    ClassMateScaffold(
        title = s.quizTitle(index + 1, questions.size),
        onBack = { viewModel.goBackOrHome() },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Dimens.screen, vertical = Dimens.m),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.m),
                ) {
                    SecondaryButton(
                        text = s.quizPrev,
                        onClick = { viewModel.goToQuestion(index - 1) },
                        enabled = index > 0,
                        modifier = Modifier.weight(1f),
                    )
                    if (isLast) {
                        PrimaryButton(
                            text = s.quizToReview,
                            onClick = { viewModel.ensureReviewPlan(); viewModel.navigateTo(Screen.REVIEW) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        PrimaryButton(
                            text = s.quizNext,
                            onClick = { viewModel.goToQuestion(index + 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            LinearProgressIndicator(
                progress = (index + 1).toFloat() / questions.size,
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )

            QuietCard {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    QuestionTypeChip(question.type)
                    DifficultyBadge(question.difficulty)
                }
                Spacer(Modifier.height(Dimens.m))
                Text(question.stem, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }

            question.options.forEach { option ->
                OptionRow(
                    option = option,
                    revealed = revealed,
                    selected = selectedOptionId == option.id,
                    strings = s,
                    onClick = { viewModel.answer(question, option.id) },
                )
            }

            if (revealed) {
                AnswerExplanation(viewModel, question, session, s)
            }
        }
    }
}

@Composable
private fun OptionRow(option: QuizOption, revealed: Boolean, selected: Boolean, strings: Strings, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    // Distinct states: neutral / selected (pre-reveal) / correct / your-wrong. Each has a colour AND a
    // label/icon cue, so state never relies on colour alone.
    val accent = when {
        revealed && option.isCorrect -> ext.success
        revealed && selected -> cs.error
        selected -> cs.primary
        else -> cs.onSurfaceVariant
    }
    val borderColor = when {
        revealed && option.isCorrect -> ext.success
        revealed && selected -> cs.error
        selected -> cs.primary
        else -> cs.outlineVariant
    }
    val container = when {
        revealed && option.isCorrect -> ext.success.copy(alpha = 0.12f)
        revealed && selected -> cs.errorContainer.copy(alpha = 0.5f)
        selected -> cs.primary.copy(alpha = 0.08f)
        else -> cs.surface
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!revealed) Modifier.clickable { onClick() } else Modifier),
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(if (selected || (revealed && option.isCorrect)) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A / B / C / D label badge — also a non-colour cue for which option this is.
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(option.id, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(Dimens.m))
                Text(option.text, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, modifier = Modifier.weight(1f))
                when {
                    revealed && option.isCorrect -> {
                        Spacer(Modifier.width(Dimens.s))
                        Icon(Icons.Filled.Check, contentDescription = strings.quizCorrect, tint = ext.success, modifier = Modifier.size(20.dp))
                    }
                    revealed && selected -> {
                        Spacer(Modifier.width(Dimens.s))
                        Icon(Icons.Filled.Close, contentDescription = strings.quizYourChoice, tint = cs.error, modifier = Modifier.size(20.dp))
                    }
                    !revealed && selected -> {
                        Spacer(Modifier.width(Dimens.s))
                        Icon(Icons.Filled.Check, contentDescription = strings.quizSelected, tint = cs.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (revealed && option.rationale.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(option.rationale, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AnswerExplanation(viewModel: AppViewModel, question: QuizQuestion, session: com.classmate.core.model.CourseSession, s: Strings) {
    val result = viewModel.ui.result
    QuietCard {
        Text(s.quizExplanation, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text(question.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)

        val testedTitles = question.testedKnowledgePointIds.mapNotNull { id -> result?.knowledgePoint(id)?.title }
        if (testedTitles.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.m))
            Text(s.quizTestedKp, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(Dimens.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                testedTitles.forEach { title ->
                    Pill(title, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        question.evidence.firstOrNull()?.let { span ->
            val segIndex = session.segment(span.sourceSegmentId)?.index
            Spacer(Modifier.height(Dimens.m))
            EvidenceBlock(quote = span.quote, segmentLabel = if (segIndex != null) s.quizSegmentLabel(segIndex) else s.quizSourceLabel)
        }

        Spacer(Modifier.height(Dimens.m))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.m)) {
            SecondaryButton(
                text = s.quizTooHard,
                onClick = { viewModel.submitFeedback(FeedbackType.TOO_HARD, FeedbackTargetKind.QUIZ_QUESTION, question.id) },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = s.quizNeedMore,
                onClick = { viewModel.submitFeedback(FeedbackType.NEED_MORE_EXAMPLES, FeedbackTargetKind.QUIZ_QUESTION, question.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
