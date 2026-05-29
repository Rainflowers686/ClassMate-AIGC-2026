package com.classmate.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classmate.app.state.ClassMateScreen
import com.classmate.app.state.ClassMateViewModel
import com.classmate.app.ui.screens.AnalyzeScreen
import com.classmate.app.ui.screens.CourseInputScreen
import com.classmate.app.ui.screens.HomeScreen
import com.classmate.app.ui.screens.HotwordScreen
import com.classmate.app.ui.screens.QuizScreen
import com.classmate.app.ui.screens.ReviewPlanScreen
import com.classmate.app.ui.screens.SettingsScreen
import com.classmate.app.ui.screens.TimelineScreen
import com.classmate.app.ui.theme.ClassMateTheme

/**
 * Root composable. Dispatches on the sealed [ClassMateScreen]; the ViewModel
 * owns navigation state so process-death survival reduces to "remember
 * what screen you were on" (v0.5 task).
 *
 * The theme wrapper lives here so theme changes propagate through every
 * screen with a single recomposition.
 */
@Composable
fun AppRoot(viewModel: ClassMateViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    BackHandler(enabled = state.screen != ClassMateScreen.Home) {
        viewModel.back()
    }

    ClassMateTheme(themeId = state.themeId) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (state.screen) {
                ClassMateScreen.Home -> HomeScreen(
                    state = state,
                    onStart = { viewModel.navigateTo(ClassMateScreen.CourseInput) },
                    onLoadDemo = {
                        viewModel.loadDemoInput()
                        viewModel.navigateTo(ClassMateScreen.CourseInput)
                    },
                    onOpenSettings = { viewModel.navigateTo(ClassMateScreen.Settings) }
                )

                ClassMateScreen.CourseInput -> CourseInputScreen(
                    state = state,
                    onTitleChange = viewModel::updateCourseTitle,
                    onTextChange = viewModel::updateCourseText,
                    onLoadDemo = viewModel::loadDemoInput,
                    onBack = viewModel::back,
                    onNext = { viewModel.navigateTo(ClassMateScreen.Hotword) }
                )

                ClassMateScreen.Hotword -> HotwordScreen(
                    state = state,
                    onPendingChange = viewModel::updatePendingHotword,
                    onAdd = viewModel::addHotword,
                    onRemove = viewModel::removeHotword,
                    onBack = viewModel::back,
                    onNext = {
                        // Only re-run Segmenter if state.segments is empty; the
                        // demo loader already filled them with verbatim ids.
                        if (state.segments.isEmpty()) viewModel.runSegmentation()
                        viewModel.navigateTo(ClassMateScreen.Analyze)
                    }
                )

                ClassMateScreen.Analyze -> AnalyzeScreen(
                    state = state,
                    onRunSegment = viewModel::runSegmentation,
                    onRunAnalyze = viewModel::runAnalysis,
                    onBack = viewModel::back,
                    onNext = { viewModel.navigateTo(ClassMateScreen.Timeline) }
                )

                ClassMateScreen.Timeline -> TimelineScreen(
                    state = state,
                    onShowEvidence = { sid ->
                        val kp = state.knowledgePoints.firstOrNull { it.sourceSegmentId == sid }
                        viewModel.selectKnowledgePoint(kp)
                        viewModel.showEvidenceFor(sid)
                    },
                    onCloseEvidence = {
                        viewModel.selectKnowledgePoint(null)
                        viewModel.showEvidenceFor(null)
                    },
                    onBack = viewModel::back,
                    onNext = { viewModel.navigateTo(ClassMateScreen.Quiz) }
                )

                ClassMateScreen.Quiz -> QuizScreen(
                    state = state,
                    onSubmit = viewModel::submitQuizAnswer,
                    onShowEvidence = { sid -> viewModel.showEvidenceFor(sid) },
                    onCloseEvidence = { viewModel.showEvidenceFor(null) },
                    onPrev = viewModel::previousQuiz,
                    onNext = viewModel::nextQuiz,
                    onBack = { viewModel.navigateTo(ClassMateScreen.Timeline) },
                    onReviewPlan = { viewModel.navigateTo(ClassMateScreen.ReviewPlan) }
                )

                ClassMateScreen.ReviewPlan -> ReviewPlanScreen(
                    state = state,
                    onBackToTimeline = { viewModel.navigateTo(ClassMateScreen.Timeline) },
                    onHome = {
                        viewModel.resetSession()
                        viewModel.navigateTo(ClassMateScreen.Home)
                    }
                )

                ClassMateScreen.Settings -> SettingsScreen(
                    state = state,
                    onThemeChange = viewModel::setTheme,
                    onBack = viewModel::back
                )
            }
        }
    }
}
