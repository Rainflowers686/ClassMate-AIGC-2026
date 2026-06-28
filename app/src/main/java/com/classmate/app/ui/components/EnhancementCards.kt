package com.classmate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.EnhancementUiState
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.i18n.AppLanguage
import com.classmate.app.ui.i18n.appStrings

/**
 * Shared status surfaces for long-running tasks (analysis / import / OCR / export / study pack / AI
 * enhancement). They replace scattered toasts and cramped button rows with one consistent shape:
 * a prominent loading card, a prominent error card (what happened / why / next step), and an action
 * row that stacks vertically on narrow screens. None of them ever shows a fake progress percent.
 */

/** A button layout that lays out 2 actions side-by-side on wide screens but stacks vertically when narrow
 *  (or when there are 3 actions), so buttons are never cramped into one tight row. */
@Composable
fun ActionButtonRow(
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryText: String? = null,
    onTertiary: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
) {
    val hasTertiary = tertiaryText != null && onTertiary != null
    val hasSecondary = secondaryText != null && onSecondary != null
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stack = maxWidth < 340.dp || hasTertiary
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.s)) {
                PrimaryButton(primaryText, onPrimary, Modifier.fillMaxWidth(), enabled = primaryEnabled)
                if (hasSecondary) SecondaryButton(secondaryText!!, onSecondary!!, Modifier.fillMaxWidth())
                if (hasTertiary) TertiaryButton(tertiaryText!!, onTertiary!!, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                if (hasSecondary) {
                    SecondaryButton(secondaryText!!, onSecondary!!, Modifier.weight(1f))
                    PrimaryButton(primaryText, onPrimary, Modifier.weight(1f), enabled = primaryEnabled)
                } else {
                    PrimaryButton(primaryText, onPrimary, Modifier.fillMaxWidth(), enabled = primaryEnabled)
                }
            }
        }
    }
}

/** A prominent "working" card: task title + status line + optional estimate + long-wait note. The bar is an
 *  honest indeterminate heartbeat — never a fabricated percent. */
@Composable
fun ProminentLoadingCard(
    title: String,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    estimateText: String? = null,
    longWaitNote: String? = null,
    onCancel: (() -> Unit)? = null,
    language: AppLanguage = AppLanguage.ZH,
    cancelText: String = appStrings(language).enhancementCancel,
) {
    PremiumCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(Dimens.s))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        if (!statusText.isNullOrBlank()) {
            Spacer(Modifier.height(Dimens.xs))
            Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Dimens.s))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp))
        if (!estimateText.isNullOrBlank()) {
            Spacer(Modifier.height(Dimens.xs))
            Text(estimateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!longWaitNote.isNullOrBlank()) {
            Spacer(Modifier.height(Dimens.xs))
            Text(longWaitNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        if (onCancel != null) {
            Spacer(Modifier.height(Dimens.s))
            SecondaryButton(cancelText, onCancel, Modifier.fillMaxWidth())
        }
    }
}

/** A prominent error card: what happened / possible cause / next step, plus a tidy retry + manual/cancel row. */
@Composable
fun ProminentErrorCard(
    whatHappened: String,
    modifier: Modifier = Modifier,
    possibleCause: String? = null,
    nextStep: String? = null,
    retryText: String? = null,
    onRetry: (() -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    language: AppLanguage = AppLanguage.ZH,
) {
    val s = appStrings(language)
    PremiumCard(modifier) {
        Text(whatHappened, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
        if (!possibleCause.isNullOrBlank()) {
            Spacer(Modifier.height(Dimens.xs))
            Text(s.enhancementPossibleCause(possibleCause), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!nextStep.isNullOrBlank()) {
            Spacer(Modifier.height(Dimens.xxs))
            Text(s.enhancementNextStep(nextStep), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onRetry != null && retryText != null) {
            Spacer(Modifier.height(Dimens.s))
            ActionButtonRow(
                primaryText = retryText,
                onPrimary = onRetry,
                secondaryText = secondaryText,
                onSecondary = onSecondary,
                tertiaryText = dismissText,
                onTertiary = onDismiss,
            )
        }
    }
}

/**
 * A complete AI-enhancement surface: a trigger when idle, a loading card while running, a result card with
 * its honest source label + copy when done, and an error card on failure. Reused by study-pack and quiz
 * feedback screens.
 */
@Composable
fun EnhancementPanel(
    state: EnhancementUiState,
    idleTitle: String,
    idleHint: String,
    triggerText: String,
    runningTitle: String,
    onGenerate: () -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    language: AppLanguage = AppLanguage.ZH,
) {
    val s = appStrings(language)
    when {
        state.running -> ProminentLoadingCard(
            title = runningTitle,
            statusText = s.enhancementRunningStatus,
            language = language,
            modifier = modifier,
        )
        state.failed -> ProminentErrorCard(
            whatHappened = s.enhancementAiIncomplete,
            possibleCause = s.enhancementAiUnavailable,
            nextStep = s.enhancementUseBasicMaterial,
            retryText = s.retry,
            onRetry = onGenerate,
            secondaryText = s.cancel,
            onSecondary = onDismiss,
            language = language,
            modifier = modifier,
        )
        state.hasResult -> PremiumCard(modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(idleTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusChip(state.sourceZh, tone = ChipTone.INFO)
            }
            Spacer(Modifier.height(Dimens.s))
            Text(state.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Dimens.s))
            ActionButtonRow(
                primaryText = s.copy,
                onPrimary = { onCopy(state.text) },
                secondaryText = s.regenerate,
                onSecondary = onGenerate,
            )
        }
        else -> PremiumCard(modifier) {
            Text(idleTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Dimens.xxs))
            Text(idleHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.s))
            PrimaryButton(triggerText, onGenerate, Modifier.fillMaxWidth())
        }
    }
}
