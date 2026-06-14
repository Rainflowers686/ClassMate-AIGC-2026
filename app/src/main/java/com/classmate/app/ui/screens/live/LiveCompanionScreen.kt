package com.classmate.app.ui.screens.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.classmate.app.asr.AndroidSpeechRecognizerClient
import com.classmate.app.asr.AsrEventListener
import com.classmate.app.asr.AsrState
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.BreathingTimerRing
import com.classmate.app.ui.components.ChipTone
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.EmptyStateCard
import com.classmate.app.ui.components.FlowAccent
import com.classmate.app.ui.components.FlowSceneCard
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.components.StatusChip
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.design.Radii
import com.classmate.app.ui.flow.FlowScenes
import com.classmate.app.ui.screens.transcript.SpeakerSelector
import com.classmate.core.live.TranscriptStatus
import com.classmate.core.material.zhLabel
import com.classmate.core.transcript.TranscriptClock
import com.classmate.core.transcript.zhLabelOrNull
import kotlinx.coroutines.delay

@Composable
fun LiveCompanionScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val session = ui.liveTranscript
    val status = session?.status ?: TranscriptStatus.IDLE
    val running = status == TranscriptStatus.RUNNING

    // Live-ticking clock while recording, so the timer feels alive. Stops when not running.
    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running) {
        while (running) {
            nowTick = System.currentTimeMillis()
            delay(1000)
        }
    }
    var selectedScene by remember { mutableStateOf(FlowScenes.all.first().id) }

    // --- experimental system ASR wiring (no raw audio saved, no upload, no background recording) ---
    val context = LocalContext.current
    val asr = viewModel.ui.asrSession
    val engine = remember { AndroidSpeechRecognizerClient(context) }
    val listener = remember {
        object : AsrEventListener {
            override fun onListening() = viewModel.asrOnListening()
            override fun onPartial(text: String) = viewModel.asrOnPartial(text)
            override fun onFinal(text: String, confidence: Double?) = viewModel.asrOnFinal(text, confidence)
            override fun onEndOfSpeech() = viewModel.asrOnEndOfSpeech()
            override fun onError(message: String) { viewModel.asrOnError(message); engine.stop() }
        }
    }
    DisposableEffect(Unit) { onDispose { engine.destroy() } }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && viewModel.asrBegin(engine.isAvailable(), true) == AsrState.LISTENING) {
            engine.start(listener)
        } else if (!granted) {
            viewModel.asrBegin(engine.isAvailable(), permissionGranted = false)
        }
    }
    val startAsr = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        when {
            !engine.isAvailable() -> viewModel.asrBegin(available = false, permissionGranted = granted)
            granted -> if (viewModel.asrBegin(true, true) == AsrState.LISTENING) { engine.start(listener); AsrState.LISTENING } else AsrState.IDLE
            else -> { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); AsrState.PERMISSION_REQUIRED }
        }
    }

    ClassMateScaffold(title = "Live Companion", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            // --- Flow header ---
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(Radii.heroShape)
                    .background(Brush.linearGradient(FlowAccent.gradient))
                    .padding(Dimens.xl),
            ) {
                Column {
                    Text("课堂伴学 · Live", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(Dimens.xxs))
                    Text(
                        "手动转写 + 实时转写实验（系统语音识别）；不保存原始音频、不后台录音、不做声纹识别。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }

            // --- Timer + state machine ---
            ClassMateCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    BreathingTimerRing(
                        elapsedLabel = formatElapsed(session?.elapsedMs(nowTick) ?: 0L),
                        statusLabel = statusZh(status),
                        running = running,
                    )
                    Spacer(Modifier.height(Dimens.m))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                    ) {
                        StatusChip(statusZh(status), tone = statusTone(status))
                        StatusChip("片段 ${session?.segments?.size ?: 0}", tone = ChipTone.NEUTRAL)
                        StatusChip("手动转写", tone = ChipTone.INFO)
                        if (ui.liveAnalyzed) StatusChip("已生成时间线", tone = ChipTone.SUCCESS)
                    }
                }
            }

            OutlinedTextField(
                value = ui.liveTitleDraft,
                onValueChange = viewModel::updateLiveTitle,
                label = { Text("课程标题 Course title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                PrimaryButton(
                    text = if (session == null) "开始课堂" else "重新开始",
                    onClick = { viewModel.startLiveClass() },
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = if (status == TranscriptStatus.PAUSED) "继续" else "暂停",
                    onClick = {
                        if (status == TranscriptStatus.PAUSED) viewModel.resumeLiveClass() else viewModel.pauseLiveClass()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = session != null && status != TranscriptStatus.ENDED,
                )
            }

            // --- Append a segment ---
            ClassMateCard {
                Text("说话人（手动标注，未做声纹识别）", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xs))
                SpeakerSelector(selected = ui.liveSpeakerDraft, onSelect = { viewModel.setLiveSpeaker(it) })
                Spacer(Modifier.height(Dimens.s))
                OutlinedTextField(
                    value = ui.liveSegmentDraft,
                    onValueChange = viewModel::updateLiveSegment,
                    label = { Text("追加课堂片段 / 笔记（当前说话人：${ui.liveSpeakerDraft.zhLabel()}）") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = "追加片段",
                    onClick = { viewModel.appendLiveSegment() },
                    enabled = session != null && status != TranscriptStatus.ENDED && ui.liveSegmentDraft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Experimental live ASR ---
            ClassMateCard {
                Text("实时转写实验", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xs))
                Text(
                    "使用系统语音识别能力，效果依设备而定。不会保存原始音频，不会后台录音，不做声纹识别。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                ) {
                    StatusChip(asrStateZh(asr.state), tone = asrStateTone(asr.state))
                    StatusChip("已确认 ${asr.confirmedCount}", tone = ChipTone.NEUTRAL)
                }
                if (asr.partialText.isNotBlank()) {
                    Spacer(Modifier.height(Dimens.xs))
                    Text("识别中：${asr.partialText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                when (asr.state) {
                    AsrState.UNSUPPORTED -> AsrHint("不支持系统语音识别，请使用手动转写或导入字幕。")
                    AsrState.PERMISSION_REQUIRED -> AsrHint("未授权麦克风，仍可手动记录或导入转写稿。")
                    AsrState.ERROR -> AsrHint(asr.errorMessage ?: "系统语音识别出错，请改用手动转写。")
                    else -> {}
                }
                Spacer(Modifier.height(Dimens.s))
                Text("说话人（手动标注）", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xxs))
                SpeakerSelector(selected = ui.liveSpeakerDraft, onSelect = { viewModel.setLiveSpeaker(it) })
                Spacer(Modifier.height(Dimens.s))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    if (asr.isActive) {
                        SecondaryButton("暂停", onClick = { engine.stop(); viewModel.pauseAsr() }, modifier = Modifier.weight(1f))
                        SecondaryButton("停止", onClick = { engine.stop(); viewModel.stopAsr() }, modifier = Modifier.weight(1f))
                    } else if (asr.state == AsrState.PAUSED) {
                        PrimaryButton("继续", onClick = { viewModel.resumeAsr(); engine.start(listener) }, modifier = Modifier.weight(1f))
                        SecondaryButton("停止", onClick = { engine.stop(); viewModel.stopAsr() }, modifier = Modifier.weight(1f))
                    } else {
                        PrimaryButton("开始实时转写", onClick = { startAsr() }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = "转为手动编辑",
                    onClick = { engine.stop(); viewModel.asrToManualEdit() },
                    enabled = asr.confirmedCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (asr.segments.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.s))
                    Text("已确认片段", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.xxs))
                    asr.segments.takeLast(5).forEach {
                        val time = it.startMs?.let { ms -> TranscriptClock.format(ms) + " · " } ?: ""
                        val speaker = it.speaker.zhLabelOrNull()?.let { sp -> "[$sp] " } ?: ""
                        Text("$time$speaker${it.text}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(Dimens.xxs))
                    }
                }
                Spacer(Modifier.height(Dimens.xs))
                Text("系统不支持或失败时，请改用手动转写或字幕导入。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // --- Recent segments ---
            val segments = session?.segments.orEmpty()
            if (segments.isEmpty()) {
                EmptyStateCard(
                    title = "还没有片段",
                    message = "开始课堂后，手动追加你听到的要点；结束时会拼成课堂文本进入分析。",
                )
            } else {
                ClassMateCard {
                    Text("最近片段", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.s))
                    segments.takeLast(5).forEach {
                        val time = it.startMs?.let { ms -> TranscriptClock.format(ms) + " · " } ?: ""
                        val speaker = it.speaker.zhLabelOrNull()?.let { sp -> "[$sp] " } ?: ""
                        Text("${it.index}. $time$speaker${it.text}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Dimens.xxs))
                    }
                }
            }

            // --- Flow ambience (visual only) ---
            ClassMateCard {
                Text("心流氛围 Flow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xs))
                Text(
                    FlowScenes.DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                ) {
                    FlowScenes.all.forEach { scene ->
                        FlowSceneCard(
                            scene = scene,
                            selected = scene.id == selectedScene,
                            onClick = { selectedScene = scene.id },
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.s))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                ) {
                    FlowScenes.mixerChannels.forEach { ch ->
                        StatusChip("$ch · 占位", tone = ChipTone.NEUTRAL)
                    }
                }
            }

            // --- End / analyze ---
            val canGenerate = viewModel.canGenerateLiveTimeline()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                SecondaryButton(
                    text = "结束课堂",
                    onClick = { viewModel.endLiveClass() },
                    enabled = session != null && status != TranscriptStatus.ENDED,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "生成知识时间线",
                    onClick = { viewModel.analyzeLiveTranscript() },
                    enabled = canGenerate,
                    modifier = Modifier.weight(1f),
                )
            }
            if (status == TranscriptStatus.ENDED && !canGenerate) {
                Text(
                    "课堂已结束但没有任何片段，无法生成时间线。请先开始课堂并添加片段。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

private fun statusZh(status: TranscriptStatus): String = when (status) {
    TranscriptStatus.IDLE -> "未开始"
    TranscriptStatus.RUNNING -> "进行中"
    TranscriptStatus.PAUSED -> "已暂停"
    TranscriptStatus.ENDED -> "已结束"
}

private fun statusTone(status: TranscriptStatus): ChipTone = when (status) {
    TranscriptStatus.RUNNING -> ChipTone.SUCCESS
    TranscriptStatus.PAUSED -> ChipTone.WARNING
    TranscriptStatus.ENDED -> ChipTone.NEUTRAL
    TranscriptStatus.IDLE -> ChipTone.NEUTRAL
}

@Composable
private fun AsrHint(text: String) {
    Spacer(Modifier.height(Dimens.xs))
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

private fun asrStateZh(state: AsrState): String = when (state) {
    AsrState.IDLE -> "未开始"
    AsrState.PERMISSION_REQUIRED -> "待授权麦克风"
    AsrState.LISTENING -> "识别中"
    AsrState.PROCESSING -> "处理中"
    AsrState.PAUSED -> "已暂停"
    AsrState.ERROR -> "出错"
    AsrState.UNSUPPORTED -> "不支持"
}

private fun asrStateTone(state: AsrState): ChipTone = when (state) {
    AsrState.LISTENING -> ChipTone.SUCCESS
    AsrState.PROCESSING -> ChipTone.INFO
    AsrState.PAUSED -> ChipTone.WARNING
    AsrState.ERROR, AsrState.UNSUPPORTED, AsrState.PERMISSION_REQUIRED -> ChipTone.WARNING
    AsrState.IDLE -> ChipTone.NEUTRAL
}
