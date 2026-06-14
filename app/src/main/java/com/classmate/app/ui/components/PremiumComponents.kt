package com.classmate.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.theme.ClassMateTheme

/**
 * Premium primitives that outlived the Stage 10 rebuild because they're still consumed: the pressable
 * scale modifier (shared by the product + learning surfaces), the Analyze-flow [PageHero]/[PremiumCard],
 * and the Settings [CapabilityStatusPill]. The Stage 9B hero/strip/tile/empty-state variants and the
 * ExpandableSection were deleted — their roles moved to ProductHero / GroupedList / ProductCollapse.
 */

/** A pressable scale modifier — the quiet "this is tappable" feedback used across premium tiles. */
@Composable
fun Modifier.pressable(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.975f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")
    return this.scale(s)
}

/** Large-title page header (iOS/Notion feel): an overline, a big confident title, a supporting line. */
@Composable
fun PageHero(
    title: String,
    subtitle: String? = null,
    overline: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    overline.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Dimens.m))
            trailing()
        }
    }
}

/**
 * The premium container. Layered: a hairline border + a whisper of shadow so cards read as objects on
 * paper rather than flat fills. [emphasis] lifts a card (primary CTA surfaces).
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    emphasis: Boolean = false,
    padding: androidx.compose.ui.unit.Dp = Dimens.cardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val base = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.pressable(interaction) else Modifier)
        .then(modifier)
    Surface(
        modifier = if (onClick != null) {
            base.clickable(interaction, indication = null) { onClick() }
        } else {
            base
        },
        shape = MaterialTheme.shapes.large,
        color = cs.surface,
        contentColor = cs.onSurface,
        border = BorderStroke(0.75.dp, if (emphasis) cs.primary.copy(alpha = 0.35f) else cs.outlineVariant),
        shadowElevation = if (emphasis) 3.dp else 1.dp,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** Capability status pill (ready/standby) for the Settings overview row. */
@Composable
fun CapabilityStatusPill(label: String, ready: Boolean, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    val dot = if (ready) ext.success else cs.outline
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceVariant,
        border = BorderStroke(0.75.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (ready) "就绪" else "待初始化",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (ready) ext.success else cs.onSurfaceVariant,
            )
        }
    }
}
