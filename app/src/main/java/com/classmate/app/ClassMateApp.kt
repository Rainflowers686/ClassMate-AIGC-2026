package com.classmate.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.classmate.app.data.EvidenceAssetStore
import com.classmate.app.data.FileHistoryStore
import com.classmate.app.data.FileExportStore
import com.classmate.app.data.FileSnapshotIo
import com.classmate.app.data.L3PersistenceRepository
import com.classmate.app.data.LocalSemanticIndexRepository
import com.classmate.app.data.ThemePreferenceRepository
import com.classmate.app.l3.AndroidClassroomAudioRecorder
import com.classmate.app.l3.AndroidLocalTtsPlayer
import com.classmate.app.capture.CaptureGateway
import com.classmate.app.l3.OfficialRuntimeGatewayFactory
import com.classmate.app.navigation.ClassMateNavHost
import com.classmate.app.ondevice.OnDeviceLlmController
import com.classmate.app.platform.CaptureConfigLoader
import com.classmate.app.platform.ModelConfigRepository
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.state.Tab
import com.classmate.app.ui.i18n.Strings
import com.classmate.app.ui.i18n.appStrings
import com.classmate.app.ui.theme.ClassMateTheme
import com.classmate.core.learning.LearningStore
import java.io.File
import kotlinx.coroutines.delay

private val TAB_ROOTS = setOf(Screen.HOME, Screen.IMPORT, Screen.REVIEW, Screen.HISTORY, Screen.SETTINGS)

@Composable
fun ClassMateApp() {
    val context = LocalContext.current.applicationContext
    val viewModel: AppViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val exportDir = context.getExternalFilesDir("exports") ?: File(context.filesDir, "exports")
                // Single credential file shared by the model config, capture (OCR/ASR) and the official
                // runtime gateway: on a real device config.local.json is effectively absent, so the
                // Settings-saved AppID/AppKey here is the one source the cloud capabilities read.
                val modelConfigFile = File(context.filesDir, "classmate_model_config.json")
                AppViewModel(
                    historyStore = FileHistoryStore(File(context.filesDir, "classmate_history.json")),
                    learningStore = LearningStore(FileSnapshotIo(File(context.filesDir, "classmate_learning_state.json"))),
                    exportStore = FileExportStore(exportDir),
                    // Persist the official-model config to app-private storage (survives restart;
                    // never committed — see .gitignore). On-device BlueLM 3B uses the honest
                    // missing-SDK bridge until app/libs/llm-sdk-release.aar is bundled.
                    modelConfigRepository = ModelConfigRepository(modelConfigFile),
                    // Capture + official runtime read the SAME Settings credential (model-config file)
                    // as a fallback to config.local.json, so OCR/ASR aren't stuck "未配置" on device.
                    captureGatewayProvider = { CaptureGateway(configLoader = CaptureConfigLoader(modelConfigFile = modelConfigFile)) },
                    officialRuntimeGateway = OfficialRuntimeGatewayFactory.production(
                        configLoader = CaptureConfigLoader(modelConfigFile = modelConfigFile),
                    ),
                    themePreferenceRepository = ThemePreferenceRepository(File(context.filesDir, "classmate_theme_preferences.json")),
                    semanticIndexRepository = LocalSemanticIndexRepository(File(context.filesDir, "classmate_semantic_index.json")),
                    l3PersistenceRepository = L3PersistenceRepository(File(context.filesDir, "classmate_l3_store.json")),
                    evidenceAssetStore = EvidenceAssetStore(File(context.filesDir, "classmate_evidence_assets")),
                    classroomAudioRecorder = AndroidClassroomAudioRecorder(File(context.filesDir, "classmate_recordings")),
                    localTtsPlayer = AndroidLocalTtsPlayer(context),
                    // Real reflection bridge: drives the vivo on-device SDK when app/libs/llm-sdk-release.aar
                    // is bundled, and honestly reports SDK_MISSING (no crash) when it is absent.
                    onDeviceController = OnDeviceLlmController.real(),
                )
            }
        },
    )
    val ui = viewModel.ui
    val strings = appStrings(ui.language)
    val darkTheme = ui.darkMode ?: isSystemInDarkTheme()
    val showBottomBar = viewModel.currentScreen in TAB_ROOTS

    ClassMateTheme(
        themePreset = ui.theme,
        accentColor = ui.accentColor,
        customPalette = ui.customPalette,
        typographyPreset = ui.typographyPreset,
        darkTheme = darkTheme,
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavigationDock(
                            currentTab = viewModel.currentTab,
                            strings = strings,
                            onSelect = { viewModel.selectTab(it) },
                        )
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ClassMateNavHost(viewModel)

                    val toast = ui.toast
                    if (toast != null) {
                        LaunchedEffect(toast) {
                            delay(2200)
                            viewModel.consumeToast()
                        }
                        Snackbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                        ) { Text(toast) }
                    }

                    if (ui.aiConfigPrompt.visible) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissAiConfigPrompt() },
                            title = { Text("需要配置云端 AI") },
                            text = {
                                Text(
                                    "配置蓝心大模型后，课堂分析、问答、练习和导出会获得更好的云端效果。" +
                                        "未配置时，你仍可以继续使用端侧蓝心或手动编辑。",
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { viewModel.goToAiConfigFromPrompt() }) {
                                    Text("去设置")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.dismissAiConfigPrompt() }) {
                                    Text("稍后")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationDock(
    currentTab: Tab,
    strings: Strings,
    onSelect: (Tab) -> Unit,
) {
    val themeColors = ClassMateTheme.colors
    val dockShape = RoundedCornerShape(999.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .clip(dockShape),
        shape = dockShape,
        color = themeColors.surfaceContainerLow.copy(alpha = if (themeColors.isDark) 0.74f else 0.9f),
        border = BorderStroke(0.75.dp, themeColors.outline.copy(alpha = if (themeColors.isDark) 0.16f else 0.09f)),
        shadowElevation = if (themeColors.isDark) 0.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 14.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                BottomNavigationDockItem(
                    selected = currentTab == tab,
                    icon = tabIcon(tab),
                    label = tabLabel(tab, strings),
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationDockItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeColors = ClassMateTheme.colors
    val indicator by animateColorAsState(
        targetValue = if (selected) themeColors.primary.copy(alpha = if (themeColors.isDark) 0.78f else 0.72f) else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "bottom-nav-selected-dot",
    )
    val content by animateColorAsState(
        targetValue = if (selected) themeColors.primary else themeColors.textSecondary,
        animationSpec = tween(durationMillis = 180),
        label = "bottom-nav-selected-content",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.006f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "bottom-nav-selected-scale",
    )
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(19.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(indicator),
        )
    }
}

private fun tabIcon(tab: Tab): ImageVector = when (tab) {
    Tab.HOME -> Icons.Filled.Home
    Tab.IMPORT -> Icons.Filled.Add
    Tab.REVIEW -> Icons.Filled.CheckCircle
    Tab.HISTORY -> Icons.Filled.DateRange
    Tab.SETTINGS -> Icons.Filled.Settings
}

private fun tabLabel(tab: Tab, s: Strings): String = when (tab) {
    Tab.HOME -> s.tabHome
    Tab.IMPORT -> s.tabImport
    Tab.REVIEW -> s.tabReview
    Tab.HISTORY -> s.tabHistory
    Tab.SETTINGS -> s.tabSettings
}
