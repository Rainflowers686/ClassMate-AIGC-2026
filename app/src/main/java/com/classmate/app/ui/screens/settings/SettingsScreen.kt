package com.classmate.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.classmate.app.BuildConfig
import com.classmate.app.BuildInfo
import com.classmate.app.ondevice.BitmapToRgb
import com.classmate.app.ondevice.OnDevicePermissions
import com.classmate.app.platform.ConfigImportPreview
import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.app.platform.ProviderSummary
import com.classmate.app.state.AnalysisSourceReport
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.ClassMateUiState
import com.classmate.core.ondevice.OnDeviceErrorExplain
import com.classmate.core.ondevice.OnDeviceLlmConfig
import com.classmate.core.provider.BlueLMDiagnosticStatus
import com.classmate.app.ui.components.CapabilityStatusPill
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.DiagnosticDetailsCard
import com.classmate.app.ui.components.PageHero
import com.classmate.app.ui.product.ProductCanvas
import com.classmate.app.ui.product.ProductHero
import com.classmate.app.ui.product.ProductScaffold
import com.classmate.app.ui.product.ProductSpace
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.components.ThemePreviewCard
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.theme.ClassMateTheme
import com.classmate.app.ui.theme.ThemeOption
import com.classmate.app.ui.i18n.AppLanguage
import com.classmate.app.ui.i18n.appStrings

private enum class SettingsSection(val title: String, val subtitle: String) {
    HOME("设置首页", "常用入口与状态概览"),
    THEME("主题设置", "Focus / Flow / Vitality 与授权背景音说明"),
    MODEL("模型接入", "云端、端侧、OCR、ASR 与路由状态"),
    LEARNING_EXPORT("学习与导出", "学习资料处理偏好与导出默认项"),
    DEVELOPER("开发者选项", "诊断、smoke 与脱敏日志"),
}

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val s = appStrings(ui.language)
    var showDebug by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf(SettingsSection.HOME) }

    ProductCanvas {
      ProductScaffold(contextLabel = s.settingsTitle) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ProductSpace.gutter)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            Spacer(Modifier.height(ProductSpace.tight))
            ProductHero(
                overline = "能力中心",
                title = "模型与诊断",
                subtitle = "云端蓝心 → 端侧蓝心 → 安全占位。在这里管理官方模型路径、端侧诊断与权限；开发详情默认折叠。",
            )
            CapabilityOverviewRow(viewModel)
            Spacer(Modifier.height(Dimens.xxs))
            SettingsSectionNav(section = section, onSelect = { section = it })

            if (section == SettingsSection.HOME) {
                SettingsLandingCard(
                    onTheme = { section = SettingsSection.THEME },
                    onModel = { section = SettingsSection.MODEL },
                    onLearning = { section = SettingsSection.LEARNING_EXPORT },
                    onDeveloper = { section = SettingsSection.DEVELOPER },
                )
            }

            if (section == SettingsSection.MODEL) {
            ModelApiManagementCard(viewModel)
            PermissionCenterCard(viewModel)
            OnDeviceDiagnosticCard(viewModel)
            OnDeviceMultimodalDiagnosticCard(viewModel)
            OnDeviceCapabilityCard(viewModel)
            ModelAccessNotesCard()
            }

            if (section == SettingsSection.THEME) {
            ClassMateCard {
                Text(s.settingsLanguage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xxs))
                Text(s.settingsLanguageDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.s))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    AppLanguage.entries.forEach { lang ->
                        SelectableChip(lang.displayName, ui.language == lang) { viewModel.setLanguage(lang) }
                    }
                }
            }

            PrivacyCard()
            SpeakerCapabilityCard()
            CapabilityRoadmapCard()

            ClassMateCard {
                Text("主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xxs))
                Text(
                    "默认 Focus 专注主题。Flow 心流仅用于课堂伴学 / 专注计时等沉浸场景，Vitality 活力为预留增强主题。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
                ThemeOption.entries.forEach { option ->
                    ThemePreviewCard(
                        name = option.displayName,
                        tagline = option.tagline,
                        description = option.description,
                        swatches = themeSwatches(option),
                        selected = ui.theme == option,
                        onClick = { viewModel.setTheme(option) },
                        modifier = Modifier.padding(top = Dimens.xs),
                    )
                }
                Spacer(Modifier.height(Dimens.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    SelectableChip("跟随系统", ui.darkMode == null) { viewModel.setDarkMode(null) }
                    SelectableChip("浅色", ui.darkMode == false) { viewModel.setDarkMode(false) }
                    SelectableChip("深色", ui.darkMode == true) { viewModel.setDarkMode(true) }
                }
            }
            BackgroundAudioPolicyCard()
            }

            if (section == SettingsSection.LEARNING_EXPORT) {
                LearningExportSettingsCard(viewModel)
                PrivacyCard()
                SpeakerCapabilityCard()
                CapabilityRoadmapCard()
            }

            if (section == SettingsSection.DEVELOPER) {
            if (BuildConfig.DEBUG) {
                ClassMateCard {
                    Text("调试区", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.s))
                    Text("仅 debug build 可见。默认折叠，不在设置首页展开大段导入内容。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Dimens.s))
                    SecondaryButton(
                        text = if (showDebug) "收起 Debug 导入" else "展开 Debug 导入",
                        onClick = { showDebug = !showDebug },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showDebug) DebugImportCard(viewModel)
            }

            ClassMateCard {
                Text("日志", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Text("仅显示脱敏后的短状态日志；默认折叠。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = if (showLogs) "收起日志" else "展开最近日志",
                    onClick = { showLogs = !showLogs },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showLogs) LogsCard(viewModel)
            BuildInfoCard()
            }
        }
      }
    }
}

/**
 * The competition main config page: official 蓝心大模型 first, then the on-device + local-rule
 * fallbacks. It intentionally surfaces ONLY the official cloud model and the on-device/local
 * fallbacks — no third-party or external-model enhancement. The neutral custom-API affordance stays
 * in the debug-only, collapsed section below.
 */
@Composable
private fun SettingsSectionNav(section: SettingsSection, onSelect: (SettingsSection) -> Unit) {
    ClassMateCard {
        Text("设置分组", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            SettingsSection.entries.forEach { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(item) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (item == section) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(Modifier.padding(Dimens.s)) {
                        Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsLandingCard(
    onTheme: () -> Unit,
    onModel: () -> Unit,
    onLearning: () -> Unit,
    onDeveloper: () -> Unit,
) {
    ClassMateCard {
        Text("四个设置入口", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        SettingsLandingRow("主题设置", "主题、明暗模式、授权循环背景音说明", onTheme)
        SettingsLandingRow("模型接入", "云端优先、端侧兜底、OCR/ASR 状态", onModel)
        SettingsLandingRow("学习与导出", "学习资料处理偏好和默认导出结构", onLearning)
        SettingsLandingRow("开发者选项", "诊断、smoke、脱敏日志，默认折叠", onDeveloper)
    }
}

@Composable
private fun SettingsLandingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.xxs)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(Dimens.s)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModelAccessNotesCard() {
    ClassMateCard {
        Text("AI 路由说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text("云端优先，端侧兜底，用户确认后再写入学习资料。官方 OCR / ASR 未配置时，仍可继续编辑草稿或粘贴转写文本。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.xs))
        ProviderStatusRow("TTS / 音频生成", "未配置时导出课程精华脚本文本")
        ProviderStatusRow("翻译辅助学习", "保留 derived note，不修改原始证据")
        ProviderStatusRow("端侧文本安全审核", "不可用时提示，不阻断核心学习")
        Spacer(Modifier.height(Dimens.xs))
        Text("这里仅显示配置是否可用，不显示任何密钥内容。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BackgroundAudioPolicyCard() {
    ClassMateCard {
        Text("沉浸背景音", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text("后续使用授权明确的循环音频素材，不使用实时音频生成，也不模拟老师或同学声音。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LearningExportSettingsCard(viewModel: AppViewModel) {
    val audio = viewModel.ui.courseEssenceAudioResult
    val safety = viewModel.ui.textSafetyResult
    ClassMateCard {
        Text("学习与导出", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text("默认保留用户确认、证据校验和脱敏导出。练习、复习、报告和课程精华脚本都来自本节课证据。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("默认练习题数量", "8 题，可按课堂资料不足自动减少")
        ProviderStatusRow("默认复习优先级", "错题、薄弱标记、久未复习、证据复核优先")
        ProviderStatusRow("默认导出格式", "Markdown/Text、HTML、PDF、音频脚本")
        ProviderStatusRow("文本安全检查", safety?.status?.name ?: "未运行")
        ProviderStatusRow("课程精华音频", audio?.status?.name ?: "可生成脚本文本")
        ProviderStatusRow("翻译辅助学习", "面向 evidence / knowledge point 的双语注记")
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = "生成课程精华音频脚本",
            onClick = { viewModel.generateCourseEssenceAudioScript() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ModelApiManagementCard(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val summary = ui.providerConfigSummary
    val masked = ui.modelConfigMasked
    var editing by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var appId by remember { mutableStateOf("") }
    var appKey by remember { mutableStateOf("") }

    val activeModel = masked?.model
        ?: summary.providers.firstOrNull { it.provider == "BLUELM" }?.model?.takeIf { it.isNotBlank() }
        ?: "qwen3.5-plus"

    ClassMateCard {
        Text("模型 API 管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text("当前模型：蓝心大模型 · $activeModel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(Dimens.xxs))
        Text(
            "主路径：云端蓝心（官方蓝心大模型 API） → 端侧蓝心（端侧 BlueLM 3B 本地智能兜底） → 安全占位（模型不可用保护）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.m))
        ProviderStatusRow("云端蓝心", cloudBlueLmStatus(ui))
        ProviderStatusRow("端侧蓝心（本地智能兜底）", onDeviceShortStatus(ui))
        ProviderStatusRow("安全占位", if (summary.localFallbackEnabled) "就绪（仅模型全部不可用时）" else "已停用")
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "Provider path：${providerPathText(summary, ui)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (masked != null && masked.credentialPresent) {
            Spacer(Modifier.height(Dimens.xxs))
            Text(
                "已保存配置：appId=${masked.maskedAppId} · appKey=${masked.maskedAppKey}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Dimens.m))
        SecondaryButton(
            text = if (editing) "收起官方模型配置" else "编辑官方模型配置",
            onClick = { editing = !editing },
            modifier = Modifier.fillMaxWidth(),
        )
        if (editing) {
            Spacer(Modifier.height(Dimens.s))
            OutlinedTextField(
                value = baseUrl, onValueChange = { baseUrl = it },
                label = { Text("Base URL（默认 vivo 官方）") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Dimens.s))
            OutlinedTextField(
                value = model, onValueChange = { model = it },
                label = { Text("模型（默认 qwen3.5-plus）") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Dimens.s))
            OutlinedTextField(
                value = appId, onValueChange = { appId = it },
                label = { Text("AppID") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Dimens.s))
            OutlinedTextField(
                value = appKey, onValueChange = { appKey = it },
                label = { Text("AppKEY") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Dimens.s))
            PrimaryButton(
                text = "保存配置",
                onClick = {
                    viewModel.saveOfficialModelConfig(baseUrl, model, appId, appKey)
                    appId = ""; appKey = "" // never keep secrets in UI state after save
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.s))
            PrimaryButton(
                text = if (ui.blueLmDiagnosticRunning) "测试中…" else "测试连接",
                onClick = { viewModel.testOfficialModelConnection() },
                enabled = !ui.blueLmDiagnosticRunning,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.s))
            SecondaryButton(
                text = "删除配置",
                onClick = { viewModel.deleteOfficialModelConfig() },
                modifier = Modifier.fillMaxWidth(),
            )
            ui.blueLmDiagnostic?.let { report ->
                Spacer(Modifier.height(Dimens.m))
                Text("连接诊断", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                DiagnosticDetailsCard(lines = report.safeLines())
            }
            Spacer(Modifier.height(Dimens.s))
            Text(
                "配置仅保存在本机应用私有存储；AppID/AppKEY 不写入仓库、日志、导出或截图。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun launchAllFilesAccess(context: android.content.Context, perms: OnDevicePermissions): Boolean {
    for (intent in perms.allFilesAccessIntents()) {
        if (runCatching { context.startActivity(intent) }.isSuccess) return true
    }
    return false
}

/** 权限与能力诊断 (Task 2). Functional permissions for the on-device model + learning-material import. */
@Composable
private fun PermissionCenterCard(viewModel: AppViewModel) {
    val context = LocalContext.current
    val perms = remember { OnDevicePermissions(context) }
    val snap = viewModel.ui.onDevicePermissions

    LaunchedEffect(Unit) { viewModel.refreshOnDevicePermissions(perms.snapshot()) }

    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshOnDevicePermissions(perms.snapshot()) }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshOnDevicePermissions(perms.snapshot()) }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshOnDevicePermissions(perms.snapshot()) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshOnDevicePermissions(perms.snapshot()) }
    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshOnDevicePermissions(perms.snapshot()) }

    ClassMateCard {
        Text("权限与能力诊断", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))

        // 1) On-device model directory access (all files).
        PermissionStatusRow("模型目录访问（所有文件）", snap.allFilesAccess)
        Text(
            "端侧 BlueLM 3B 模型由官方云真机预置在 /sdcard/1225。Android 13+ 可能需要授予模型目录访问权限，否则 SDK 可发现但模型初始化失败。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "授予模型目录访问权限",
            onClick = {
                if (!launchAllFilesAccess(context, perms)) viewModel.toast("无法打开权限设置页，请在系统设置中手动授予所有文件访问。")
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.m))
        // 2) Learning-material media import (Android 13+ READ_MEDIA_*).
        PermissionStatusRow("图片权限", snap.mediaImages)
        PermissionStatusRow("视频权限", snap.mediaVideo)
        PermissionStatusRow("音频权限", snap.mediaAudio)
        Text(
            "用于导入课件截图、板书照片、题目图片、课程视频/字幕来源、课堂录音/音频转写来源。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "申请媒体导入权限",
            onClick = { mediaLauncher.launch(perms.mediaRequestPermissions()) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.m))
        // 3) Camera (capture study material).
        PermissionStatusRow("相机", snap.camera)
        Text(
            "用于当场拍摄课件、板书、题目、纸质资料，后续进入端侧多模态 / OCR / 资料篮学习链路（拍照主链路逐步接入）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "申请相机权限",
            onClick = { cameraLauncher.launch(perms.cameraPermission()) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.m))
        // 4) Microphone (live ASR experiment).
        PermissionStatusRow("麦克风", snap.recordAudio)
        Text("用于实时转写实验模式（系统语音识别）。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "申请麦克风权限",
            onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.m))
        // 5) Bluetooth audio devices (paired headset / mic) + audio routing.
        PermissionStatusRow("蓝牙音频设备", snap.bluetoothConnect)
        Text(
            "用于与已配对蓝牙耳机 / 蓝牙麦克风交互：蓝牙麦克风课堂录音输入、蓝牙耳机播报设备状态诊断、音频路由优化（MODIFY_AUDIO_SETTINGS）。不扫描附近设备。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "申请蓝牙音频权限",
            onClick = {
                val perm = perms.bluetoothRequestPermission()
                if (perm != null) bluetoothLauncher.launch(perm) else viewModel.toast("当前系统蓝牙权限在安装时授予。")
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.m))
        // 6) Review notifications (Android 13+).
        PermissionStatusRow("通知", snap.postNotifications)
        Text("用于复习提醒、学习任务提醒（功能逐步接入，未实现前不会推送）。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "申请通知权限",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.toast("当前系统通知由系统设置管理。")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.m))
        Text(
            "导入/导出说明：现代 Android 使用系统文件选择器与 MediaStore（下载/分享）；旧系统保留读写存储兼容；端侧模型目录读取走所有文件访问权限。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = "刷新权限状态",
            onClick = { viewModel.refreshOnDevicePermissions(perms.snapshot()) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 端侧 BlueLM 3B readiness + gated real text self-test (P3/P5/Task 5). Honest unavailable when SDK absent. */
@Composable
private fun OnDeviceDiagnosticCard(viewModel: AppViewModel) {
    val context = LocalContext.current
    val perms = remember { OnDevicePermissions(context) }
    val ui = viewModel.ui
    val diag = ui.onDeviceDiagnostic
    val files = ui.onDeviceModelFiles
    var pathInput by remember(ui.onDeviceModelPath) { mutableStateOf(ui.onDeviceModelPath) }
    ClassMateCard {
        Text("端侧模型诊断", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text(
            "端侧 BlueLM 3B 是真正的本地智能兜底；端侧也不可用时仅降级到安全占位（防止空结果或崩溃），不影响主流程。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("SDK", if (diag?.sdkPresent == true) "已发现" else "未接入（等待 llm-sdk-release.aar）")
        ProviderStatusRow("状态", diag?.status?.displayZh ?: "未知")
        PermissionStatusRow("模型目录访问", ui.onDevicePermissions.allFilesAccess)
        diag?.initState?.let { ProviderStatusRow("初始化", it.displayZh) }
        diag?.generateState?.let { ProviderStatusRow("文本生成", it.displayZh) }
        diag?.errorCode?.let { ProviderStatusRow("错误码", it) }
        ProviderStatusRow("安全占位", if (diag?.fallbackAvailable != false) "就绪（模型全部不可用时）" else "不可用")

        // Bounded model-file diagnostic (Task 3): exists/readable only — never reads content.
        files?.let { f ->
            Spacer(Modifier.height(Dimens.s))
            Text("模型目录文件诊断", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            ProviderStatusRow("模型目录", if (f.modelDirExists) (if (f.modelDirReadable) "存在·可读" else "存在·不可读") else "不存在")
            ProviderStatusRow("tokenizer", if (f.tokenizerExists) (if (f.tokenizerReadable) "存在·可读" else "存在·不可读") else "不存在")
            ProviderStatusRow("config", if (f.configExists) (if (f.configReadable) "存在·可读" else "存在·不可读") else "不存在")
        }

        // Honest, actionable error explanation (Task 4) — never a raw stack.
        OnDeviceErrorExplain.explain(diag?.errorCode)?.let { explain ->
            Spacer(Modifier.height(Dimens.s))
            Text(explain, style = MaterialTheme.typography.bodySmall, color = ClassMateTheme.extended.warning, fontWeight = FontWeight.Medium)
            if (!ui.onDevicePermissions.allFilesAccess) {
                Spacer(Modifier.height(Dimens.xs))
                SecondaryButton(
                    text = "授予模型目录访问权限",
                    onClick = { if (!launchAllFilesAccess(context, perms)) viewModel.toast("无法打开权限设置页。") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(Dimens.m))
        OutlinedTextField(
            value = pathInput,
            onValueChange = { pathInput = it },
            label = { Text("端侧模型路径") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "官方推荐路径（2026-06-11 文档）：${OnDeviceLlmConfig.DEFAULT_MODEL_DIR}；兼容旧目录 ${OnDeviceLlmConfig.LEGACY_MODEL_DIR}。" +
                "候选检测只查固定路径的第一层文件名，不扫描全盘、不读取文件内容。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = "更新模型路径",
            onClick = { viewModel.setOnDeviceModelPath(pathInput) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = "检测候选模型目录",
            onClick = { viewModel.detectOnDeviceModelPath() },
            modifier = Modifier.fillMaxWidth(),
        )
        ui.modelPathDetection?.let { detection ->
            Spacer(Modifier.height(Dimens.s))
            ProviderStatusRow(
                "当前路径完整性",
                when {
                    detection.current.multimodalComplete -> "完整（文本 + 多模态）"
                    detection.current.textComplete -> "文本完整 · 缺多模态 VIT 文件"
                    else -> "不完整"
                },
            )
            detection.candidates.forEach { c ->
                ProviderStatusRow(
                    c.path,
                    when {
                        c.multimodalComplete -> "完整"
                        c.textComplete -> "文本完整"
                        c.exists -> "存在 · 不完整"
                        else -> "不存在"
                    },
                )
            }
            detection.recommended?.let { rec ->
                Spacer(Modifier.height(Dimens.s))
                Text(
                    "检测到官方模型目录：${rec.path}，建议切换后重试。",
                    style = MaterialTheme.typography.bodySmall,
                    color = ClassMateTheme.extended.warning,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(Dimens.xs))
                PrimaryButton(
                    text = "切换到推荐路径",
                    onClick = { viewModel.setOnDeviceModelPath(rec.path) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = if (ui.onDeviceDiagnosticRunning) "诊断中…" else "测试端侧模型（文本）",
            onClick = { viewModel.testOnDeviceModel(perms.snapshot()) },
            enabled = !ui.onDeviceDiagnosticRunning,
            modifier = Modifier.fillMaxWidth(),
        )
        diag?.outputPreview?.let { preview ->
            Spacer(Modifier.height(Dimens.s))
            Text("生成预览（前 80 字符）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        diag?.let { report ->
            Spacer(Modifier.height(Dimens.m))
            DiagnosticDetailsCard(lines = report.safeLines())
        }
    }
}

/** 端侧多模态诊断（实验）— P4/Task 6/8E P0-4. Gated VIT round-trip; never the course pipeline, never crashes. */
@Composable
private fun OnDeviceMultimodalDiagnosticCard(viewModel: AppViewModel) {
    val context = LocalContext.current
    val perms = remember { OnDevicePermissions(context) }
    val ui = viewModel.ui
    val mm = ui.onDeviceMultimodalDiagnostic
    // Stage 8E P0-4: real-image diagnostic via the system Photo Picker (no extra permission). The
    // image is decoded with a software allocator, bounded-scaled, RGB-converted, then callVit+generate.
    val realImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bitmap = decodeSoftwareBitmap(context, uri)
            if (bitmap == null) {
                viewModel.toast("无法读取该图片。")
            } else {
                viewModel.testOnDeviceMultimodalWithImage(
                    image = BitmapToRgb.toRgbScaled(bitmap),
                    originalWidth = bitmap.width,
                    originalHeight = bitmap.height,
                    snapshot = perms.snapshot(),
                )
            }
        }
    }
    ClassMateCard {
        Text("端侧多模态诊断（实验）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text(
            "VIT 图像编码自检：内置 2x2 测试图验证 callVit 通路；也可选择真实图片做端侧多模态诊断（仅诊断，不落库、不进入课程分析、不替代 OCR）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        Text(
            "需先授予模型目录访问权限，并通过文本模型初始化成功后才会调用端侧多模态；未满足前不会触碰原生 init/callVit。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("多模态支持", if (mm?.sdkSupportsMultimodalField == true) "是" else "否")
        ProviderStatusRow("callVit 方法", if (mm?.callVitMethodPresent == true) "存在" else "不存在")
        ProviderStatusRow("模型路径", mm?.modelDir ?: ui.onDeviceModelPath)
        if (mm != null) {
            ProviderStatusRow("测试图", "${mm.testImageWidth}x${mm.testImageHeight} · RGB ${mm.rgbByteLength} 字节")
            ProviderStatusRow("VIT 状态", mm.state.displayZh)
            mm.callVitReturnCode?.let { ProviderStatusRow("callVit 返回码", it.toString()) }
            ProviderStatusRow("多模态生成", mm.generateState.displayZh)
            mm.errorCode?.let { ProviderStatusRow("错误码", it) }
        }
        OnDeviceErrorExplain.explain(mm?.errorCode)?.let { explain ->
            Spacer(Modifier.height(Dimens.s))
            Text(explain, style = MaterialTheme.typography.bodySmall, color = ClassMateTheme.extended.warning, fontWeight = FontWeight.Medium)
        }
        ui.onDeviceRealImageMeta?.let { ProviderStatusRow("真实图片", it) }
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = if (ui.onDeviceMultimodalRunning) "诊断中…" else "测试端侧多模态（内置 2x2）",
            onClick = { viewModel.testOnDeviceMultimodal(perms.snapshot()) },
            enabled = !ui.onDeviceMultimodalRunning,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = if (ui.onDeviceMultimodalRunning) "诊断中…" else "选择真实图片测试（不落库）",
            onClick = {
                realImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        mm?.outputPreview?.let { preview ->
            Spacer(Modifier.height(Dimens.s))
            Text("生成预览（前 80 字符）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        mm?.let { report ->
            Spacer(Modifier.height(Dimens.m))
            DiagnosticDetailsCard(lines = report.safeLines())
        }
    }
}

/** Stage 9B: the capability-center overview — four quiet status pills, the first thing in Settings. */
@Composable
private fun CapabilityOverviewRow(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val cloudReady = ui.providerConfigSummary.blueLmConfigured
    val onDeviceReady = ui.onDeviceDiagnostic?.status?.available == true
    val multimodalReady = ui.onDeviceMultimodalDiagnostic?.callVitMethodPresent == true
    val permsReady = ui.onDevicePermissions.allFilesAccess
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
        CapabilityStatusPill("云端蓝心", cloudReady, Modifier.weight(1f))
        CapabilityStatusPill("端侧蓝心", onDeviceReady, Modifier.weight(1f))
    }
    Spacer(Modifier.height(Dimens.s))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
        CapabilityStatusPill("多模态", multimodalReady, Modifier.weight(1f))
        CapabilityStatusPill("模型目录权限", permsReady, Modifier.weight(1f))
    }
}

/** Phase 7: shows the on-device model as a cross-app local AI layer + an offline-mode analysis check. */
@Composable
private fun OnDeviceCapabilityCard(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val onDeviceReady = ui.onDeviceDiagnostic?.status?.available == true
    val capStatus = if (onDeviceReady) "已接入端侧蓝心" else "待端侧就绪（先在上方完成端侧初始化）"
    ClassMateCard {
        Text("端侧本地智能层", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text(
            "端侧 BlueLM 3B 已贯穿核心学习闭环：云端不可用时自动接管。路径：云端蓝心 → 端侧蓝心 → 安全占位。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        listOf("课程分析", "Ask 问答", "练习解释", "复习建议", "报告/导出建议", "图片学习草稿", "文本生成", "多模态理解").forEach {
            ProviderStatusRow(it, capStatus)
        }

        Spacer(Modifier.height(Dimens.m))
        Text("端侧独立模式检查", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ProviderStatusRow("云端配置", if (ui.providerConfigSummary.blueLmConfigured) "已配置" else "未配置（将由端侧接管）")
        ui.onDeviceAnalysisReason?.let {
            ProviderStatusRow("最近端侧课程分析", "$it · ${AnalysisSourceReport.onDeviceReasonZh(it)}")
        }
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = if (ui.onDeviceAnalysisCheckRunning) "检查中…" else "测试端侧课程分析（无云端）",
            onClick = { viewModel.runOfflineOnDeviceAnalysisCheck() },
            enabled = !ui.onDeviceAnalysisCheckRunning,
            modifier = Modifier.fillMaxWidth(),
        )
        // Stage 8D-2: the detailed, content-free breakdown of the last on-device analysis attempt.
        ui.onDeviceAnalysisDiagnostic?.let { diag ->
            Spacer(Modifier.height(Dimens.m))
            DiagnosticDetailsCard(lines = diag.safeLines())
        }
        Spacer(Modifier.height(Dimens.s))
        Text(
            "端侧结构化分析必须通过校验才落库；不通过仅显示安全占位，绝不生成假知识点。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionStatusRow(name: String, granted: Boolean) {
    ProviderStatusRow(name, if (granted) "已授予" else "未授予")
}

private fun cloudBlueLmStatus(ui: ClassMateUiState): String {
    val base = if (ui.providerConfigSummary.blueLmConfigured) "已配置" else "未配置"
    return when (ui.blueLmDiagnostic?.status) {
        BlueLMDiagnosticStatus.OK -> "$base · 连接正常"
        BlueLMDiagnosticStatus.FAIL -> "$base · 连接失败"
        else -> base
    }
}

private fun onDeviceShortStatus(ui: ClassMateUiState): String =
    ui.onDeviceDiagnostic?.status?.displayZh ?: "未知"

private fun providerPathText(summary: ProviderConfigSummary, ui: ClassMateUiState): String {
    val nodes = buildList {
        if (summary.blueLmConfigured) add("云端蓝心")
        addAll(ui.localProviderPath) // honest local chain (Chinese): [端侧蓝心, 安全占位] or [安全占位]
    }
    return nodes.joinToString(" → ").ifBlank { "安全占位" }
}

@Composable
private fun SpeakerCapabilityCard() {
    ClassMateCard {
        Text("说话人与音频能力", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        listOf(
            "实时转写实验" to "使用系统语音识别，需要麦克风权限；不保存原始音频、不后台录音",
            "说话人标签" to "当前支持手动标注（教师 / 学生 / 未知）",
            "自动说话人分段" to "依赖 ASR provider，规划中",
            "声纹身份识别" to "暂缓，不作为复赛核心能力",
            "底噪处理 / 人声增强" to "优先依赖系统与 ASR provider，不自研底层音频算法",
            "音视频本体解析" to "当前不解析、不上传；仅记录文件元数据",
        ).forEach { (name, status) ->
            ProviderStatusRow(name, status)
        }
    }
}

@Composable
private fun CapabilityRoadmapCard() {
    ClassMateCard {
        Text("能力路线图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        RoadmapGroup(
            "已支持",
            listOf(
                "云端蓝心分析（官方 BlueLM）",
                "端侧蓝心本地智能兜底（端侧 BlueLM 3B）",
                "安全占位（模型全部不可用时保护）",
                "证据链",
                "微测",
                "复习计划",
                "课程库",
                "导出报告",
                "粘贴文本 / .txt / .md",
                "SRT / VTT / TXT 转写稿",
                "手动 OCR 资料流",
            ),
        )
        RoadmapGroup(
            "实验模式",
            listOf(
                "系统实时转写 ASR（依设备能力）",
                "Live ASR 不保存原始音频",
            ),
        )
        RoadmapGroup(
            "待接入",
            listOf(
                "端侧 BlueLM 3B（等待 llm-sdk-release.aar）",
                "端侧文本审核（CmsLocalFrame，规划中）",
                "vivo ASR provider",
                "vivo OCR provider",
                "自动说话人分段",
            ),
        )
        RoadmapGroup(
            "暂缓",
            listOf(
                "第三方平台视频爬取",
                "声纹身份识别",
                "自研底噪处理",
                "云同步 / 团队协作",
            ),
        )
        Spacer(Modifier.height(Dimens.s))
        Text(
            "Proof readiness: 官方路径、兜底、脱敏日志，以及不含凭据的导出材料。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RoadmapGroup(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(Dimens.xs))
    items.forEach { item ->
        Text("- $item", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(Dimens.s))
}

@Composable
private fun PrivacyCard() {
    ClassMateCard {
        Text("Privacy and security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        listOf(
            "Real credentials are not written to the repository, README, logs, exports, history, screenshots, or tests.",
            "Debug import keeps credentials in memory only and displays masked presence status.",
            "Logs use short provider/status/latency/error labels and never include prompt, course text, vendor body, Authorization, app_id value, or reasoning content.",
            "实时转写实验需要麦克风权限，仅在你点击“开始实时转写”后请求；不保存原始音频、不上传音频、不后台录音、不做声纹身份识别。",
        ).forEach { line ->
            Text("- $line", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.xxs))
        }
    }
}

@Composable
private fun LogsCard(viewModel: AppViewModel) {
    val logs = viewModel.ui.logs
    ClassMateCard {
        Text("Last analysis logs (redacted)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        if (logs.isEmpty()) {
            Text("No log yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            logs.forEach { entry ->
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

@Composable
private fun BuildInfoCard() {
    ClassMateCard {
        Text("Build", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        listOf(
            "Version" to "${BuildInfo.versionName} (${BuildInfo.versionCode})",
            "Build type" to BuildInfo.buildType,
            "Built at" to BuildInfo.builtAt,
            "Commit" to BuildInfo.gitCommitShort,
        ).forEach { (label, value) ->
            ProviderStatusRow(label, value)
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
    val diagnostic = viewModel.ui.blueLmDiagnostic
    val diagnosticRunning = viewModel.ui.blueLmDiagnosticRunning
    val compatibleRunning = viewModel.ui.compatibleDiagnosticRunning

    ClassMateCard {
        Text("Debug config import", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text(
            "Debug-only in-memory provider config import. The pasted value is not logged, persisted, or shown back.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.m))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Config JSON") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = "Inspect and import for this session",
            onClick = { preview = viewModel.importDebugProviderConfig(input) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = if (diagnosticRunning) "Testing BlueLM" else "Test BlueLM connection",
            onClick = { viewModel.testBlueLmConnection() },
            enabled = !diagnosticRunning,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        // Neutral "custom model API" affordance — long-term product only, debug-gated and collapsed.
        // Deliberately NOT branded as a competition enhancement and never shown on the main config page.
        PrimaryButton(
            text = if (compatibleRunning) "测试中…" else "测试自定义模型接口（调试）",
            onClick = { viewModel.testCompatibleConnection() },
            enabled = !compatibleRunning,
            modifier = Modifier.fillMaxWidth(),
        )
        preview?.let { p ->
            Spacer(Modifier.height(Dimens.m))
            val color = if (p.containsRealSecret) ClassMateTheme.extended.warning else ClassMateTheme.extended.success
            Text(p.message, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Medium)
            p.providerSummaries.forEach { summary -> ProviderPreviewRow(summary) }
        }
        diagnostic?.let { report ->
            Spacer(Modifier.height(Dimens.m))
            Text("BlueLM diagnostic", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            DiagnosticDetailsCard(lines = report.safeLines())
        }
        viewModel.ui.compatibleDiagnostic?.let { report ->
            Spacer(Modifier.height(Dimens.m))
            Text("自定义模型接口诊断（调试）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            DiagnosticDetailsCard(lines = report.safeLines())
        }
    }
}

@Composable
private fun ProviderPreviewRow(summary: ProviderSummary) {
    Spacer(Modifier.height(Dimens.xxs))
    Text(
        "${summary.provider}: baseUrl=${summary.baseUrl.ifBlank { "not set" }}, model=${summary.model.ifBlank { "not set" }}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "credential_present=${summary.credentialPresent}, appId=${summary.maskedAppId}, appKey=${summary.maskedAppKey}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Small swatch strip per theme for the Settings preview — illustrative, not the live scheme. */
private fun themeSwatches(option: ThemeOption): List<androidx.compose.ui.graphics.Color> = when (option) {
    ThemeOption.FOCUS -> listOf(androidx.compose.ui.graphics.Color(0xFFF3F4F7), androidx.compose.ui.graphics.Color(0xFF3A64D8))
    ThemeOption.VITALITY -> listOf(androidx.compose.ui.graphics.Color(0xFFEEF1F6), androidx.compose.ui.graphics.Color(0xFF515ED0))
    ThemeOption.FLOW -> listOf(androidx.compose.ui.graphics.Color(0xFF1B1A22), androidx.compose.ui.graphics.Color(0xFFE0A86A))
}

/** Software-allocator bitmap decode (hardware bitmaps cannot getPixels). Never throws. */
private fun decodeSoftwareBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}.getOrNull()
