package com.classmate.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.ui.screens.analyze.AnalyzeProgressScreen
import com.classmate.app.ui.screens.evidence.EvidenceDetailScreen
import com.classmate.app.ui.screens.feedback.FeedbackScreen
import com.classmate.app.ui.screens.home.HomeScreen
import com.classmate.app.ui.screens.importcourse.ImportCourseScreen
import com.classmate.app.ui.screens.knowledge.KnowledgeTimelineScreen
import com.classmate.app.ui.screens.quiz.QuizScreen
import com.classmate.app.ui.screens.review.ReviewPlanScreen
import com.classmate.app.ui.screens.settings.SettingsScreen

/** Lightweight, dependency-free navigation: a Crossfade over the back stack's current screen. */
@Composable
fun ClassMateNavHost(viewModel: AppViewModel) {
    BackHandler(enabled = viewModel.canGoBack) { viewModel.goBack() }

    Crossfade(targetState = viewModel.currentScreen, label = "nav") { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(viewModel)
            Screen.IMPORT -> ImportCourseScreen(viewModel)
            Screen.ANALYZE -> AnalyzeProgressScreen(viewModel)
            Screen.KNOWLEDGE -> KnowledgeTimelineScreen(viewModel)
            Screen.EVIDENCE -> EvidenceDetailScreen(viewModel)
            Screen.QUIZ -> QuizScreen(viewModel)
            Screen.REVIEW -> ReviewPlanScreen(viewModel)
            Screen.FEEDBACK -> FeedbackScreen(viewModel)
            Screen.SETTINGS -> SettingsScreen(viewModel)
        }
    }
}
