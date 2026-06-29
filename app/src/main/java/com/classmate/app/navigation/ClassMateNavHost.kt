package com.classmate.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.ui.screens.analyze.AnalyzeProgressScreen
import com.classmate.app.ui.screens.evidence.EvidenceDetailScreen
import com.classmate.app.ui.screens.feedback.FeedbackScreen
import com.classmate.app.ui.screens.history.HistoryScreen
import com.classmate.app.ui.screens.home.HomeScreen
import com.classmate.app.ui.screens.importcourse.ImportCourseScreen
import com.classmate.app.ui.screens.importcourse.ImportSettingsScreen
import com.classmate.app.ui.screens.importcourse.MaterialTrayScreen
import com.classmate.app.ui.screens.knowledge.KnowledgeTimelineScreen
import com.classmate.app.ui.screens.live.LiveCompanionScreen
import com.classmate.app.ui.screens.practice.PracticeSessionScreen
import com.classmate.app.ui.screens.transcript.TranscriptEditorScreen
import com.classmate.app.ui.screens.transcript.TranscriptImportScreen
import com.classmate.app.ui.screens.quiz.QuizScreen
import com.classmate.app.ui.screens.review.ReviewPlanScreen
import com.classmate.app.ui.screens.settings.SettingsScreen

/** Lightweight, dependency-free navigation: a Crossfade over the back stack's current screen. */
@Composable
fun ClassMateNavHost(viewModel: AppViewModel) {
    // Single source of truth for system back: protect recordings, walk settings sub-pages, then pop the
    // app back stack — never silently exit from an in-app sub-page.
    BackHandler(enabled = viewModel.canHandleSystemBack) { viewModel.handleSystemBack() }

    if (viewModel.ui.showRecordingBackPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRecordingBackPrompt() },
            title = { Text("正在录音 / 实时转写") },
            text = { Text("当前正在录音并实时转写。停止并保存会保留音频与已识别文本；取消录音不会保存任何内容。") },
            confirmButton = { TextButton(onClick = { viewModel.stopRecordingFromBackPrompt() }) { Text("停止并保存") } },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.cancelRecordingFromBackPrompt() }) { Text("取消录音") }
                    TextButton(onClick = { viewModel.dismissRecordingBackPrompt() }) { Text("留在页面") }
                }
            },
        )
    }

    Crossfade(targetState = viewModel.currentScreen, label = "nav") { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(viewModel)
            Screen.IMPORT -> ImportCourseScreen(viewModel)
            Screen.IMPORT_TRAY -> MaterialTrayScreen(viewModel)
            Screen.IMPORT_SETTINGS -> ImportSettingsScreen(viewModel)
            Screen.TRANSCRIPT_IMPORT -> TranscriptImportScreen(viewModel)
            Screen.TRANSCRIPT_EDITOR -> TranscriptEditorScreen(viewModel)
            Screen.LIVE -> LiveCompanionScreen(viewModel)
            Screen.ANALYZE -> AnalyzeProgressScreen(viewModel)
            Screen.KNOWLEDGE -> KnowledgeTimelineScreen(viewModel)
            Screen.COURSE_DETAIL -> com.classmate.app.ui.screens.course.CourseDetailScreen(viewModel)
            Screen.EVIDENCE -> EvidenceDetailScreen(viewModel)
            Screen.QUIZ -> QuizScreen(viewModel)
            Screen.REVIEW -> ReviewPlanScreen(viewModel)
            Screen.PRACTICE -> PracticeSessionScreen(viewModel)
            Screen.FEEDBACK -> FeedbackScreen(viewModel)
            Screen.HISTORY -> HistoryScreen(viewModel)
            Screen.SETTINGS -> SettingsScreen(viewModel)
        }
    }
}
