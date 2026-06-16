package com.classmate.app.ui.flow

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Flow companion UI — an immersive focus / ambient study space, reverse-engineered from
 * docs/design_refs/classmate_flow.html (rendered to .codex_work/design_refs_rendered/flow/). These
 * composables serve ONLY the FlowCompanion screen — they are NOT a global theme and must not be
 * applied to Home / Import / Course / History / Settings. The look is a warm dark light-field backdrop
 * (radial gradient + drifting glow + vignette, not flat black), a breathing timer ring as the hero,
 * dark glass panels, sound-scene tiles, and a cached-knowledge surface. Audio is local-only: bundled
 * licensed loops, no recording, no upload, no runtime synthesis.
 */

// ── Scenes ─────────────────────────────────────────────────────────────────────────────────────────
/** One ambient scene: name + mood + its own light-field recipe (matches the Flow mockup's 6 scenes). */
data class FlowCompScene(
    val id: String,
    val name: String,
    val mood: String,
    val sound: AmbientSound,
    val accent: Color,
    val backdrop: List<Color>,
    val anchorX: Float,
    val anchorY: Float,
)

val flowCompScenes: List<FlowCompScene> = listOf(
    FlowCompScene("rain", "窗边细雨", "轻雨 · 稳定", AmbientSoundCatalog.byId("rain")!!, Color(0xFF8FB6DD), listOf(Color(0xFF1B2738), Color(0xFF131B29), Color(0xFF0E131D)), 0.30f, 0.08f),
    FlowCompScene("forest", "森林晨读", "鸟鸣 · 清醒", AmbientSoundCatalog.byId("forest")!!, Color(0xFF8FC6A0), listOf(Color(0xFF1A2A22), Color(0xFF15201B), Color(0xFF0F1714)), 0.70f, 0.10f),
    FlowCompScene("ocean", "海边复盘", "海浪 · 宽阔", AmbientSoundCatalog.byId("ocean")!!, Color(0xFF75B8C8), listOf(Color(0xFF172B35), Color(0xFF10202A), Color(0xFF0C141B)), 0.40f, 0.16f),
    FlowCompScene("stream", "溪边专注", "水流 · 轻快", AmbientSoundCatalog.byId("stream")!!, Color(0xFF8ECAB9), listOf(Color(0xFF173026), Color(0xFF12231D), Color(0xFF0E1714)), 0.62f, 0.18f),
    FlowCompScene("cafe", "清晨咖啡馆", "低语 · 杯响", AmbientSoundCatalog.byId("cafe")!!, Color(0xFFC9AD88), listOf(Color(0xFF2F2A24), Color(0xFF221E18), Color(0xFF16130F)), 0.60f, 0.18f),
    FlowCompScene("night_crickets", "夜间书桌", "虫鸣 · 夜读", AmbientSoundCatalog.byId("night_crickets")!!, Color(0xFFE0A86A), listOf(Color(0xFF2C2418), Color(0xFF1A1813), Color(0xFF111016)), 0.78f, 0.12f),
)

fun flowCompSceneOf(id: String): FlowCompScene = flowCompScenes.firstOrNull { it.id == id } ?: flowCompScenes.first()

/** Honest, never-overclaim copy for the companion's audio/companion features. */
object FlowCompanionCopy {
    const val COMPANION_TAG = "沉浸陪学"
    const val AUDIO_DISCLAIMER = "背景音来自内置授权循环素材，仅本地播放；不录音、不上传、不使用实时生成。"
    const val TRANSCRIPT_NOTE = "转写能力按配置启用；阶段总结需用户确认后才并入复习计划。"
}

object FlowCompColors {
    val textPrimary = Color(0xFFF2F2F3)
    val textSecondary = Color(0xFFFFFFFF).copy(alpha = 0.62f)
    val textMuted = Color(0xFFFFFFFF).copy(alpha = 0.40f)
    val glass = Color(0xFFFFFFFF).copy(alpha = 0.055f)
    val glassStrong = Color(0xFFFFFFFF).copy(alpha = 0.10f)
    val hairline = Color(0xFFFFFFFF).copy(alpha = 0.12f)
}

// ── Motion helpers ───────────────────────────────────────────────────────────────────────────────
/** Subtle entrance: fade + 10dp rise; always settles visible (reduce-motion safe). */
@Composable
fun Modifier.flowCompEnter(delayMillis: Int = 0): Modifier {
    var shown by remember { mutableStateOf(false) }
    val p by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(300, delayMillis, LinearEasing),
        label = "flowCompEnter",
    )
    androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
    return this.graphicsLayer { alpha = p; translationY = (1f - p) * 18f }
}

@Composable
private fun Modifier.flowCompPress(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.94f else 1f, tween(150, easing = LinearEasing), label = "flowCompPress")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

// ── Backdrop ─────────────────────────────────────────────────────────────────────────────────────
/** A soft standalone glow orb — a radial halo used behind the hero ring. */
@Composable
fun FlowGlowOrb(accent: Color, modifier: Modifier = Modifier) {
    Box(modifier.drawBehind {
        drawCircle(
            brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent), center = center, radius = size.minDimension / 2f),
            radius = size.minDimension / 2f,
        )
    })
}

/** A whisper of static grain so flat panels don't band — pure decoration, very low alpha. */
@Composable
fun FlowNoiseOverlay(modifier: Modifier = Modifier) {
    Box(modifier.drawBehind {
        val step = 9f
        var y = 0f
        while (y < size.height) {
            var x = ((y / step).toInt() % 2) * (step / 2f)
            while (x < size.width) {
                drawCircle(Color.White.copy(alpha = 0.012f), radius = 0.6f, center = Offset(x, y))
                x += step
            }
            y += step
        }
    })
}

/**
 * The signature companion atmosphere: a scene-anchored radial light field + two slow drifting glow
 * spots (22s / 27s) + inner vignette, drawn behind [content]. No blur, no asset — cheap and
 * reduce-motion-safe (drift never hides content).
 */
@Composable
fun FlowCompanionBackdrop(scene: FlowCompScene, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "companionAmbient")
    val d1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "d1")
    val d2 by t.animateFloat(1f, 0f, infiniteRepeatable(tween(27000, easing = LinearEasing), RepeatMode.Reverse), label = "d2")
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = scene.backdrop,
                        center = Offset(size.width * scene.anchorX, size.height * scene.anchorY),
                        radius = size.maxDimension * 1.15f,
                    ),
                )
                val g1 = Offset(size.width * (0.18f + 0.24f * d1), size.height * (0.18f + 0.04f * d1))
                val r1 = size.minDimension * 0.85f
                drawCircle(Brush.radialGradient(listOf(scene.accent.copy(alpha = 0.16f), Color.Transparent), center = g1, radius = r1), radius = r1, center = g1)
                val g2 = Offset(size.width * (0.84f - 0.22f * d2), size.height * (0.5f + 0.14f * d2))
                val r2 = size.minDimension * 0.95f
                drawCircle(Brush.radialGradient(listOf(scene.accent.copy(alpha = 0.10f), Color.Transparent), center = g2, radius = r2), radius = r2, center = g2)
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        center = Offset(size.width * 0.5f, size.height * 0.42f),
                        radius = size.maxDimension * 0.8f,
                    ),
                )
            },
    ) { content() }
}

// ── Glass primitive (companion-local) ──────────────────────────────────────────────────────────────
@Composable
private fun flowGlass(shape: RoundedCornerShape, strong: Boolean = false): Modifier =
    Modifier
        .clip(shape)
        .background(if (strong) FlowCompColors.glassStrong else FlowCompColors.glass)
        .border(BorderStroke(1.dp, FlowCompColors.hairline), shape)

// ── Top bar ──────────────────────────────────────────────────────────────────────────────────────
@Composable
fun FlowSessionTopBar(course: String, status: String, accent: Color, modifier: Modifier = Modifier, onBack: () -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(FlowCompColors.glass).clickable(interaction, indication = null) { onBack() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = FlowCompColors.textPrimary, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(8.dp))
            Text(course, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = FlowCompColors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.clip(RoundedCornerShape(50)).background(FlowCompColors.glass).padding(horizontal = 11.dp, vertical = 6.dp)) {
            Text(status, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = FlowCompColors.textSecondary)
        }
    }
}

// ── Breathing timer (hero) ─────────────────────────────────────────────────────────────────────────
/** The hero: a breathing ring (7s scale 1↔1.06 + alpha glow) with a drawArc progress + center labels. */
@Composable
fun FlowBreathingTimer(
    elapsedLabel: String,
    sublabel: String,
    progress: Float,
    running: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val t = rememberInfiniteTransition(label = "breath")
    val pulse by t.animateFloat(
        0.92f, 1.06f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "pulse",
    )
    val glow by t.animateFloat(
        0.18f, 0.34f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "glow",
    )
    val scale = if (running) pulse else 1f
    val glowA = if (running) glow else 0.2f
    Box(modifier.size(232.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            // Glow halo (breathes).
            drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(alpha = glowA), Color.Transparent), center = center, radius = r),
                radius = r * scale,
            )
            val stroke = 10f
            val arcSize = Size(size.width - stroke * 4, size.height - stroke * 4)
            val topLeft = Offset(stroke * 2, stroke * 2)
            // Track.
            drawArc(color = Color.White.copy(alpha = 0.10f), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
            // Progress.
            drawArc(color = accent, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(elapsedLabel, fontSize = 46.sp, fontWeight = FontWeight.Bold, color = FlowCompColors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(sublabel, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = FlowCompColors.textMuted)
        }
    }
}

// ── Control cluster ────────────────────────────────────────────────────────────────────────────────
/** 记灵感(pencil) · pause/resume(large accent) · 添加截图(image). Drawn glyphs — robust, core-icon-free. */
@Composable
fun FlowControlCluster(
    paused: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onJot: () -> Unit,
    onToggle: () -> Unit,
    onShot: () -> Unit,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        FlowCircleButton(size = 52.dp, filled = false, accent = accent, contentDescription = "记一条灵感", onClick = onJot) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = FlowCompColors.textPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(26.dp))
        FlowCircleButton(size = 66.dp, filled = true, accent = accent, contentDescription = if (paused) "继续专注" else "暂停专注", onClick = onToggle) {
            PausePlayGlyph(paused = paused)
        }
        Spacer(Modifier.width(26.dp))
        FlowCircleButton(size = 52.dp, filled = false, accent = accent, contentDescription = "添加截图", onClick = onShot) {
            ImageGlyph()
        }
    }
}

@Composable
private fun FlowCircleButton(
    size: androidx.compose.ui.unit.Dp,
    filled: Boolean,
    accent: Color,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(size)
            .flowCompPress(interaction)
            .clip(CircleShape)
            .background(if (filled) accent else FlowCompColors.glassStrong)
            .border(BorderStroke(1.dp, if (filled) Color.Transparent else FlowCompColors.hairline), CircleShape)
            .clickable(interaction, indication = null, onClickLabel = contentDescription) { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun PausePlayGlyph(paused: Boolean) {
    Canvas(Modifier.size(24.dp)) {
        val c = Color(0xFF2B1F0E)
        if (paused) {
            val w = size.width * 0.30f
            val h = size.height * 0.7f
            val triPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.34f, size.height * 0.22f)
                lineTo(size.width * 0.34f, size.height * 0.78f)
                lineTo(size.width * 0.80f, size.height * 0.5f)
                close()
            }
            drawPath(triPath, c)
        } else {
            val barW = size.width * 0.18f
            val barH = size.height * 0.62f
            val top = (size.height - barH) / 2f
            drawRoundRect(c, topLeft = Offset(size.width * 0.28f - barW / 2, top), size = Size(barW, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
            drawRoundRect(c, topLeft = Offset(size.width * 0.72f - barW / 2, top), size = Size(barW, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
        }
    }
}

@Composable
private fun ImageGlyph() {
    Canvas(Modifier.size(20.dp)) {
        val col = FlowCompColors.textPrimary
        drawRoundRect(color = col, topLeft = Offset(size.width * 0.1f, size.height * 0.18f), size = Size(size.width * 0.8f, size.height * 0.64f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(width = 2.2f))
        drawCircle(col, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.34f, size.height * 0.38f))
        val mtn = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.74f)
            lineTo(size.width * 0.42f, size.height * 0.5f)
            lineTo(size.width * 0.62f, size.height * 0.66f)
            lineTo(size.width * 0.78f, size.height * 0.52f)
            lineTo(size.width * 0.84f, size.height * 0.74f)
        }
        drawPath(mtn, col, style = Stroke(width = 2.2f))
    }
}

// ── Sound scene card + mini player ───────────────────────────────────────────────────────────────
@Composable
fun FlowSoundSceneCard(scene: FlowCompScene, playingLabel: String, modifier: Modifier = Modifier, onOpenPicker: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .then(flowGlass(RoundedCornerShape(18.dp)))
            .flowCompPress(interaction)
            .clickable(interaction, indication = null) { onOpenPicker() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(scene.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(scene.accent))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(scene.name, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = FlowCompColors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(playingLabel, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = FlowCompColors.textSecondary)
        }
        FlowEqualizer(accent = scene.accent)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "选择声音场景", tint = FlowCompColors.textMuted, modifier = Modifier.size(20.dp))
    }
}

/** A tiny animated equalizer used as a local loop playback indicator. */
@Composable
fun FlowEqualizer(accent: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    val bars = (0 until 4).map { i ->
        t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700 + i * 160, easing = LinearEasing), RepeatMode.Reverse), label = "eq$i")
    }
    Row(modifier.height(18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        bars.forEach { a ->
            Box(Modifier.width(3.dp).height((18 * a.value).dp).clip(RoundedCornerShape(2.dp)).background(accent.copy(alpha = 0.8f)))
        }
    }
}

@Composable
fun FlowMiniPlayer(sceneName: String, soundName: String, playing: Boolean, volume: Float, minutes: Int, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier.then(flowGlass(RoundedCornerShape(16.dp))).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        FlowEqualizer(accent = accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${if (playing) "循环播放" else "已暂停"} · $soundName", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = FlowCompColors.textPrimary)
            Text("$sceneName · 音量 ${(volume.coerceIn(0f, 1f) * 100).toInt()}% · 已陪伴 $minutes 分钟", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = FlowCompColors.textMuted)
        }
    }
}

// ── Cached knowledge ───────────────────────────────────────────────────────────────────────────────
@Composable
fun FlowKnowledgeCacheCard(count: Int, accent: Color, modifier: Modifier = Modifier, onView: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .then(flowGlass(RoundedCornerShape(18.dp)))
            .flowCompPress(interaction)
            .clickable(interaction, indication = null) { onView() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Text("$count", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("已缓存 $count 个知识点", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = FlowCompColors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text("查看本次阶段总结", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = FlowCompColors.textSecondary)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = FlowCompColors.textMuted, modifier = Modifier.size(20.dp))
    }
}

// ── Session footer ─────────────────────────────────────────────────────────────────────────────────
@Composable
fun FlowSessionFooter(minutes: Int, accent: Color, modifier: Modifier = Modifier, onEnd: () -> Unit, onBack: () -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Text("保持呼吸 · 你已专注 $minutes 分钟", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = FlowCompColors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowPillButton("结束专注", filled = true, accent = accent, modifier = Modifier.weight(1f), onClick = onEnd)
            FlowPillButton("返回课程", filled = false, accent = accent, modifier = Modifier.weight(1f), onClick = onBack)
        }
    }
}

/** A pill action button — accent-filled (primary) or glass-outline (secondary). */
@Composable
fun FlowPillButton(label: String, filled: Boolean, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .flowCompPress(interaction)
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) accent else Color.Transparent)
            .border(BorderStroke(1.dp, if (filled) Color.Transparent else FlowCompColors.hairline), RoundedCornerShape(16.dp))
            .clickable(interaction, indication = null) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = if (filled) Color(0xFF2B1F0E) else FlowCompColors.textPrimary)
    }
}

// ── Scene picker ───────────────────────────────────────────────────────────────────────────────────
/** A 2-column grid of scene tiles, each with its OWN mini light-field preview + name + selected check. */
@Composable
fun FlowScenePicker(selectedId: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Column(modifier.fillMaxWidth()) {
        flowCompScenes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { scene ->
                    FlowSceneTile(scene = scene, selected = scene.id == selectedId, modifier = Modifier.weight(1f), onClick = { onSelect(scene.id) })
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FlowSceneTile(scene: FlowCompScene, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .aspectRatio(1.35f)
            .flowCompPress(interaction)
            .clip(shape)
            .drawBehind {
                drawRect(Brush.radialGradient(scene.backdrop, center = Offset(size.width * scene.anchorX, size.height * scene.anchorY), radius = size.maxDimension))
                drawCircle(Brush.radialGradient(listOf(scene.accent.copy(alpha = 0.30f), Color.Transparent), center = Offset(size.width * 0.3f, size.height * 0.3f), radius = size.minDimension * 0.7f), radius = size.minDimension * 0.7f, center = Offset(size.width * 0.3f, size.height * 0.3f))
            }
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) scene.accent else FlowCompColors.hairline), shape)
            .clickable(interaction, indication = null) { onClick() }
            .padding(14.dp),
    ) {
        Box(Modifier.align(Alignment.TopStart).size(28.dp).clip(CircleShape).background(scene.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(scene.accent))
        }
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).size(24.dp).clip(CircleShape).background(scene.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = "已选择", tint = Color(0xFF1A140C), modifier = Modifier.size(15.dp))
            }
        }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(scene.name, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = FlowCompColors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(scene.mood, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = FlowCompColors.textSecondary)
        }
    }
}

// ── Section label ──────────────────────────────────────────────────────────────────────────────────
@Composable
fun FlowCompSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(start = 4.dp), style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = FlowCompColors.textMuted, letterSpacing = 1.5.sp)
}

/** A plain glass panel for grouping companion content (mixer rows, summary, jot composer). */
@Composable
fun FlowCompPanel(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier.then(flowGlass(RoundedCornerShape(20.dp), strong = false)).padding(18.dp), content = content)
}
