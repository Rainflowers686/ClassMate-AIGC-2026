package com.classmate.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.data.HistoryRecord
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.Pill
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.product.ProductCanvas
import com.classmate.app.ui.product.ProductHero
import com.classmate.app.ui.product.ProductScaffold
import com.classmate.app.ui.product.ProductSpace
import com.classmate.app.ui.product.QuietCard
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.i18n.Strings
import com.classmate.app.ui.i18n.appStrings
import com.classmate.core.library.CourseSummary
import com.classmate.core.learning.LearningSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class CourseLibraryFilter(val label: String) {
    ALL("全部"),
    DUE("有待复习"),
    WEAK("有薄弱点"),
    RECENT("最近学习"),
    OFFICIAL("官方 BlueLM"),
    LOCAL("安全占位"),
}

internal enum class CourseLibrarySort(val label: String) {
    RECENT("最近学习"),
    KP_COUNT("知识点数量"),
    DUE_COUNT("待复习数量"),
    TITLE("课程标题"),
}

@Composable
fun HistoryScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val s = appStrings(ui.language)
    val history = ui.history
    val summaries = viewModel.courseSummaries()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CourseLibraryFilter.ALL) }
    var sort by remember { mutableStateOf(CourseLibrarySort.RECENT) }
    val visibleSummaries = remember(summaries, history, ui.learningSnapshot, query, filter, sort) {
        filterCourseLibrary(
            summaries = summaries,
            history = history,
            learningSnapshot = ui.learningSnapshot,
            query = query,
            filter = filter,
            sort = sort,
        )
    }

    ProductCanvas {
      ProductScaffold(contextLabel = s.historyTitle) { padding ->
        if (history.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxWidth().padding(horizontal = ProductSpace.gutter)) {
                Spacer(Modifier.height(ProductSpace.tight))
                ProductHero(overline = "课程库", title = "还没有课程", subtitle = s.historyEmpty)
            }
            return@ProductScaffold
        }

        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ProductSpace.gutter)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(ProductSpace.block),
        ) {
            Spacer(Modifier.height(ProductSpace.tight))
            ProductHero(overline = "课程库", title = "我的课程", subtitle = "${summaries.size} 门课程 · 按来源筛选与搜索")
            CourseLibraryControls(
                query = query,
                onQueryChange = { query = it },
                filter = filter,
                onFilterChange = { filter = it },
                sort = sort,
                onSortChange = { sort = it },
            )
            if (visibleSummaries.isEmpty()) {
                EmptyFilteredState(onClear = {
                    query = ""
                    filter = CourseLibraryFilter.ALL
                    sort = CourseLibrarySort.RECENT
                })
            }
            visibleSummaries.forEach { summary ->
                CourseCard(summary = summary, s = s, onOpen = { viewModel.openCourse(summary.courseKey) })
            }
        }
      }
    }
}

@Composable
private fun CourseLibraryControls(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: CourseLibraryFilter,
    onFilterChange: (CourseLibraryFilter) -> Unit,
    sort: CourseLibrarySort,
    onSortChange: (CourseLibrarySort) -> Unit,
) {
    QuietCard {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("搜索课程、知识点、科目或来源") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.s))
        Text("筛选", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xxs))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            CourseLibraryFilter.entries.forEach { item ->
                LibraryChoiceChip(item.label, selected = item == filter) { onFilterChange(item) }
            }
        }
        Spacer(Modifier.height(Dimens.s))
        Text("排序", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xxs))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            CourseLibrarySort.entries.forEach { item ->
                LibraryChoiceChip(item.label, selected = item == sort) { onSortChange(item) }
            }
        }
    }
}

@Composable
private fun EmptyFilteredState(onClear: () -> Unit) {
    QuietCard {
        Text("没有找到课程", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text("可以换一个关键词，或清除筛选后查看全部课程。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton("清除筛选", onClick = onClear, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CourseCard(summary: CourseSummary, s: Strings, onOpen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    QuietCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(summary.courseName.ifBlank { s.untitledCourse }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(summary.subject, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Pill(s.lessonsUnit(summary.lessonCount), cs.secondaryContainer, cs.onSecondaryContainer)
        }
        Spacer(Modifier.height(Dimens.s))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            Pill(s.kpUnit(summary.knowledgePointTotal), cs.tertiaryContainer, cs.onTertiaryContainer)
            Pill(s.quizUnit(summary.quizTotal), cs.tertiaryContainer, cs.onTertiaryContainer)
            Pill(s.reviewUnit(summary.dueReviewTaskCount), cs.surfaceVariant, cs.onSurfaceVariant)
        }
        Spacer(Modifier.height(Dimens.s))
        Text(s.recentAt(formatTime(summary.latestLearningTime)), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        Text(
            s.sourceLabel("${summary.recentProvider}${if (summary.recentFallbackUsed) " / " + s.localFallback else ""}"),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun LibraryChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) cs.primaryContainer else cs.surfaceVariant,
        contentColor = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
    }
}

@Composable
private fun HistoryCard(record: HistoryRecord, s: Strings, onOpen: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    QuietCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                record.title.ifBlank { s.untitledCourse },
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = s.deleteCd, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(Dimens.xxs))
        Text(
            "${formatTime(record.createdAtEpochMs)} · ${record.profileLabel} · ${providerLabel(record.providerName, record.fallbackUsed, s)}",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
        if (record.model.isNotBlank()) {
            Text(s.modelLabel(record.model), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.height(Dimens.s))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            Pill(s.kpUnit(record.knowledgePointCount), cs.secondaryContainer, cs.onSecondaryContainer)
            Pill(s.quizUnit(record.quizCount), cs.secondaryContainer, cs.onSecondaryContainer)
            Pill(s.validation(record.validationStatus), cs.tertiaryContainer, cs.onTertiaryContainer)
            if (record.fallbackUsed) Pill(s.localFallback, cs.surfaceVariant, cs.onSurfaceVariant)
        }
    }
}

private fun providerLabel(providerName: String, fallbackUsed: Boolean, s: Strings): String =
    when {
        fallbackUsed -> s.localFallback
        providerName.equals("BLUELM", ignoreCase = true) -> s.providerBlueLM
        providerName.equals("COMPATIBLE", ignoreCase = true) -> s.providerCompatible
        providerName.equals("LOCAL_FALLBACK", ignoreCase = true) -> s.localFallback
        else -> providerName
    }

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

internal fun filterCourseLibrary(
    summaries: List<CourseSummary>,
    history: List<HistoryRecord>,
    learningSnapshot: LearningSnapshot,
    query: String,
    filter: CourseLibraryFilter,
    sort: CourseLibrarySort,
): List<CourseSummary> {
    val nowLatest = summaries.maxOfOrNull { it.latestLearningTime } ?: 0L
    val recentWindowMs = 7L * 24L * 60L * 60L * 1000L
    val normalizedQuery = query.trim().lowercase()
    return summaries
        .filter { summary ->
            normalizedQuery.isBlank() || courseSearchText(summary, history).contains(normalizedQuery)
        }
        .filter { summary ->
            when (filter) {
                CourseLibraryFilter.ALL -> true
                CourseLibraryFilter.DUE -> summary.dueReviewTaskCount > 0
                CourseLibraryFilter.WEAK -> hasWeakTask(summary, learningSnapshot)
                CourseLibraryFilter.RECENT -> summary.latestLearningTime >= nowLatest - recentWindowMs
                CourseLibraryFilter.OFFICIAL -> summary.recentProvider.contains("BlueLM", ignoreCase = true)
                CourseLibraryFilter.LOCAL -> summary.recentFallbackUsed || summary.recentProvider.contains("Local", ignoreCase = true)
            }
        }
        .let { list ->
            when (sort) {
                CourseLibrarySort.RECENT -> list.sortedByDescending { it.latestLearningTime }
                CourseLibrarySort.KP_COUNT -> list.sortedByDescending { it.knowledgePointTotal }
                CourseLibrarySort.DUE_COUNT -> list.sortedByDescending { it.dueReviewTaskCount }
                CourseLibrarySort.TITLE -> list.sortedBy { it.courseName.lowercase() }
            }
        }
}

private fun courseSearchText(summary: CourseSummary, history: List<HistoryRecord>): String {
    val key = summary.courseKey
    val related = history.filter { com.classmate.core.library.CourseLibraryBuilder.normalizeCourseName(it.title).lowercase() == key }
    val kpTitles = related.flatMap { it.result.knowledgePoints.map { kp -> kp.title } }
    val providers = related.flatMap { listOf(it.providerName, it.profileLabel, providerSearchLabel(it.providerName, it.fallbackUsed)) }
    return (listOf(summary.courseName, summary.subject, summary.recentProvider) + kpTitles + providers)
        .joinToString(" ")
        .lowercase()
}

private fun hasWeakTask(summary: CourseSummary, learningSnapshot: LearningSnapshot): Boolean =
    learningSnapshot.tasks.any { task ->
        com.classmate.core.library.CourseLibraryBuilder.normalizeCourseName(task.courseTitle).lowercase() == summary.courseKey &&
            !task.manuallyRemoved &&
            (task.counters.wrongAnswer > 0 || task.counters.tooHard > 0 || task.counters.needExample > 0 || task.needsHumanReview)
    }

private fun providerSearchLabel(providerName: String, fallbackUsed: Boolean): String =
    when {
        fallbackUsed -> "安全占位 SafetyPlaceholder"
        providerName.equals("BLUELM", ignoreCase = true) -> "BlueLM official 官方 BlueLM"
        providerName.equals("COMPATIBLE", ignoreCase = true) -> "Compatible demo"
        providerName.equals("LOCAL_FALLBACK", ignoreCase = true) -> "安全占位 SafetyPlaceholder"
        else -> providerName
    }
