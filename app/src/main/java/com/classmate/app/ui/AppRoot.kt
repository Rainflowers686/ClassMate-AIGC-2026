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

/**
 * Root composable. Dispatches on [ClassMateScreen] — no NavHost, just a
 * `when` over a sealed type. Six screens, linear forward + back chain, no
 * deep-link or process-death restoration needed for v0.3.
 *
 * Adding a new screen requires touching this `when`, ClassMateScreen, and
 * the ViewModel's `back()` chain — three sites, all caught by the compiler.
 */
@Composable
fun AppRoot(viewModel: ClassMateViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    // Hardware back walks the linear chain; on Home it's a no-op so the OS
    // can finish the activity.
    BackHandler(enabled = state.screen != ClassMateScreen.Home) {
        viewModel.back()
    }

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
                }
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
                    viewModel.runSegmentation()
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
                onShowEvidence = { sid, _ ->
                    // We pass the KP through selectKnowledgePoint so the
                    // selected span is available to SegmentCard. The screen
                    // looks up the KP by sourceSegmentId.
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
                onShowEvidence = { sid, _ -> viewModel.showEvidenceFor(sid) },
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
        }
    }
}
