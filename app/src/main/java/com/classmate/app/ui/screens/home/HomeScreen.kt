package com.classmate.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.state.Tab
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.product.GroupedList
import com.classmate.app.ui.product.PrimaryCommand
import com.classmate.app.ui.product.ProductCanvas
import com.classmate.app.ui.product.ProductPill
import com.classmate.app.ui.product.ProductRow
import com.classmate.app.ui.product.ProductScaffold
import com.classmate.app.ui.product.ProductSectionTitle
import com.classmate.app.ui.product.ProductSpace
import com.classmate.app.ui.product.ProviderPathStrip
import com.classmate.app.ui.product.QuietCard
import com.classmate.app.ui.product.StatStrip
import com.classmate.app.ui.i18n.appStrings
import com.classmate.app.ui.theme.ClassMateTheme
import com.classmate.core.learning.ReviewEngine
import com.classmate.core.ondevice.ProviderPathNode

@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val s = appStrings(ui.language)
    val now = System.currentTimeMillis()
    val dueCount = ReviewEngine.dueCount(ui.learningSnapshot, now)
    val recent = ui.history.firstOrNull()
    val onDeviceReady = ui.onDeviceDiagnostic?.status?.available == true
    var showFirstRunGuide by remember { mutableStateOf(true) }
    val shouldShowGuide = ui.history.isEmpty() && showFirstRunGuide

    ProductCanvas {
        ProductScaffold(contextLabel = s.appName) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ProductSpace.gutter)
                    .padding(bottom = 128.dpv()),
                verticalArrangement = Arrangement.spacedBy(ProductSpace.block),
            ) {
                Spacer(Modifier.height(ProductSpace.tight))
                StudyCockpitCard(
                    overline = if (recent != null) "继续学习" else "今日学习驾驶舱",
                    title = "想把哪节课变清楚？",
                    subtitle = if (recent != null) "上次资料已经整理好，继续从证据、练习和复习接上。" else "导入课堂资料后，ClassMate 会把内容整理成知识地图、证据和下一步学习动作。",
                    providerLabel = if (onDeviceReady) "端侧蓝心 就绪" else "端侧蓝心 待命",
                    actionTitle = if (recent != null) "继续上次学习" else "整理一份新资料",
                    actionSubtitle = if (recent != null) recent.title.ifBlank { "未命名课程" } else "图片 / 拍照 / 文本，确认后生成知识地图",
                    actionIcon = if (recent != null) Icons.Filled.PlayArrow else Icons.Filled.Add,
                    onAction = {
                        if (recent != null) {
                            viewModel.openHistory(recent)
                        } else {
                            viewModel.navigateTo(Screen.IMPORT)
                        }
                    },
                )

                StatStrip(
                    items = listOf("$dueCount" to "待复习", "${ui.history.size}" to "课堂记录", "${recent?.knowledgePointCount ?: 0}" to "知识点"),
                    accentFirst = dueCount > 0,
                )

                if (shouldShowGuide) {
                    FirstRunGuideCard(
                        onImport = { viewModel.navigateTo(Screen.IMPORT) },
                        onSample = { viewModel.loadSample(); viewModel.navigateTo(Screen.IMPORT_TRAY) },
                        onDemo = { viewModel.toast("学习流程：导入资料 → 生成时间线 → 查看证据 → 微测与复习 → 导出报告。") },
                        onDismiss = { showFirstRunGuide = false },
                    )
                }

                // Quick input as grouped-inset rows (not stacked cards).
                ProductSectionTitle("快速输入")
                GroupedList(
                    rows = listOf(
                        ProductRow("图片学习输入", "课件截图 · 端侧多模态草稿", Icons.Filled.Add, MaterialTheme.colorScheme.primary, onClick = { viewModel.navigateTo(Screen.IMPORT) }),
                        ProductRow("拍照学习输入", "板书 / 题目 · 端侧多模态草稿", Icons.Filled.PlayArrow, MaterialTheme.colorScheme.secondary, onClick = { viewModel.navigateTo(Screen.IMPORT) }),
                        ProductRow("粘贴课堂文本", "笔记 / 讲义 / 转写稿", Icons.Filled.Edit, MaterialTheme.colorScheme.tertiary, onClick = { viewModel.navigateTo(Screen.IMPORT_TRAY) }),
                    ),
                )

                // Continue learning.
                ProductSectionTitle("继续学习")
                if (recent != null) {
                    QuietCard(onClick = { viewModel.openHistory(recent) }) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(recent.title.ifBlank { "未命名课程" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Spacer(Modifier.height(4.dpv()))
                                Text("${recent.knowledgePointCount} 个知识点 · ${recent.quizCount} 道微测", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ProductPill(ProviderPathNode.sourceLabelZh(recent.providerName))
                        }
                    }
                }
                GroupedList(
                    rows = listOf(
                        ProductRow(
                            "今日复习计划",
                            if (dueCount > 0) "$dueCount 个任务到期，处理薄弱点" else "今天没有到期任务",
                            Icons.Filled.DateRange,
                            trailing = "$dueCount",
                            onClick = { viewModel.selectTab(Tab.REVIEW) },
                        ),
                        // Restrained entry into the immersive Flow companion (not a global theme).
                        ProductRow(
                            "心流学习 · 背景音陪伴",
                            "内置授权背景音 + 专注计时，沉浸式复习",
                            Icons.Filled.PlayArrow,
                            onClick = { viewModel.navigateTo(Screen.LIVE) },
                        ),
                    ),
                )

                // AI capability — one quiet line.
                Spacer(Modifier.height(ProductSpace.tight))
                ProviderPathStrip(activeIndex = if (onDeviceReady) 1 else 0)
                Text(
                    "云端蓝心 → 端侧蓝心 → 安全占位",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StudyCockpitCard(
    overline: String,
    title: String,
    subtitle: String,
    providerLabel: String,
    actionTitle: String,
    actionSubtitle: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit,
) {
    val colors = ClassMateTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerLow,
        contentColor = colors.textPrimary,
        border = BorderStroke(0.75.dp, colors.outline.copy(alpha = if (colors.isDark) 0.34f else 0.2f)),
        shadowElevation = if (colors.isDark) 0.dp else 5.dp,
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primary.copy(alpha = if (colors.isDark) 0.16f else 0.08f),
                            colors.surfaceContainerLow,
                            ClassMateTheme.extended.evidenceHighlight.copy(alpha = if (colors.isDark) 0.06f else 0.12f),
                        ),
                    ),
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    overline.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ProductPill(providerLabel)
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PrimaryCommand(actionTitle, actionSubtitle, actionIcon, onClick = onAction)
            ProviderPathStrip(activeIndex = if (providerLabel.contains("就绪")) 1 else 0)
        }
    }
}

@Composable
private fun FirstRunGuideCard(
    onImport: () -> Unit,
    onSample: () -> Unit,
    onDemo: () -> Unit,
    onDismiss: () -> Unit,
) {
    QuietCard(padding = 16.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("三步开始", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("像路径一样推进，不需要先学会工具。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ProductPill("学习路径")
        }
        Spacer(Modifier.height(ProductSpace.block))
        Column(verticalArrangement = Arrangement.spacedBy(8.dpv())) {
            LearningPathStep("1", "导入课堂资料", "图片、拍照、文本或转写稿先进入草稿确认。", isLast = false)
            LearningPathStep("2", "生成知识时间线", "重点、证据和来源整理成可追问结构。", isLast = false)
            LearningPathStep("3", "开始复习与练习", "用微测发现薄弱点，再生成复习动作。", isLast = true)
        }
        Spacer(Modifier.height(ProductSpace.block))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dpv())) {
            PrimaryButton("导入资料", onClick = onImport, modifier = Modifier.weight(1f))
            SecondaryButton("加载示例课堂", onClick = onSample, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dpv()))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dpv())) {
            SecondaryButton("查看学习流程", onClick = onDemo, modifier = Modifier.weight(1f))
            SecondaryButton("暂时关闭", onClick = onDismiss, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LearningPathStep(index: String, title: String, caption: String, isLast: Boolean) {
    val colors = ClassMateTheme.colors
    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 58.dp), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = if (colors.isDark) 0.22f else 0.11f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(index, style = MaterialTheme.typography.labelLarge, color = colors.primary, fontWeight = FontWeight.Bold)
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f)),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(top = 1.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun Int.dpv() = androidx.compose.ui.unit.Dp(this.toFloat())
