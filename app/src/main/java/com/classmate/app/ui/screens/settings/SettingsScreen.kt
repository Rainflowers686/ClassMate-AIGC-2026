package com.classmate.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.BuildConfig
import com.classmate.app.platform.ConfigImportPreview
import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.app.platform.ProviderSummary
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.theme.ClassMateTheme
import com.classmate.app.ui.theme.ThemeOption

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui

    ClassMateScaffold(title = "设置", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            // --- Theme ---
            ClassMateCard {
                Text("主题风格", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Text("默认「专注」，可切换到「活力」或「心流」。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.m))
                ThemeOption.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { viewModel.setTheme(option) }.padding(vertical = Dimens.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = ui.theme == option, onClick = { viewModel.setTheme(option) })
                        Spacer(Modifier.width(Dimens.s))
                        Column(Modifier.weight(1f)) {
                            Text(option.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(option.tagline, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(Dimens.m))
                Text("外观", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    SelectableChip("跟随系统", ui.darkMode == null) { viewModel.setDarkMode(null) }
                    SelectableChip("浅色", ui.darkMode == false) { viewModel.setDarkMode(false) }
                    SelectableChip("深色", ui.darkMode == true) { viewModel.setDarkMode(true) }
                }
            }

            // --- Model config ---
            ClassMateCard {
                val providerSummary = ui.providerConfigSummary
                Text("模型配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Text(
                    "主路径是 vivo 蓝心大模型（BlueLM）。调用顺序：BlueLM → 兼容备用 → 本地兜底。" +
                        "真实协议/签名完成前，即使导入凭据也会安全兜底；示例课程展示的是人工编写的演示数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.m))
                ProviderStatusRow("蓝心大模型 BlueLM", providerSummary.blueLmStatus())
                ProviderStatusRow("兼容备用 Compatible", "备用 · 未启用")
                ProviderStatusRow("本地兜底 Local", if (providerSummary.localFallbackEnabled) "始终可用 · 仅兜底" else "已关闭")
            }

            // --- Debug-only config import ---
            if (BuildConfig.DEBUG) {
                DebugImportCard(viewModel)
            }

            // --- Privacy & security ---
            ClassMateCard {
                Text("隐私与安全", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                listOf(
                    "真实 AppID / AppKEY 绝不写入仓库、README、日志或截图。",
                    "真实密钥只通过本地 config.local.json（已 gitignore）或本页 Debug 导入注入。",
                    "日志只记录 provider / 状态 / 延迟 / 校验结果 / 是否兜底 / 短错误码。",
                    "不记录密钥、Authorization、prompt、课程全文或厂商返回体。",
                ).forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Dimens.xxs))
                }
            }

            // --- Redacted logs ---
            ClassMateCard {
                Text("最近一次分析日志（已脱敏）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                if (ui.logs.isEmpty()) {
                    Text("还没有日志。先分析一节课。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ui.logs.forEach { entry ->
                        Text(
                            entry.format(),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Dimens.xxs))
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) cs.primaryContainer else cs.surfaceVariant,
        contentColor = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
private fun ProviderStatusRow(name: String, status: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = Dimens.xxs), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DebugImportCard(viewModel: AppViewModel) {
    var input by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ConfigImportPreview?>(null) }

    ClassMateCard {
        Text("Debug 配置导入（仅调试版）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text(
            "这是注入真实 BlueLM 凭据的本地入口规划。当前仅做安全检查，不持久化、不上传、不回显密钥值。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.m))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("粘贴 config JSON") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = "安全检查",
            onClick = { preview = viewModel.importDebugProviderConfig(input) },
            modifier = Modifier.fillMaxWidth(),
        )
        preview?.let { p ->
            Spacer(Modifier.height(Dimens.m))
            val color = if (p.containsRealSecret) ClassMateTheme.extended.warning else ClassMateTheme.extended.success
            Text(p.message, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Medium)
            if (p.providersFound.isNotEmpty()) {
                Spacer(Modifier.height(Dimens.xxs))
                Text("识别到的 provider：${p.providersFound.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("BlueLM 已配置：${if (p.bluelmConfigured) "是" else "否"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            p.providerSummaries.forEach { summary ->
                ProviderPreviewRow(summary)
            }
        }
    }
}

@Composable
private fun ProviderPreviewRow(summary: ProviderSummary) {
    Spacer(Modifier.height(Dimens.xxs))
    Text(
        "${summary.provider}: baseUrl=${summary.baseUrl.ifBlank { "未设置" }}, model=${summary.model.ifBlank { "未设置" }}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "credential_present=${summary.credentialPresent}, appId=${summary.maskedAppId}, appKey=${summary.maskedAppKey}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun ProviderConfigSummary.blueLmStatus(): String =
    when {
        primaryReady -> "主路径 · 可用"
        blueLmConfigured -> "凭据已导入 · 协议/签名待实现 · 当前安全兜底"
        else -> "主路径 · 未配置密钥 · 当前本地兜底"
    }
