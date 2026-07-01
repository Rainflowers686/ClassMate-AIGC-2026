package com.classmate.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.classmate.app.BuildConfig
import com.classmate.app.BuildInfo
import com.classmate.app.ondevice.BitmapToRgb
import com.classmate.app.ondevice.OnDevicePermissions
import com.classmate.app.platform.ConfigImportPreview
import com.classmate.app.platform.AiModelProviderMode
import com.classmate.app.platform.ModelApiProfile
import com.classmate.app.platform.ProviderDryRunResult
import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.app.platform.ProviderSummary
import com.classmate.app.state.AnalysisSourceReport
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.state.SettingsDeepLink
import com.classmate.core.ondevice.OnDeviceErrorExplain
import com.classmate.core.ondevice.OnDeviceLlmConfig
import com.classmate.core.official.VivoOfficialProviderRegistry
import com.classmate.core.provider.BlueLMDiagnosticStatus
import com.classmate.core.provider.CloudModelQualityProfile
import com.classmate.app.ui.components.CapabilityStatusPill
import com.classmate.app.ui.components.ClassMateChipText
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateSingleLineText
import com.classmate.app.ui.components.ClassMateTwoLineDescription
import com.classmate.app.ui.components.DiagnosticDetailsCard
import com.classmate.app.ui.product.ProductCanvas
import com.classmate.app.ui.product.ProductHero
import com.classmate.app.ui.product.ProductScaffold
import com.classmate.app.ui.product.ProductSpace
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.components.ThemePreviewCard
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.flow.AmbientSoundCatalog
import com.classmate.app.ui.theme.AccentColorPreset
import com.classmate.app.ui.theme.ClassMateColorScheme
import com.classmate.app.ui.theme.ClassMateTheme
import com.classmate.app.ui.theme.CustomPalette
import com.classmate.app.ui.theme.ThemePreset
import com.classmate.app.ui.theme.TypographyPreset
import com.classmate.app.ui.theme.bestOnColorFor
import com.classmate.app.ui.theme.classMateColorScheme
import com.classmate.app.ui.theme.classMateTypographyFor
import com.classmate.app.ui.theme.normalizeHexColorOrNull
import com.classmate.app.ui.theme.validateCustomPalette
import com.classmate.app.ui.theme.withCustomPalette
import com.classmate.app.ui.i18n.AppLanguage
import com.classmate.app.ui.i18n.appStrings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

enum class SettingsPage(val title: String, val subtitle: String) {
    SETTINGS_HOME("设置", "日常设置与诊断入口分开管理"),
    GENERAL_SETTINGS("通用设置", "外观、AI 模型、隐私、导出设置和背景音"),
    APPEARANCE_THEME("外观与主题", "默认学习、活力学习、沉浸学习与强调色"),
    ADVANCED_COLOR_CUSTOMIZATION("高级颜色自定义", "自定义 ClassMate 的主色、次色和辅助色"),
    AI_MODEL_CONFIG("AI 模型配置", "蓝心大模型与自有模型配置，保存后持续可用"),
    PRIVACY_PERMISSIONS("隐私与权限", "本地数据、导入权限和用户确认说明"),
    LEARNING_EXPORT("导出设置", "学习包、复习报告和导出格式"),
    AMBIENT_SOUND("沉浸式背景音", "授权循环背景音、音量和播放说明"),
    EXPERIMENTAL_FEATURES("实验性功能", "学习图解、复习短视频和双语课堂入口"),
    DEVELOPER_SETTINGS("开发者设置", "诊断、smoke、端侧状态与脱敏日志");

    /** Parent page for in-settings back navigation (system back goes up this tree, not out of the app). */
    val parent: SettingsPage?
        get() = when (this) {
            SETTINGS_HOME -> null
            GENERAL_SETTINGS, DEVELOPER_SETTINGS -> SETTINGS_HOME
            APPEARANCE_THEME, AI_MODEL_CONFIG, PRIVACY_PERMISSIONS, LEARNING_EXPORT, AMBIENT_SOUND, EXPERIMENTAL_FEATURES -> GENERAL_SETTINGS
            ADVANCED_COLOR_CUSTOMIZATION -> APPEARANCE_THEME
        }
}

private enum class SettingsEntryIcon {
    APPEARANCE_THEME,
    ADVANCED_COLOR,
    AI_MODEL_CONFIG,
    PRIVACY_PERMISSIONS,
    LEARNING_EXPORT,
    AMBIENT_SOUND,
    EXPERIMENTAL_FEATURES,
    GENERAL_SETTINGS,
    DEVELOPER_SETTINGS,
}

private fun SettingsEntryIcon.imageVector(): ImageVector = when (this) {
    SettingsEntryIcon.APPEARANCE_THEME -> Icons.Filled.Star
    SettingsEntryIcon.ADVANCED_COLOR -> Icons.Filled.Edit
    SettingsEntryIcon.AI_MODEL_CONFIG -> Icons.Filled.PlayArrow
    SettingsEntryIcon.PRIVACY_PERMISSIONS -> Icons.Filled.CheckCircle
    SettingsEntryIcon.LEARNING_EXPORT -> Icons.Filled.DateRange
    SettingsEntryIcon.AMBIENT_SOUND -> Icons.Filled.Add
    SettingsEntryIcon.EXPERIMENTAL_FEATURES -> Icons.Filled.Star
    SettingsEntryIcon.GENERAL_SETTINGS -> Icons.Filled.Settings
    SettingsEntryIcon.DEVELOPER_SETTINGS -> Icons.Filled.Edit
}

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val s = appStrings(ui.language)
    var showDebug by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    // The settings sub-page lives in the ViewModel (viewModel.settingsPage) so the Android system back
    // key / gesture can walk up the settings tree instead of exiting the app. Reads use that state;
    // writes go through openSettingsPage.
    val page = viewModel.settingsPage

    LaunchedEffect(ui.settingsDeepLink) {
        if (ui.settingsDeepLink == SettingsDeepLink.AI_MODEL_CONFIG_BLUELM) {
            viewModel.openSettingsPage(SettingsPage.AI_MODEL_CONFIG)
            viewModel.consumeSettingsDeepLink()
        }
    }

    ProductCanvas {
      ProductScaffold(contextLabel = s.settingsTitle) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ProductSpace.gutter)
                .padding(bottom = 224.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            Spacer(Modifier.height(ProductSpace.tight))
            when (page) {
                SettingsPage.SETTINGS_HOME -> {
                    ProductHero(
                        overline = "偏好",
                        title = "设置",
                        subtitle = "学习体验、模型配置和诊断入口分开管理。",
                    )
                    SettingsHomeStatusCards(viewModel)
                    SettingsHomeCard(
                        onGeneral = { viewModel.openSettingsPage(SettingsPage.GENERAL_SETTINGS) },
                        onDeveloper = { viewModel.openSettingsPage(SettingsPage.DEVELOPER_SETTINGS) },
                    )
                }

                SettingsPage.GENERAL_SETTINGS -> {
                    SettingsPageHeader(page = page, onBack = { viewModel.openSettingsPage(SettingsPage.SETTINGS_HOME) })
                    GeneralSettingsListCard(
                        onAppearance = { viewModel.openSettingsPage(SettingsPage.APPEARANCE_THEME) },
                        onAiModel = { viewModel.openSettingsPage(SettingsPage.AI_MODEL_CONFIG) },
                        onPrivacy = { viewModel.openSettingsPage(SettingsPage.PRIVACY_PERMISSIONS) },
                        onLearningExport = { viewModel.openSettingsPage(SettingsPage.LEARNING_EXPORT) },
                        onAmbientAudio = { viewModel.openSettingsPage(SettingsPage.AMBIENT_SOUND) },
                        onExperimentalFeatures = { viewModel.openSettingsPage(SettingsPage.EXPERIMENTAL_FEATURES) },
                    )
                }

                SettingsPage.APPEARANCE_THEME -> {
                    SettingsPageHeader(page = page, onBack = { viewModel.openSettingsPage(SettingsPage.GENERAL_SETTINGS) })
                    AppearanceAndThemeSettingsCard(
                        viewModel = viewModel,
                        onAdvancedColors = { viewModel.openSettingsPage(SettingsPage.ADVANCED_COLOR_CUSTOMIZATION) },
                    )
                }

                SettingsPage.ADVANCED_COLOR_CUSTOMIZATION -> {
                    SettingsPageHeader(page = page, onBack = { viewModel.openSettingsPage(SettingsPage.APPEARANCE_THEME) })
                    AdvancedColorCustomizationPage(
                        viewModel = viewModel,
                        onBack = { viewModel.openSettingsPage(SettingsPage.APPEARANCE_THEME) },
                    )
                }

                SettingsPage.AI_MODEL_CONFIG -> {
                    SettingsPageHeader(page = page, onBack = { viewModel.openSettingsPage(SettingsPage.GENERAL_SETTINGS) })
                    AiModelConfigurationPage(viewModel)
                    ModelAccessNotesCard(viewModel)
                    OfficialProviderReadinessCard(includeDevLab = false)
                }

                SettingsPage.PRIVACY_PERMISSIONS -> {
                    SettingsPageHeader(page = page, onBack = { viewModel.openSettingsPage(SettingsPage.GENERAL_SETTINGS) })
                    PrivacyAndPermissionsSettingsCard()
                    PermissionCenterCard(viewModel)
                }

                SettingsPage.LEARNING_EXPORT -> {
                    SettingsPageHeader(page = page, onBack = { viewModel.openSettingsPage(SettingsPage.GENERAL_SETTINGS) })
                    LearningExportSettingsCard(viewModel)
                    LearningExportDocxPolicyCard(viewModel)
                }

                SettingsPage.AMBIENT_SOUND -> {
                    SettingsPageHeader(page = page, onBack = { viewModel.openSettingsPage(SettingsPage.GENERAL_SETTINGS) })
                    BackgroundAudioPolicyCard()
                }

                SettingsPage.EXPERIMENTAL_FEATURES -> {
                    SettingsPageHeader(page = page, onBack = { viewModel.openSettingsPage(SettingsPage.GENERAL_SETTINGS) })
                    ExperimentalFeaturesSettingsCard(viewModel)
                }

                SettingsPage.DEVELOPER_SETTINGS -> {
                    SettingsPageHeader(page = page, onBack = { viewModel.openSettingsPage(SettingsPage.SETTINGS_HOME) })
                    DeveloperSettingsHomeCard(viewModel)
                    OnDeviceDiagnosticCard(viewModel)
                    OnDeviceMultimodalDiagnosticCard(viewModel)
                    OnDeviceCapabilityCard(viewModel)
                    OfficialProviderReadinessCard(includeDevLab = true)
                    if (BuildConfig.DEBUG) {
                        ClassMateCard {
                            Text("Debug 导入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(Dimens.s))
                            Text("仅 debug build 可见。用于临时诊断，不保存到普通用户设置。", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun SettingsPageHeader(page: SettingsPage, onBack: () -> Unit) {
    val colors = ClassMateTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.s),
    ) {
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onBack),
            shape = RoundedCornerShape(999.dp),
            color = colors.surface.copy(alpha = if (colors.isDark) 0.78f else 0.9f),
            border = BorderStroke(0.75.dp, colors.outline.copy(alpha = if (colors.isDark) 0.12f else 0.06f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                page.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                page.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsHomeStatusCards(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val colors = ClassMateTheme.colors
    val multimodalReady = ui.onDeviceMultimodalDiagnostic?.callVitMethodPresent == true
    val modelPermissionReady = ui.onDevicePermissions.allFilesAccess
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (colors.isDark) colors.surfaceContainerLow.copy(alpha = 0.78f) else colors.surface.copy(alpha = 0.96f),
        border = BorderStroke(0.75.dp, colors.outline.copy(alpha = if (colors.isDark) 0.14f else 0.07f)),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
    ) {
        Row(
            Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsMiniStatusCard(
                title = "导入草稿",
                status = if (multimodalReady) "可编辑" else "按配置启用",
                detail = "图片资料先进入确认草稿",
                active = multimodalReady,
                modifier = Modifier.weight(1f),
            )
            SettingsMiniStatusCard(
                title = "端侧模型",
                status = if (modelPermissionReady) "已授权" else "待确认",
                detail = "影响本机处理和离线兜底",
                active = modelPermissionReady,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SettingsMiniStatusCard(
    title: String,
    status: String,
    detail: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = ClassMateTheme.colors
    val container by animateColorAsState(
        targetValue = if (active) colors.primary.copy(alpha = if (colors.isDark) 0.08f else 0.055f) else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "settings-mini-status-container",
    )
    val border by animateColorAsState(
        targetValue = if (active) colors.primary.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "settings-mini-status-border",
    )
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 88.dp),
        shape = RoundedCornerShape(17.dp),
        color = container,
        border = BorderStroke(0.75.dp, border),
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(Dimens.s), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status, style = MaterialTheme.typography.labelMedium, color = if (active) colors.primary else colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsHomeCard(
    onGeneral: () -> Unit,
    onDeveloper: () -> Unit,
) {
    SettingsGroupedListCard {
        SettingsEntryRow("通用设置", "外观、AI 模型配置、隐私权限、导出设置与沉浸背景音", SettingsEntryIcon.GENERAL_SETTINGS, onGeneral, emphasized = true, grouped = true)
        SettingsEntryRow("开发者设置", "Provider 诊断、smoke dry-run、端侧状态与脱敏日志", SettingsEntryIcon.DEVELOPER_SETTINGS, onDeveloper, grouped = true)
    }
}

@Composable
private fun GeneralSettingsListCard(
    onAppearance: () -> Unit,
    onAiModel: () -> Unit,
    onPrivacy: () -> Unit,
    onLearningExport: () -> Unit,
    onAmbientAudio: () -> Unit,
    onExperimentalFeatures: () -> Unit,
) {
    SettingsGroupedListCard {
        SettingsEntryRow("外观与主题", "默认学习、活力学习、沉浸学习、强调色和阅读密度", SettingsEntryIcon.APPEARANCE_THEME, onAppearance, grouped = true)
        SettingsEntryRow("AI 模型配置", "蓝心大模型与自有模型配置，保存后持续可用", SettingsEntryIcon.AI_MODEL_CONFIG, onAiModel, grouped = true)
        SettingsEntryRow("隐私与权限", "本地数据、用户确认、导入内容和相机 / 文件 / 音频权限", SettingsEntryIcon.PRIVACY_PERMISSIONS, onPrivacy, grouped = true)
        SettingsEntryRow("导出设置", "学习包、复习报告、PDF / Word / HTML / Markdown", SettingsEntryIcon.LEARNING_EXPORT, onLearningExport, grouped = true)
        SettingsEntryRow("沉浸式背景音", "6 种授权循环背景音、音量和播放说明", SettingsEntryIcon.AMBIENT_SOUND, onAmbientAudio, grouped = true)
        SettingsEntryRow("实验性功能", "学习图解、复习短视频和双语课堂同声传译入口", SettingsEntryIcon.EXPERIMENTAL_FEATURES, onExperimentalFeatures, grouped = true)
    }
}

@Composable
private fun SettingsGroupedListCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = ClassMateTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (colors.isDark) colors.surfaceContainerLow.copy(alpha = 0.82f) else colors.surface.copy(alpha = 0.96f),
        border = BorderStroke(0.75.dp, colors.outline.copy(alpha = if (colors.isDark) 0.14f else 0.07f)),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
    ) {
        Column(
            Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            content = content,
        )
    }
}

@Composable
private fun ExperimentalFeaturesSettingsCard(viewModel: AppViewModel) {
    val ui = viewModel.ui
    ClassMateCard {
        Text("实验性功能", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        ClassMateTwoLineDescription("默认关闭；开启后才会在学习页面显示相关入口，主学习闭环仍按稳定路径运行。")
        Spacer(Modifier.height(Dimens.s))
        ExperimentalFeatureToggleRow(
            title = "实验性：学习图解生成",
            description = "根据知识点生成概念图提示词；图片生成服务未配置时只保留提示词。",
            checked = ui.enableExperimentalImageGeneration,
            onCheckedChange = viewModel::setExperimentalImageGeneration,
        )
        ExperimentalFeatureToggleRow(
            title = "实验性：复习短视频生成",
            description = "根据错题和复习任务生成短视频脚本/分镜；不伪装真实视频生成。",
            checked = ui.enableExperimentalVideoGeneration,
            onCheckedChange = viewModel::setExperimentalVideoGeneration,
        )
        ExperimentalFeatureToggleRow(
            title = "实验性：双语课堂同声传译",
            description = "用于英文授课和双语课堂；当前优先保留双语转写草稿和翻译证据。",
            checked = ui.enableExperimentalSimultaneousInterpretation,
            onCheckedChange = viewModel::setExperimentalSimultaneousInterpretation,
        )
    }
}

@Composable
private fun ExperimentalFeatureToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.s),
    ) {
        Column(Modifier.weight(1f)) {
            ClassMateSingleLineText(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Dimens.xxs))
            ClassMateTwoLineDescription(description)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsEntryRow(title: String, subtitle: String, icon: SettingsEntryIcon, onClick: () -> Unit, emphasized: Boolean = false, grouped: Boolean = false) {
    val colors = ClassMateTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val container by animateColorAsState(
        targetValue = when {
            pressed -> colors.surfaceContainerHigh.copy(alpha = if (colors.isDark) 0.78f else 0.86f)
            grouped -> androidx.compose.ui.graphics.Color.Transparent
            emphasized -> colors.surface.copy(alpha = if (colors.isDark) 0.92f else 0.98f)
            else -> colors.surface.copy(alpha = if (colors.isDark) 0.88f else 0.96f)
        },
        animationSpec = tween(durationMillis = 170),
        label = "settings-entry-container",
    )
    val iconContainer by animateColorAsState(
        targetValue = when {
            pressed -> colors.primary.copy(alpha = 0.12f)
            emphasized -> colors.primary.copy(alpha = if (colors.isDark) 0.08f else 0.045f)
            else -> colors.surfaceContainerHigh.copy(alpha = if (colors.isDark) 0.64f else 0.74f)
        },
        animationSpec = tween(durationMillis = 170),
        label = "settings-entry-icon-container",
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed || colors.isDark) 0.dp else 1.dp,
        animationSpec = tween(durationMillis = 170),
        label = "settings-entry-elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.988f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "settings-entry-scale",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (grouped) 0.dp else Dimens.xxs)
            .defaultMinSize(minHeight = 72.dp)
            .scale(scale)
            .clickable(interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(if (grouped) 16.dp else 17.dp),
        color = container,
        border = if (grouped) null else BorderStroke(0.75.dp, if (emphasized) colors.primary.copy(alpha = if (colors.isDark) 0.12f else 0.075f) else colors.outline.copy(alpha = if (colors.isDark) 0.18f else 0.085f)),
        shadowElevation = if (grouped) 0.dp else elevation,
    ) {
        Row(
            Modifier.padding(horizontal = Dimens.m, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon.imageVector(), contentDescription = null, tint = if (emphasized) colors.primary else colors.textSecondary, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AppearanceAndThemeSettingsCard(
    viewModel: AppViewModel,
    onAdvancedColors: () -> Unit,
) {
    val ui = viewModel.ui
    val s = appStrings(ui.language)
    val systemLabel = AppLanguage.SYSTEM.displayNameFor(ui.language)
    val previewPalette = ui.customPalette
    ClassMateCard {
        Text("外观与主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        ClassMateTwoLineDescription(
            "选择适合当前学习节奏的界面氛围。主题决定背景、卡片层级和圆角；强调色只影响按钮、选中态和重点状态。",
        )
        Spacer(Modifier.height(Dimens.s))
        Text(s.settingsLanguage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xxs))
        ClassMateTwoLineDescription(s.settingsLanguageDesc)
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            AppLanguage.entries.forEach { lang ->
                SelectableChip(lang.displayNameFor(ui.language), ui.language == lang, modifier = Modifier.weight(1f)) { viewModel.setLanguage(lang) }
            }
        }
        Spacer(Modifier.height(Dimens.m))
        Text("学习主题", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        ThemePreset.entries.forEach { option ->
            val preview = classMateColorScheme(option, ui.accentColor).withCustomPalette(previewPalette)
            ThemePreviewCard(
                name = option.displayName,
                tagline = themeSelectorTagline(option),
                description = themeSelectorDescription(option),
                backgroundColor = preview.background,
                surfaceColor = preview.surfaceContainerLow,
                accentColor = preview.primary,
                secondaryColor = preview.secondary,
                tertiaryColor = preview.tertiary,
                selected = ui.theme == option,
                onClick = { viewModel.setTheme(option) },
                modifier = Modifier.padding(top = Dimens.xs),
            )
        }
        Spacer(Modifier.height(Dimens.m))
        Text("强调色", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        ClassMateTwoLineDescription("强调色会跟随三套主题调整亮度和可读性，不会覆盖每套主题自己的 surface 层级。")
        Spacer(Modifier.height(Dimens.s))
        AccentColorGrid(
            selected = ui.accentColor,
            themePreset = ui.theme,
            onSelect = { viewModel.setAccentColor(it) },
        )
        Spacer(Modifier.height(Dimens.m))
        SettingsGroupedListCard {
            SettingsEntryRow(
                "高级颜色自定义",
                "自定义主色、次色与辅助色，并自动检查文字可读性",
                SettingsEntryIcon.ADVANCED_COLOR,
                onAdvancedColors,
                grouped = true,
            )
        }
        Spacer(Modifier.height(Dimens.m))
        TypographyPresetSection(
            selected = ui.typographyPreset,
            onSelect = { viewModel.setTypographyPreset(it) },
        )
        Spacer(Modifier.height(Dimens.m))
        Text("显示", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            SelectableChip(systemLabel, ui.darkMode == null, modifier = Modifier.weight(1f)) { viewModel.setDarkMode(null) }
            SelectableChip("浅色", ui.darkMode == false, modifier = Modifier.weight(1f)) { viewModel.setDarkMode(false) }
            SelectableChip("深色", ui.darkMode == true, modifier = Modifier.weight(1f)) { viewModel.setDarkMode(true) }
        }
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("文字适配", "标题单行、说明两行、chip 与按钮单行显示")
    }
}

private fun themeSelectorTagline(theme: ThemePreset): String = when (theme) {
    ThemePreset.STANDARD_STUDY -> "日常阅读与复习"
    ThemePreset.ACTIVE_STUDY -> "练习与进度反馈"
    ThemePreset.FOCUS_IMMERSION -> "专注学习"
}

private fun themeSelectorDescription(theme: ThemePreset): String = when (theme) {
    ThemePreset.STANDARD_STUDY -> "安静留白，适合日常阅读与复习"
    ThemePreset.ACTIVE_STUDY -> "更明快，适合练习与进度反馈"
    ThemePreset.FOCUS_IMMERSION -> "深色低干扰，适合专注学习"
}

@Composable
private fun AdvancedColorCustomizationPage(viewModel: AppViewModel, onBack: () -> Unit) {
    val ui = viewModel.ui
    val colors = ClassMateTheme.colors
    var customEnabled by remember(ui.customPalette) { mutableStateOf(ui.customPalette.enabled) }
    var primaryHex by remember(ui.customPalette) { mutableStateOf(ui.customPalette.primaryHex) }
    var secondaryHex by remember(ui.customPalette) { mutableStateOf(ui.customPalette.secondaryHex) }
    var tertiaryHex by remember(ui.customPalette) { mutableStateOf(ui.customPalette.tertiaryHex) }
    val normalizedPrimaryHex = normalizeHexColorOrNull(primaryHex)
    val normalizedSecondaryHex = normalizeHexColorOrNull(secondaryHex)
    val normalizedTertiaryHex = normalizeHexColorOrNull(tertiaryHex)
    val canApplyCustomPalette = normalizedPrimaryHex != null && normalizedSecondaryHex != null && normalizedTertiaryHex != null
    val draftPalette = CustomPalette(
        enabled = true,
        primaryHex = normalizedPrimaryHex ?: primaryHex,
        secondaryHex = normalizedSecondaryHex ?: secondaryHex,
        tertiaryHex = normalizedTertiaryHex ?: tertiaryHex,
    )
    val previewPalette = if (canApplyCustomPalette) {
        CustomPalette(
            enabled = true,
            primaryHex = normalizedPrimaryHex ?: CustomPalette.DEFAULT_PRIMARY,
            secondaryHex = normalizedSecondaryHex ?: CustomPalette.DEFAULT_SECONDARY,
            tertiaryHex = normalizedTertiaryHex ?: CustomPalette.DEFAULT_TERTIARY,
        )
    } else {
        CustomPalette.Default
    }
    val customWarnings = validateCustomPalette(draftPalette, colors.background, colors.textPrimary)
    val preview = classMateColorScheme(ui.theme, ui.accentColor, dark = colors.isDark).withCustomPalette(previewPalette)
    val customApplied = ui.customPalette.enabled &&
        normalizedPrimaryHex == ui.customPalette.primaryHex &&
        normalizedSecondaryHex == ui.customPalette.secondaryHex &&
        normalizedTertiaryHex == ui.customPalette.tertiaryHex

    ClassMateCard {
        Text("高级颜色自定义", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        ClassMateTwoLineDescription("自定义 ClassMate 的主色、次色和辅助色。正文阅读色会由主题继续保护。")
        Spacer(Modifier.height(Dimens.m))
        AdvancedColorImpactPreview(preview)
        Spacer(Modifier.height(Dimens.m))
        AdvancedColorSection(
            enabled = customEnabled,
            primaryHex = primaryHex,
            secondaryHex = secondaryHex,
            tertiaryHex = tertiaryHex,
            warnings = customWarnings,
            canApply = canApplyCustomPalette,
            applied = customApplied,
            onEnabledChange = { customEnabled = it },
            onPrimaryChange = { primaryHex = it },
            onSecondaryChange = { secondaryHex = it },
            onTertiaryChange = { tertiaryHex = it },
            onApply = {
                val nextPalette = CustomPalette(
                    enabled = true,
                    primaryHex = normalizedPrimaryHex ?: CustomPalette.DEFAULT_PRIMARY,
                    secondaryHex = normalizedSecondaryHex ?: CustomPalette.DEFAULT_SECONDARY,
                    tertiaryHex = normalizedTertiaryHex ?: CustomPalette.DEFAULT_TERTIARY,
                )
                customEnabled = true
                primaryHex = nextPalette.primaryHex
                secondaryHex = nextPalette.secondaryHex
                tertiaryHex = nextPalette.tertiaryHex
                viewModel.setCustomPalette(nextPalette)
            },
            onReset = {
                customEnabled = false
                primaryHex = CustomPalette.DEFAULT_PRIMARY
                secondaryHex = CustomPalette.DEFAULT_SECONDARY
                tertiaryHex = CustomPalette.DEFAULT_TERTIARY
                viewModel.resetCustomPalette()
            },
        )
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton("返回外观与主题", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AdvancedColorImpactPreview(preview: ClassMateColorScheme) {
    Text("主题影响预览", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(Dimens.xs))
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            ColorImpactBlock(
                title = "主按钮",
                subtitle = "Primary Action",
                container = preview.primary,
                content = bestOnColorFor(preview.primary),
                modifier = Modifier.weight(1f),
            )
            ColorImpactBlock(
                title = "学习状态",
                subtitle = "Progress / Success",
                container = preview.progressSurface,
                content = preview.secondary,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            ColorImpactBlock(
                title = "证据与专注",
                subtitle = "Evidence / Focus",
                container = preview.evidenceSurface,
                content = preview.tertiary,
                modifier = Modifier.weight(1f),
            )
            MixedPalettePreview(preview, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ColorImpactBlock(
    title: String,
    subtitle: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 78.dp),
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
        border = BorderStroke(0.75.dp, content.copy(alpha = 0.22f)),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            ClassMateSingleLineText(title, style = MaterialTheme.typography.labelLarge, color = content, fontWeight = FontWeight.SemiBold)
            ClassMateSingleLineText(subtitle, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.82f))
        }
    }
}

@Composable
private fun MixedPalettePreview(preview: ClassMateColorScheme, modifier: Modifier = Modifier) {
    val content = preview.textPrimary
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 78.dp),
        shape = RoundedCornerShape(16.dp),
        color = preview.surfaceContainerLow,
        contentColor = content,
        border = BorderStroke(0.75.dp, preview.outline.copy(alpha = 0.28f)),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ClassMateSingleLineText("三色组合", style = MaterialTheme.typography.labelLarge, color = content, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(preview.primary, preview.secondary, preview.tertiary).forEach { color ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(color),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp)).background(preview.primary))
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp)).background(preview.secondary))
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp)).background(preview.tertiary))
            }
        }
    }
}

@Composable
private fun AdvancedColorSection(
    enabled: Boolean,
    primaryHex: String,
    secondaryHex: String,
    tertiaryHex: String,
    warnings: List<String>,
    canApply: Boolean,
    applied: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPrimaryChange: (String) -> Unit,
    onSecondaryChange: (String) -> Unit,
    onTertiaryChange: (String) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
) {
    Text("高级自定义色彩", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(Dimens.xs))
    ClassMateTwoLineDescription("主色影响 CTA 与选中态；次色影响进度与复习状态；辅助色影响证据、专注和辅助高亮。")
    Spacer(Modifier.height(Dimens.s))
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
        SelectableChip("启用", enabled) { onEnabledChange(true) }
        SelectableChip("关闭", !enabled) { onEnabledChange(false) }
    }
    Spacer(Modifier.height(Dimens.s))
    CustomColorEditor("Primary", primaryHex, onPrimaryChange)
    Spacer(Modifier.height(Dimens.xs))
    CustomColorEditor("Secondary", secondaryHex, onSecondaryChange)
    Spacer(Modifier.height(Dimens.xs))
    CustomColorEditor("Tertiary", tertiaryHex, onTertiaryChange)
    Spacer(Modifier.height(Dimens.s))
    val status = if (!canApply) {
        "HEX 格式未通过，请修正后再应用"
    } else if (applied) {
        "已应用；预览、选中态和按钮会立即反映自定义色"
    } else if (!enabled) {
        "未启用；当前使用预设强调色"
    } else if (warnings.isEmpty()) {
        "对比度检查通过；文字色会自动选择深色或浅色"
    } else {
        warnings.take(2).joinToString("；")
    }
    ProviderStatusRow("对比度检查", status)
    Spacer(Modifier.height(Dimens.s))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
        PrimaryButton(if (applied) "已应用" else "应用自定义色", onClick = onApply, modifier = Modifier.weight(1f), enabled = canApply)
        SecondaryButton("恢复默认高级颜色", onClick = onReset, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CustomColorEditor(label: String, value: String, onValueChange: (String) -> Unit) {
    val normalized = normalizeHexColorOrNull(value)
    val preview = normalized?.let { com.classmate.app.ui.theme.parseHexColorOrNull(it) } ?: ClassMateTheme.colors.outline
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = preview,
                contentColor = bestOnColorFor(preview),
                border = BorderStroke(0.75.dp, ClassMateTheme.colors.outline.copy(alpha = 0.24f)),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Aa", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("#RRGGBB", maxLines = 1, softWrap = false) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                supportingText = {
                    Text(
                        normalized ?: "请输入 6 位 HEX，例如 #55624D",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
        CustomColorPresetSwatches(onPick = onValueChange)
    }
}

@Composable
private fun CustomColorPresetSwatches(onPick: (String) -> Unit) {
    val presets = listOf(
        CustomPalette.DEFAULT_PRIMARY,
        CustomPalette.DEFAULT_SECONDARY,
        CustomPalette.DEFAULT_TERTIARY,
        AccentColorPreset.BLUE.tokenHex,
        AccentColorPreset.CYAN.tokenHex,
        AccentColorPreset.GREEN.tokenHex,
        AccentColorPreset.PURPLE.tokenHex,
        AccentColorPreset.AMBER.tokenHex,
        AccentColorPreset.ROSE.tokenHex,
        AccentColorPreset.GRAPHITE.tokenHex,
        AccentColorPreset.OCEAN.tokenHex,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        presets.take(8).forEach { hex ->
            val color = com.classmate.app.ui.theme.parseHexColorOrNull(hex) ?: ClassMateTheme.colors.outline
            Box(
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .clickable { onPick(hex) },
            )
        }
    }
}

@Composable
private fun TypographyPresetSection(
    selected: TypographyPreset,
    onSelect: (TypographyPreset) -> Unit,
) {
    Text("字体与阅读", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(Dimens.xs))
    ClassMateTwoLineDescription("不引入外部字体文件；正文始终优先可读，个性风格只加强标题层级。")
    Spacer(Modifier.height(Dimens.s))
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        TypographyPreset.entries.forEach { preset ->
            TypographyPresetRow(preset, selected == preset, onClick = { onSelect(preset) })
        }
    }
}

@Composable
private fun TypographyPresetRow(preset: TypographyPreset, selected: Boolean, onClick: () -> Unit) {
    val colors = ClassMateTheme.colors
    val previewTypography = classMateTypographyFor(preset)
    val container by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = if (colors.isDark) 0.1f else 0.06f) else Color.Transparent,
        animationSpec = tween(durationMillis = 160),
        label = "typography-preset-row",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ClassMateSingleLineText(preset.displayName, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            ClassMateTwoLineDescription(preset.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface.copy(alpha = if (colors.isDark) 0.46f else 0.68f),
                border = BorderStroke(0.75.dp, colors.outline.copy(alpha = if (selected) 0.24f else 0.14f)),
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ClassMateSingleLineText("标题预览 Aa", style = previewTypography.titleMedium, color = colors.textPrimary)
                    Text(
                        "正文预览：知识点、证据和复习动作",
                        style = previewTypography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ClassMateSingleLineText("按钮预览", style = previewTypography.labelLarge, color = colors.primary)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.1f),
                        ) {
                            ClassMateSingleLineText(
                                "Chip 预览",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = previewTypography.labelLarge,
                                color = colors.primary,
                            )
                        }
                    }
                }
            }
        }
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AccentColorGrid(
    selected: AccentColorPreset,
    themePreset: ThemePreset,
    onSelect: (AccentColorPreset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
        AccentColorPreset.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xs), modifier = Modifier.fillMaxWidth()) {
                row.forEach { accent ->
                    AccentColorSwatch(
                        accent = accent,
                        themePreset = themePreset,
                        selected = accent == selected,
                        onClick = { onSelect(accent) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentColorSwatch(
    accent: AccentColorPreset,
    themePreset: ThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = classMateColorScheme(themePreset, accent)
    val tokens = ClassMateTheme.colors
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.002f else 1f,
        animationSpec = tween(durationMillis = 170),
        label = "accent-swatch-scale",
    )
    val container by animateColorAsState(
        targetValue = if (selected) preview.primary.copy(alpha = if (tokens.isDark) 0.055f else 0.032f) else tokens.surfaceContainerHigh.copy(alpha = if (tokens.isDark) 0.72f else 0.82f),
        animationSpec = tween(durationMillis = 170),
        label = "accent-swatch-container",
    )
    val border by animateColorAsState(
        targetValue = if (selected) preview.primary.copy(alpha = if (tokens.isDark) 0.3f else 0.28f) else tokens.outline.copy(alpha = 0.2f),
        animationSpec = tween(durationMillis = 170),
        label = "accent-swatch-border",
    )
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 76.dp).scale(scale).clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = tokens.textPrimary,
        border = BorderStroke(0.75.dp, border),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = preview.primary.copy(alpha = if (selected) 0.09f else 0.075f),
                border = BorderStroke(0.75.dp, preview.primary.copy(alpha = if (selected) 0.28f else 0.22f)),
            ) {
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(preview.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = if (preview.isDark) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(accent.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
            Text(accent.englishName, style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
        }
    }
}

@Composable
private fun AiModelConfigurationPage(viewModel: AppViewModel) {
    val summary = viewModel.ui.providerConfigSummary
    ClassMateCard {
        Text("AI 模型配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "蓝心大模型和其他模型都只保存在本机应用私有存储。未配置云端时，课堂分析、问答、练习和导出仍会继续端侧处理或手动编辑。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("当前模型：云端蓝心", "qwen3.5-plus / DEEP_STUDY")
        ProviderStatusRow("Cloud quality profile", cloudQualityProfileStatus())
        ProviderStatusRow("Official OCR config", configuredStatus(summary.officialProviders.ocrConfigured))
        ProviderStatusRow("Query Rewrite config", configuredStatus(summary.officialProviders.queryRewriteConfigured))
        ProviderStatusRow("Text Similarity config", configuredStatus(summary.officialProviders.textSimilarityConfigured))
        ProviderStatusRow("Embedding config", configuredStatus(summary.officialProviders.embeddingConfigured))
        ProviderStatusRow("Translation config", configuredStatus(summary.officialProviders.translationConfigured))
        ProviderStatusRow("TTS config", configuredStatus(summary.officialProviders.ttsConfigured, missing = "missing: script-only"))
    }
    OfficialBlueLmConfigCard(viewModel)
    CustomModelConfigCard(viewModel)
}

@Composable
private fun OfficialBlueLmConfigCard(viewModel: AppViewModel) {
    val masked = viewModel.ui.modelConfigMasked
    val selected = masked?.mode != AiModelProviderMode.CUSTOM
    var appId by remember { mutableStateOf(ModelApiProfile.DEFAULT_APP_ID) }
    var appKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除蓝心大模型配置") },
            text = { Text("删除后云端蓝心会变为未配置。你仍可以继续使用端侧蓝心或手动编辑。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteOfficialModelConfig() }) {
                    Text("删除配置")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }

    ClassMateCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            RadioButton(selected = selected, onClick = { viewModel.selectAiModelProviderMode(AiModelProviderMode.OFFICIAL_BLUELM) })
            Column(Modifier.weight(1f)) {
                Text("蓝心大模型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "适合比赛官方能力，使用云端蓝心 / qwen3.5-plus，重要学习任务使用深度学习配置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("当前状态", if (masked?.officialConfigured == true) "已配置" else "未配置")
        ProviderStatusRow("默认 AppID", "2026374747")
        ProviderStatusRow("质量模式", "默认 DEEP_STUDY；重要学习任务使用深度思考，轻量任务使用均衡")
        ProviderStatusRow("配置保存", "配置仅保存在本机，不写入 Git / docs / tests")
        Spacer(Modifier.height(Dimens.s))
        OutlinedTextField(
            value = appId,
            onValueChange = { appId = it },
            label = { Text("AppID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "恢复默认 AppID",
            onClick = { appId = ModelApiProfile.DEFAULT_APP_ID },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        OutlinedTextField(
            value = appKey,
            onValueChange = { appKey = it },
            label = { Text("AppKey") },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = if (showKey) "隐藏 AppKey" else "显示 AppKey",
            onClick = { showKey = !showKey },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = "保存配置",
            onClick = {
                viewModel.saveOfficialModelConfig(
                    baseUrl = ModelApiProfile.DEFAULT_BASE_URL,
                    model = ModelApiProfile.DEFAULT_MODEL,
                    appId = appId,
                    appKey = appKey,
                )
                appKey = ""
                showKey = false
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "测试配置（readiness / dry-run）",
            onClick = { viewModel.testAiConfigReadiness() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "删除配置",
            onClick = { confirmDelete = true },
            modifier = Modifier.fillMaxWidth(),
        )
        if (masked?.officialConfigured == true) {
            Spacer(Modifier.height(Dimens.s))
            Text("蓝心大模型已配置；完整 AppKey 不会显示。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CustomModelConfigCard(viewModel: AppViewModel) {
    val masked = viewModel.ui.modelConfigMasked
    val selected = masked?.mode == AiModelProviderMode.CUSTOM
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var advancedJson by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var jsonError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除其他模型配置") },
            text = { Text("删除后自有模型会变为未配置。已保存的蓝心大模型配置不会被覆盖。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteCustomModelConfig() }) {
                    Text("删除配置")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }

    ClassMateCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            RadioButton(selected = selected, onClick = { viewModel.selectAiModelProviderMode(AiModelProviderMode.CUSTOM) })
            Column(Modifier.weight(1f)) {
                Text("其他模型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "适合使用自己的兼容模型服务。高级 JSON 配置可填写 endpoint、model、headers、bodyTemplate 或 extra fields。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("当前状态", if (masked?.customConfigured == true) "已配置" else "未配置")
        ProviderStatusRow("高级 JSON 配置", if (masked?.customAdvancedJsonPresent == true) "已保存" else "未填写")
        Spacer(Modifier.height(Dimens.s))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = if (showKey) "隐藏 API Key" else "显示 API Key",
            onClick = { showKey = !showKey },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = if (expanded) "收起高级 JSON 配置" else "高级 JSON 配置",
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        )
        if (expanded) {
            Spacer(Modifier.height(Dimens.s))
            OutlinedTextField(
                value = advancedJson,
                onValueChange = {
                    advancedJson = it
                    jsonError = advancedJsonError(it)
                },
                label = { Text("高级 JSON 配置") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                shape = MaterialTheme.shapes.medium,
            )
            jsonError?.let {
                Spacer(Modifier.height(Dimens.xs))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = "保存配置",
            onClick = {
                val error = advancedJsonError(advancedJson)
                jsonError = error
                if (error == null) {
                    viewModel.saveCustomModelConfig(apiKey = apiKey, advancedJson = advancedJson)
                    apiKey = ""
                    showKey = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "测试配置（readiness / dry-run）",
            onClick = { viewModel.testAiConfigReadiness() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            text = "删除配置",
            onClick = { confirmDelete = true },
            modifier = Modifier.fillMaxWidth(),
        )
        if (masked?.customConfigured == true) {
            Spacer(Modifier.height(Dimens.s))
            Text("其他模型已配置；完整 API Key 不会显示。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrivacyAndPermissionsSettingsCard() {
    ClassMateCard {
        Text("隐私与权限", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "导入内容、课程资料、AI 配置和学习记录保存在本机；AI 输出入库前需要用户确认。未配置云端时仍可走端侧蓝心或手动编辑。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("本地数据", "课程记录、学习状态、模型配置保存在应用私有目录")
        ProviderStatusRow("用户确认", "图片草稿、转写草稿和 AI 整理结果确认后再进入学习链路")
        ProviderStatusRow("相机 / 文件 / 音频", "仅用于用户主动导入资料；不会在设置页强行申请新权限")
        ProviderStatusRow("云端未配置", "继续端侧处理或手动编辑，不阻断学习")
    }
    PrivacyCard()
}

@Composable
private fun DeveloperSettingsHomeCard(viewModel: AppViewModel) {
    val summary = viewModel.ui.providerConfigSummary
    ClassMateCard {
        Text("开发者设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text("这里只放诊断、smoke dry-run、端侧状态和脱敏日志。普通用户填写 AI Key 的主入口在通用设置 → AI 模型配置。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("Provider 诊断", "可查看云端 / 端侧 / official provider readiness")
        ProviderStatusRow("Official Provider Smoke", "默认 dry-run；真实网络 smoke 需要显式授权")
        ProviderStatusRow("端侧模型状态", viewModel.ui.onDeviceDiagnostic?.status?.displayZh ?: "未知")
        ProviderStatusRow("qwen profile", cloudQualityProfileStatus())
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = if (viewModel.ui.officialProviderDiagnosticsRunning) "官方服务 dry-run 中" else "运行官方服务 dry-run",
            onClick = { viewModel.runOfficialProviderDryRun() },
            modifier = Modifier.fillMaxWidth(),
        )
        OfficialDryRunResults(viewModel.ui.officialProviderDiagnostics)
        ProviderStatusRow("云端模型", if (summary.blueLmConfigured || summary.compatibleConfigured) "已配置" else "未配置")
        ProviderStatusRow("密钥显示", "不显示完整 key，仅显示 configured / missing 或脱敏状态")
    }
}

@Composable
private fun OfficialDryRunResults(results: List<ProviderDryRunResult>) {
    if (results.isEmpty()) return
    Spacer(Modifier.height(Dimens.xs))
    results.forEach { result ->
        ProviderStatusRow(result.capability, result.displayLine())
    }
    Spacer(Modifier.height(Dimens.xs))
    Text(
        "dry-run 只显示分类和脱敏状态；缺配置输出 SKIP，不会把本地兜底写成官方成功。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun advancedJsonError(text: String): String? =
    if (text.isBlank()) {
        null
    } else {
        runCatching { settingsJson.parseToJsonElement(text).jsonObject }
            .fold(onSuccess = { null }, onFailure = { "JSON 格式不正确，请输入合法对象。" })
    }

private val settingsJson = Json { ignoreUnknownKeys = true; isLenient = true }

@Composable
private fun ModelAccessNotesCard(viewModel: AppViewModel) {
    val captureStatus = viewModel.captureConfigStatus()
    ClassMateCard {
        Text("AI 路由说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text("云端优先，端侧兜底，用户确认后再写入学习资料。官方 OCR / ASR 未配置时，仍可继续编辑草稿或粘贴转写文本。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.xs))
        ProviderStatusRow("TTS / 音频生成", "未配置时导出课程精华脚本文本")
        ProviderStatusRow("官方 OCR / ASR", "${captureStatus.labelZh()}；未配置时可继续编辑图片草稿或粘贴转写文本")
        ProviderStatusRow("翻译辅助学习", "保留 derived note，不修改原始证据")
        ProviderStatusRow("端侧文本安全审核", "不可用时提示，不阻断核心学习")
        ProviderStatusRow("检索增强", "查询改写 / 文本相似度 / 文本向量未配置时，本地证据检索继续可用")
        Spacer(Modifier.height(Dimens.xs))
        Text("这里仅显示配置是否可用，不显示任何密钥内容。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OfficialProviderReadinessCard(includeDevLab: Boolean) {
    val product = VivoOfficialProviderRegistry.productFacing
    val smoke = VivoOfficialProviderRegistry.smokeOnly
    ClassMateCard {
        Text(
            if (includeDevLab) "Official provider smoke" else "Official provider readiness",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "Product-facing ${product.size}; cloud first, on-device fallback, manual or safe fallback. Status is value-only and no keys are shown.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        product.forEach { capability ->
            ProviderStatusRow(capability.userVisibleLabel, "${capability.status.name} · docId ${capability.docId}")
        }
        if (includeDevLab) {
            Spacer(Modifier.height(Dimens.s))
            Text("Dev-lab smoke-only ${smoke.size}; not part of the default learning path.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            smoke.forEach { capability ->
                ProviderStatusRow(capability.displayName, "smoke-only · docId ${capability.docId}")
            }
            Spacer(Modifier.height(Dimens.xs))
            Text("Dry-run: scripts/qa/official_provider_smoke.ps1 -DryRun. Network smoke requires explicit authorization and is not run by default.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BackgroundAudioPolicyCard() {
    ClassMateCard {
        Text("沉浸背景音", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text("沉浸学习已内置 ${AmbientSoundCatalog.all.size} 种授权循环背景音，进入心流学习后可选择、暂停、循环播放并调节音量。背景音只在本地播放，不录音、不上传。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        AmbientSoundCatalog.all.forEach { sound ->
            ProviderStatusRow(sound.displayName, "${sound.sceneName} · ${if (sound.attributionRequired) "需署名" else "免署名"} · ${sound.licenseName}")
        }
        Spacer(Modifier.height(Dimens.xs))
        Text("授权记录见 docs/current/ambient_audio_assets.md。背景音不同于课程精华音频脚本，也不使用具体人物音色。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LearningExportSettingsCard(viewModel: AppViewModel) {
    val audio = viewModel.ui.courseEssenceAudioResult
    val safety = viewModel.ui.textSafetyResult
    ClassMateCard {
        Text("导出设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text("默认保留用户确认、证据校验和脱敏导出。练习、复习、报告和课程精华脚本都来自本节课证据。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("默认练习题数量", "8 题，可按课堂资料不足自动减少")
        ProviderStatusRow("默认题目难度", "easy / medium / hard 混合，优先覆盖薄弱知识点")
        ProviderStatusRow("默认复习优先级", "错题、薄弱标记、久未复习、证据复核优先")
        ProviderStatusRow("默认导出格式", "Markdown/Text、HTML、PDF、Word / DOCX、音频脚本")
        ProviderStatusRow("报告内容", "包含 evidence、practice、weakness、review plan、source metadata、bilingual notes")
        ProviderStatusRow("文本安全检查", safety?.status?.name ?: "未运行")
        ProviderStatusRow("课程精华音频", audio?.status?.name ?: "可生成脚本文本")
        ProviderStatusRow("翻译辅助学习", "面向 evidence / knowledge point 的双语注记")
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = "生成听背脚本",
            onClick = { viewModel.generateCourseEssenceAudioScript() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LearningExportDocxPolicyCard(viewModel: AppViewModel) {
    val audio = viewModel.ui.courseEssenceAudioResult
    val safety = viewModel.ui.textSafetyResult
    ClassMateCard {
        Text("导出格式与报告内容", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "导出流程为：AI 提炼学习报告草稿 → 选择格式 → 保存或分享。云端或端侧不可用时，仍可使用本地模板整理版本。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("默认格式", "PDF、Word / DOCX、HTML、Markdown、Text、课程精华音频脚本")
        ProviderStatusRow("Word / DOCX", "真实 OpenXML 文档，适合继续编辑和分享")
        ProviderStatusRow("默认包含", "evidence、practice result、weakness、review plan、source metadata、audio script")
        ProviderStatusRow("课程精华音频", audio?.status?.name ?: "TTS 未配置时导出 script-only")
        ProviderStatusRow("文本安全检查", safety?.status?.name ?: "未运行")
        ProviderStatusRow("翻译辅助学习", "作为 derived note 附加，不修改原始 evidence")
        ProviderStatusRow("边界", "不会模拟具体人物声音；背景音后续使用授权循环音频素材")
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = "生成报告草稿",
            onClick = { viewModel.prepareRefinedExportDraft() },
            modifier = Modifier.fillMaxWidth(),
        )
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
        // 5) Review notifications (Android 13+).
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
            "端侧 BlueLM 3B 是云端不可用时的端侧模型路径；端侧也不可用时仅降级到安全占位（防止空结果或崩溃），不影响主流程。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        ProviderStatusRow("SDK", if (diag?.sdkPresent == true) "已发现" else "未检测到本地 SDK 文件")
        ProviderStatusRow("状态", diag?.status?.displayZh ?: "未知")
        PermissionStatusRow("模型目录访问", ui.onDevicePermissions.allFilesAccess)
        diag?.initState?.let { ProviderStatusRow("初始化", it.displayZh) }
        diag?.generateState?.let { ProviderStatusRow("文本生成", it.displayZh) }
        diag?.errorCode?.let { ProviderStatusRow("错误码", it) }
        ProviderStatusRow("安全占位", if (diag?.fallbackAvailable != false) "就绪（模型全部不可用时）" else "不可用")

        // System speech-recognition readiness — helps diagnose "本机语音识别不可用" (permission / service / locale).
        run {
            val asrReadiness = com.classmate.app.asr.SpeechRecognitionReadiness(
                recordAudioGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.RECORD_AUDIO,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
                recognizerAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context),
                locale = java.util.Locale.getDefault().toLanguageTag(),
            )
            ProviderStatusRow("系统语音识别", asrReadiness.diagnosticsLine())
        }

        // Official WebSocket ASR (/asr/v2, docId 1738/1740) readiness — honest config-gated status. The
        // WebSocket transport (OkHttp) is present; the official path needs credentials + real-device
        // validation. When unavailable, recordings remain saved and the user can paste/edit transcript text;
        // system speech recognition is only an optional device fallback. Never shows the key.
        ProviderStatusRow(
            "官方实时转写（WebSocket）",
            "通道已就绪 · 需配置官方密钥后启用 · 未配置/失败时保留录音并转手动转写，系统实时识别仅为可选 fallback（待真机验证）",
        )

        // Official TTS (wss://.../tts, docId 1735): WS provider + PCM→WAV present; uses the cloud AppKey,
        // falls back to system TTS then script. Honest, never the key.
        ProviderStatusRow(
            "官方 TTS（WebSocket）",
            "通道已就绪 · 听背音频优先官方 TTS · 未配置/失败自动用系统 TTS，再失败保留文稿（待真机验证）",
        )

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
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "${OnDeviceLlmConfig.DEFAULT_MODEL_DIR} 位于共享存储：需先在上方授予「全文件访问」权限，App 才能读取模型文件。" +
                "未授权时端侧会显示 PERMISSION_MISSING，此时分析会自动改用云端或本地基础整理，不会卡住。",
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
            text = if (ui.onDeviceDiagnosticRunning) "诊断中" else "端侧文本测试",
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
            text = if (ui.onDeviceMultimodalRunning) "诊断中" else "端侧多模态",
            onClick = { viewModel.testOnDeviceMultimodal(perms.snapshot()) },
            enabled = !ui.onDeviceMultimodalRunning,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = if (ui.onDeviceMultimodalRunning) "诊断中" else "图片测试",
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
            "端侧 BlueLM 3B 已接入核心学习闭环：云端不可用时可作为端侧模型路径接管。路径：云端蓝心 → 端侧蓝心 → 安全占位。",
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
            text = if (ui.onDeviceAnalysisCheckRunning) "检查中" else "端侧课程分析",
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

private fun cloudQualityProfileStatus(): String {
    val profile = CloudModelQualityProfile.DEEP_STUDY
    return "qwen3.5-plus / ${profile.name} / enable_thinking=true if supported / reasoning_effort=${profile.reasoningEffort.wireValue}"
}

private fun configuredStatus(configured: Boolean, missing: String = "missing"): String =
    if (configured) "configured" else missing

private fun displayCloudModelName(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    return when {
        trimmed.isBlank() -> "qwen3.5-plus"
        trimmed.contains("Seed-2.0", ignoreCase = true) -> "qwen3.5-plus"
        else -> trimmed
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
        Text("能力状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        RoadmapGroup(
            "已支持",
            listOf(
                "云端蓝心分析（官方 BlueLM）",
                "端侧蓝心（端侧 BlueLM 3B）",
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
                "端侧 BlueLM 3B 真机能力复核",
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
private fun SelectableChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) cs.primaryContainer else cs.surfaceVariant,
        contentColor = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        modifier = modifier.defaultMinSize(minHeight = 36.dp).clickable { onClick() },
    ) {
        ClassMateChipText(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
private fun ProviderStatusRow(name: String, status: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 34.dp)
            .padding(vertical = Dimens.xxs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f),
        )
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
            text = if (compatibleRunning) "测试中" else "测试模型接口",
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
