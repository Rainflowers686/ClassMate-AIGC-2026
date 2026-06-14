package com.classmate.app.ui.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classmate.app.ui.theme.ClassMateTheme

/**
 * Stage 10 — a greenfield product UI language for ClassMate. The defining move (vs. 9A–9C) is
 * dropping the "stack of bordered white cards" for an Apple/Notion/Linear vocabulary:
 *  - a faint tinted CANVAS for depth,
 *  - a big-title scaffold with a slim transparent bar,
 *  - GROUPED-INSET ROWS (one rounded container, hairline-divided rows) instead of N stacked cards,
 *  - exactly one dominant PrimaryCommand per screen.
 * Colors come from the existing Focus theme; this is a layout/hierarchy rebuild, not a recolor.
 */

object ProductSpace {
    val gutter = 22.dp
    val block = 18.dp // gap between major blocks
    val tight = 10.dp
}

/** Faint top-down canvas tint — gives the page depth without a poster gradient. */
@Composable
fun ProductCanvas(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to cs.surfaceVariant.copy(alpha = 0.7f),
                0.22f to cs.background,
                1f to cs.background,
            ),
        ),
    ) { content() }
}

/** Slim transparent top bar — the big title lives in content ([ProductHero]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScaffold(
    contextLabel: String = "",
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        contextLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}

/** Press feedback used by tappable product surfaces. */
@Composable
fun Modifier.productPress(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")
    return this.scale(s)
}

/** The big page title block — generous, confident, lots of air. */
@Composable
fun ProductHero(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(overline.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 33.sp, lineHeight = 39.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(ProductSpace.tight))
            trailing()
        }
    }
}

/** Quiet section label: small caps, muted, with an optional trailing action. */
@Composable
fun ProductSectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
        trailing?.invoke()
    }
}

/** The single dominant action of a page: tall, filled with primary, leading glyph + chevron. */
@Composable
fun PrimaryCommand(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.fillMaxWidth().productPress(interaction).clickable(interaction, indication = null) { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = cs.primary,
        contentColor = cs.onPrimary,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(cs.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onPrimary.copy(alpha = 0.82f))
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

/** A borderless soft card for hero-ish content blocks (no hard outline — the calm default). */
@Composable
fun QuietCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val base = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.productPress(interaction) else Modifier).then(modifier)
    Surface(
        modifier = if (onClick != null) base.clickable(interaction, indication = null) { onClick() } else base,
        shape = RoundedCornerShape(20.dp),
        color = cs.surface,
        contentColor = cs.onSurface,
        shadowElevation = 2.dp,
    ) { Column(Modifier.padding(padding), content = content) }
}

/** Inset hairline used between grouped rows. */
@Composable
fun RowHairline(inset: Dp = 56.dp) {
    Box(Modifier.fillMaxWidth().padding(start = inset).height(0.75.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/** One row in a [GroupedList]: optional accent glyph, title, subtitle, optional trailing value + chevron. */
data class ProductRow(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val accent: Color? = null,
    val trailing: String? = null,
    val onClick: (() -> Unit)? = null,
)

/**
 * Grouped-inset list — ONE rounded container holding hairline-divided rows. This is the core
 * "not a stack of cards" surface. Replaces N separate bordered cards across the app.
 */
@Composable
fun GroupedList(rows: List<ProductRow>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = cs.surface, shadowElevation = 1.dp) {
        Column {
            rows.forEachIndexed { i, r ->
                if (i > 0) RowHairline(inset = if (r.icon != null) 56.dp else 18.dp)
                GroupedRow(r)
            }
        }
    }
}

@Composable
private fun GroupedRow(r: ProductRow) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val accent = r.accent ?: cs.primary
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (r.onClick != null) Modifier.clickable(interaction, indication = null) { r.onClick.invoke() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (r.icon != null) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(r.icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(r.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (r.subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(r.subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (r.trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(r.trailing, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
        if (r.onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = cs.outline, modifier = Modifier.size(20.dp))
        }
    }
}

/** Stat strip — big numbers with hairline separators, inside one quiet block. */
@Composable
fun StatStrip(items: List<Pair<String, String>>, modifier: Modifier = Modifier, accentFirst: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    QuietCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, (value, label) ->
                if (i > 0) Box(Modifier.width(0.75.dp).height(34.dp).background(cs.outlineVariant))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (i == 0 && accentFirst) cs.primary else cs.onSurface,
                    )
                    Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

/** One-line provider path strip: ● 云端蓝心 · ● 端侧蓝心 · ● 安全占位 (active highlighted). */
@Composable
fun ProviderPathStrip(activeIndex: Int, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    val nodes = listOf("云端蓝心" to cs.primary, "端侧蓝心" to ext.success, "安全占位" to cs.outline)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        nodes.forEachIndexed { i, (label, dot) ->
            if (i > 0) Text("·", style = MaterialTheme.typography.bodySmall, color = cs.outline, modifier = Modifier.padding(horizontal = 8.dp))
            Box(Modifier.size(if (i == activeIndex) 8.dp else 6.dp).clip(CircleShape).background(if (i == activeIndex) dot else cs.outlineVariant))
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (i == activeIndex) cs.onSurface else cs.onSurfaceVariant,
                fontWeight = if (i == activeIndex) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/** Small status pill (source / state) with a semantic dot. */
@Composable
fun ProductPill(label: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    val dot = when {
        label.contains("云端") -> cs.primary
        label.contains("端侧") -> ext.success
        else -> cs.outline
    }
    Surface(modifier = modifier, shape = CircleShape, color = cs.surfaceVariant) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** Animated collapsible block — secondary content folds away by default. */
@Composable
fun ProductCollapse(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column(modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (expanded) "收起" else "展开", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(Modifier.padding(top = ProductSpace.tight), content = content)
        }
    }
}
