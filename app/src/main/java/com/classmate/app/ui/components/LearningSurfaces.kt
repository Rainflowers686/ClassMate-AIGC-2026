package com.classmate.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.theme.ClassMateTheme

/**
 * Learning-space surfaces that survived the Stage 10 product rebuild because they ARE the new visual
 * language for the Course Detail flagship: a segmented action dock and a connected knowledge path
 * with evidence ribbons. The page chrome (hero / canvas / dominant command / grouped rows) now lives
 * in [com.classmate.app.ui.product.ProductUi]; the Stage 9C "surgery" headers and strips were deleted.
 */

/** One action in a [LearningActionDock]. */
data class DockAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** A segmented action dock — learning actions as one connected surface, not stacked buttons. */
@Composable
fun LearningActionDock(actions: List<DockAction>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = cs.surface,
        border = BorderStroke(0.75.dp, cs.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            actions.forEachIndexed { i, a ->
                if (i > 0) Box(Modifier.width(0.75.dp).height(40.dp).background(cs.outlineVariant))
                val interaction = remember { MutableInteractionSource() }
                Column(
                    Modifier.weight(1f).pressable(interaction).clickable(interaction, indication = null) { a.onClick() }.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(a.icon, contentDescription = a.label, tint = cs.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(a.label, style = MaterialTheme.typography.labelMedium, color = cs.onSurface, maxLines = 1)
                }
            }
        }
    }
}

/**
 * A visual learning path: connected nodes (dot + spine line) each carrying a knowledge card. Reads as
 * a route through the lesson, not a flat list.
 */
@Composable
fun KnowledgePathNode(
    index: Int,
    title: String,
    summary: String,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    evidence: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Spine: numbered dot + connecting line.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = cs.primary, modifier = Modifier.size(26.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$index", style = MaterialTheme.typography.labelMedium, color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
            if (!isLast) Box(Modifier.width(2.dp).height(56.dp).background(cs.outlineVariant))
        }
        Spacer(Modifier.width(Dimens.m))
        // Card.
        val interaction = remember { MutableInteractionSource() }
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else Dimens.s)
                .then(if (onClick != null) Modifier.pressable(interaction).clickable(interaction, indication = null) { onClick() } else Modifier),
            shape = MaterialTheme.shapes.medium,
            color = cs.surface,
            border = BorderStroke(0.75.dp, cs.outlineVariant),
            shadowElevation = 1.dp,
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (summary.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (!evidence.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    EvidenceRibbon(quote = evidence, source = "本节课原文")
                }
            }
        }
    }
}

/** Evidence as a citation ribbon: left accent rule + verbatim quote + source tag. */
@Composable
fun EvidenceRibbon(quote: String, source: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(ext.evidenceHighlight.copy(alpha = 0.45f))
            .padding(vertical = 8.dp, horizontal = 10.dp),
    ) {
        Box(Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(ext.evidenceBorder))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("「$quote」", style = MaterialTheme.typography.bodySmall, color = cs.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(source, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
    }
}
