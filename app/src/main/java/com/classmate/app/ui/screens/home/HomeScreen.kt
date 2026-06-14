package com.classmate.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.state.Tab
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.product.GroupedList
import com.classmate.app.ui.product.PrimaryCommand
import com.classmate.app.ui.product.ProductCanvas
import com.classmate.app.ui.product.ProductHero
import com.classmate.app.ui.product.ProductPill
import com.classmate.app.ui.product.ProductRow
import com.classmate.app.ui.product.ProductScaffold
import com.classmate.app.ui.product.ProductSectionTitle
import com.classmate.app.ui.product.ProductSpace
import com.classmate.app.ui.product.ProviderPathStrip
import com.classmate.app.ui.product.QuietCard
import com.classmate.app.ui.product.StatStrip
import com.classmate.app.ui.i18n.appStrings
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
                    .padding(bottom = 40.dpv()),
                verticalArrangement = Arrangement.spacedBy(ProductSpace.block),
            ) {
                Spacer(Modifier.height(ProductSpace.tight))
                ProductHero(
                    overline = "今天",
                    title = "想把哪节课变清楚？",
                    trailing = { ProductPill(if (onDeviceReady) "端侧蓝心 就绪" else "端侧蓝心 待命") },
                )

                // ONE dominant action.
                if (recent != null) {
                    PrimaryCommand("继续上次学习", recent.title.ifBlank { "未命名课程" }, Icons.Filled.PlayArrow, onClick = { viewModel.openHistory(recent) })
                } else {
                    PrimaryCommand("整理一份新资料", "图片 / 拍照 / 文本，确认后生成知识地图", Icons.Filled.Add, onClick = { viewModel.navigateTo(Screen.IMPORT) })
                }

                StatStrip(
                    items = listOf("$dueCount" to "待复习", "${ui.history.size}" to "课堂记录", "${recent?.knowledgePointCount ?: 0}" to "知识点"),
                    accentFirst = dueCount > 0,
                )

                if (shouldShowGuide) {
                    FirstRunGuideCard(
                        onImport = { viewModel.navigateTo(Screen.IMPORT) },
                        onSample = { viewModel.loadSample(); viewModel.navigateTo(Screen.IMPORT_TRAY) },
                        onDemo = { viewModel.toast("演示流程：导入资料 → 生成时间线 → 查看证据 → 微测与复习 → 导出报告。") },
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
                            "心流学习 · 白噪音陪伴",
                            "声音场景 + 专注计时，沉浸式复习",
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
private fun FirstRunGuideCard(
    onImport: () -> Unit,
    onSample: () -> Unit,
    onDemo: () -> Unit,
    onDismiss: () -> Unit,
) {
    QuietCard {
        Text("三步开始", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(ProductSpace.tight))
        Text("1 · 导入课堂资料", style = MaterialTheme.typography.bodyMedium)
        Text("2 · 生成知识时间线", style = MaterialTheme.typography.bodyMedium)
        Text("3 · 开始复习与练习", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(ProductSpace.block))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dpv())) {
            PrimaryButton("导入资料", onClick = onImport, modifier = Modifier.weight(1f))
            SecondaryButton("加载示例课堂", onClick = onSample, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dpv()))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dpv())) {
            SecondaryButton("查看演示流程", onClick = onDemo, modifier = Modifier.weight(1f))
            SecondaryButton("暂时关闭", onClick = onDismiss, modifier = Modifier.weight(1f))
        }
    }
}

private fun Int.dpv() = androidx.compose.ui.unit.Dp(this.toFloat())
