package com.classmate.app.ui.screens.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.FeatureRow
import com.classmate.app.ui.components.GradientHeroPanel
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SectionHeader
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.design.Dimens

@Composable
fun HomeScreen(viewModel: AppViewModel) {
    ClassMateScaffold(
        title = "ClassMate",
        actions = {
            IconButton(onClick = { viewModel.navigateTo(Screen.SETTINGS) }) {
                Icon(Icons.Filled.Settings, contentDescription = "设置")
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
            GradientHeroPanel {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                ) {
                    Text(
                        "蓝心大模型驱动 · BlueLM-first",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                Spacer(Modifier.height(Dimens.m))
                Text("ClassMate", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Dimens.xs))
                Text(
                    "证据式课堂理解 + 自适应微测复习。把一节课，变成可追溯、可自测、可优化的复习闭环。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }

            ClassMateCard {
                SectionHeader("核心价值", "为什么它不是又一个 AI 总结工具")
                Spacer(Modifier.height(Dimens.l))
                FeatureRow(
                    icon = Icons.Filled.Search,
                    title = "蓝心大模型真实理解课堂",
                    description = "不是逐段复制，而是提炼真正的概念，例如「级数收敛与发散」「p 级数」。",
                )
                Spacer(Modifier.height(Dimens.l))
                FeatureRow(
                    icon = Icons.Filled.Star,
                    title = "每个结论都绑定原文证据",
                    description = "知识点、微测题都能追溯到课堂原文，可信、可核对。",
                    iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                    iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.height(Dimens.l))
                FeatureRow(
                    icon = Icons.Filled.Refresh,
                    title = "微测 → 反馈 → 复习闭环",
                    description = "微测服务学习而非匹配原文；复习计划随你的答题与反馈持续优化。",
                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            ClassMateCard {
                SectionHeader("工作闭环", "八步，从一段课堂文本到个性化复习")
                Spacer(Modifier.height(Dimens.l))
                val steps = listOf(
                    "输入课堂文本",
                    "蓝心大模型理解内容",
                    "提炼真正的知识点",
                    "每个知识点绑定原文证据",
                    "生成有学习价值的微测题",
                    "答题并查看错因与证据",
                    "据此生成复习计划",
                    "你的反馈优化下一轮",
                )
                steps.forEachIndexed { index, label ->
                    NumberedStep(index + 1, label, isLast = index == steps.lastIndex)
                }
            }

            PrimaryButton(
                text = "一键体验示例课（级数）",
                onClick = {
                    viewModel.loadSample()
                    viewModel.startAnalysis()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "导入我的课堂文本",
                onClick = { viewModel.navigateTo(Screen.IMPORT) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NumberedStep(number: Int, label: String, isLast: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                Text(
                    number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(Dimens.m))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
    if (!isLast) Spacer(Modifier.height(Dimens.s))
}
