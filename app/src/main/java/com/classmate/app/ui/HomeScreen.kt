package com.classmate.app.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.data.ApiConfigRepository
import com.classmate.app.data.DemoInputRepository
import com.classmate.core.adapter.DemoProvider
import com.classmate.core.adapter.ModelProvider
import com.classmate.core.evidence.EvidenceValidationResult
import com.classmate.core.evidence.EvidenceValidator
import com.classmate.core.logging.RedactedLogger
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.KnowledgePoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val LOG_TAG = "ClassMateLog"
private const val UI_LOG_TAG = "ClassMateUI"

/**
 * v0.2.5 probe home screen.
 *
 * UI is deliberately bare: a title, a button, a list. The point is to prove
 * the data path — assets -> DemoProvider -> EvidenceValidator -> render —
 * works end-to-end, not to look polished.
 */
@Composable
fun HomeScreen(context: Context) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<HomeState>(HomeState.Idle) }
    var configHint by remember { mutableStateOf("") }

    // Surface the resolved provider name on first composition so the user can
    // see whether config.local.json was picked up before they press the button.
    LaunchedEffect(Unit) {
        val cfg = ApiConfigRepository.load(context)
        configHint = "provider=${cfg.provider} (config.local.json ${if (cfg.loadedFromLocalFile) "found" else "missing — using example defaults"})"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "ClassMate v0.2.5 Probe",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = configHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    state = HomeState.Loading
                    scope.launch {
                        state = runDemoPipeline(context)
                    }
                }
            ) {
                Text("加载 Demo 输出 / Load demo output")
            }

            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                HomeState.Idle -> Text(
                    "点击按钮以加载 demo_output.json 并展示知识点。",
                    style = MaterialTheme.typography.bodyMedium
                )

                HomeState.Loading -> Text(
                    "Loading…",
                    style = MaterialTheme.typography.bodyMedium
                )

                is HomeState.Error -> Text(
                    "Error: ${s.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                is HomeState.Loaded -> LoadedView(s)
            }
        }
    }
}

@Composable
private fun LoadedView(s: HomeState.Loaded) {
    Column {
        Text(
            text = s.result.course_title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = s.result.summary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        ValidationBadge(s.validation)
        Spacer(Modifier.height(8.dp))
        Text(
            "知识点 (${s.knowledgePoints.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(s.knowledgePoints) { kp ->
                KnowledgePointCard(kp)
            }
        }
    }
}

@Composable
private fun ValidationBadge(v: EvidenceValidationResult) {
    val ok = v.schemaPassed && v.spanMismatches.isEmpty()
    val color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Text(
        text = "Evidence chain: schemaPassed=${v.schemaPassed}, " +
            "spanMatchRate=${"%.2f".format(v.evidenceMatchRate)}, " +
            "missingRefs=${v.missingKpSegmentRefs.size + v.missingQuizSegmentRefs.size + v.missingRelatedKpRefs.size}, " +
            "spanMismatches=${v.spanMismatches.size}",
        style = MaterialTheme.typography.bodySmall,
        color = color
    )
}

@Composable
private fun KnowledgePointCard(kp: KnowledgePoint) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                kp.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Importance ${kp.importance}/5  •  Difficulty ${kp.difficulty}/5  •  ${kp.source_segment_id}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                kp.explanation,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private sealed interface HomeState {
    data object Idle : HomeState
    data object Loading : HomeState
    data class Loaded(
        val result: CourseAnalysisResult,
        val knowledgePoints: List<KnowledgePoint>,
        val validation: EvidenceValidationResult
    ) : HomeState

    data class Error(val message: String) : HomeState
}

/**
 * Orchestrates one probe run: load demo input + output, invoke the demo
 * provider, validate evidence, emit a redacted log line.
 *
 * Failure path (per spec §14): swallow exceptions, surface a user-facing
 * message, never crash.
 */
private suspend fun runDemoPipeline(context: Context): HomeState {
    val started = System.currentTimeMillis()
    val redactedLogger = RedactedLogger { line -> Log.i(LOG_TAG, line) }
    return try {
        val input = DemoInputRepository.loadDemoInput(context)
        val demoJson = DemoInputRepository.loadDemoOutputRaw(context)
        val provider: ModelProvider = DemoProvider(demoJson)
        val result = provider.analyzeCourse(input)
        val validation = EvidenceValidator.validate(input, result)
        val elapsed = System.currentTimeMillis() - started

        redactedLogger.courseAnalysisCall(
            timestamp = isoNow(),
            provider = provider.name,
            inputSegmentCount = input.segments.size,
            hotwordCount = input.hotwords.size,
            success = true,
            latencyMs = elapsed,
            schemaValid = validation.schemaPassed,
            evidenceMatchRate = validation.evidenceMatchRate,
            errorType = null
        )

        val kps = result.segments.flatMap { it.knowledge_points }
        HomeState.Loaded(result = result, knowledgePoints = kps, validation = validation)
    } catch (t: Throwable) {
        Log.w(UI_LOG_TAG, "demo pipeline failed", t)
        val elapsed = System.currentTimeMillis() - started
        redactedLogger.courseAnalysisCall(
            timestamp = isoNow(),
            provider = "demo",
            inputSegmentCount = 0,
            hotwordCount = 0,
            success = false,
            latencyMs = elapsed,
            schemaValid = false,
            evidenceMatchRate = null,
            errorType = t::class.simpleName ?: "Unknown"
        )
        HomeState.Error(t.message ?: t::class.simpleName ?: "unknown error")
    }
}

private fun isoNow(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    return fmt.format(Date())
}
