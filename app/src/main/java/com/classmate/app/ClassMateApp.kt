package com.classmate.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.classmate.app.data.FileHistoryStore
import com.classmate.app.data.FileExportStore
import com.classmate.app.data.FileSnapshotIo
import com.classmate.app.data.ThemePreferenceRepository
import com.classmate.app.navigation.ClassMateNavHost
import com.classmate.app.ondevice.OnDeviceLlmController
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
                AppViewModel(
                    historyStore = FileHistoryStore(File(context.filesDir, "classmate_history.json")),
                    learningStore = LearningStore(FileSnapshotIo(File(context.filesDir, "classmate_learning_state.json"))),
                    exportStore = FileExportStore(exportDir),
                    // Persist the official-model config to app-private storage (survives restart;
                    // never committed — see .gitignore). On-device BlueLM 3B uses the honest
                    // missing-SDK bridge until app/libs/llm-sdk-release.aar is bundled.
                    modelConfigRepository = ModelConfigRepository(File(context.filesDir, "classmate_model_config.json")),
                    themePreferenceRepository = ThemePreferenceRepository(File(context.filesDir, "classmate_theme_preferences.json")),
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

    ClassMateTheme(themePreset = ui.theme, accentColor = ui.accentColor, darkTheme = darkTheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val themeColors = ClassMateTheme.colors
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showBottomBar) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = themeColors.surfaceContainerLow.copy(alpha = if (themeColors.isDark) 0.88f else 0.96f),
                            border = BorderStroke(1.dp, themeColors.outline.copy(alpha = if (themeColors.isDark) 0.32f else 0.16f)),
                            shadowElevation = if (themeColors.isDark) 0.dp else 8.dp,
                        ) {
                            NavigationBar(
                                modifier = Modifier.height(72.dp),
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                            ) {
                                Tab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = viewModel.currentTab == tab,
                                        onClick = { viewModel.selectTab(tab) },
                                        icon = { Icon(tabIcon(tab), contentDescription = tabLabel(tab, strings)) },
                                        label = { Text(tabLabel(tab, strings), maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = themeColors.primary,
                                            selectedTextColor = themeColors.primary,
                                            indicatorColor = themeColors.primary.copy(alpha = if (themeColors.isDark) 0.18f else 0.12f),
                                            unselectedIconColor = themeColors.textSecondary,
                                            unselectedTextColor = themeColors.textSecondary,
                                        ),
                                    )
                                }
                            }
                        }
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
