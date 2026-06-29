package com.classmate.app.ui.screens.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.classmate.app.asr.AndroidSpeechRecognizerClient
import com.classmate.app.asr.AsrEventListener
import com.classmate.app.asr.AsrState
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.ui.flow.FlowBreathingTimer
import com.classmate.app.ui.flow.FocusDurations
import com.classmate.app.ui.flow.FlowCompColors
import com.classmate.app.ui.flow.FlowCompPanel
import com.classmate.app.ui.flow.FlowCompSectionLabel
import com.classmate.app.ui.flow.FlowCompanionBackdrop
import com.classmate.app.ui.flow.FlowCompanionCopy
import com.classmate.app.ui.flow.FlowControlCluster
import com.classmate.app.ui.flow.FlowKnowledgeCacheCard
import com.classmate.app.ui.flow.FlowMiniPlayer
import com.classmate.app.ui.flow.FlowPillButton
import com.classmate.app.ui.flow.FlowScenePicker
import com.classmate.app.ui.flow.FlowSessionFooter
import com.classmate.app.ui.flow.FlowSessionTopBar
import com.classmate.app.ui.flow.FlowSoundSceneCard
import com.classmate.app.ui.flow.flowCompEnter
import com.classmate.app.ui.flow.flowCompSceneOf
import com.classmate.core.live.TranscriptStatus
import com.classmate.core.transcript.TranscriptClock
import com.classmate.core.transcript.zhLabelOrNull
import kotlinx.coroutines.delay

private const val FOCUS_TARGET_MIN = 75

/**
 * FlowCompanion — the immersive focus / ambient study companion (the ONLY Flow-themed surface;
 * reached via an explicit "心流学习 / 沉浸复习" entry, never as a global theme). Built to match
 * docs/design_refs/classmate_flow.html: a warm dark light-field backdrop, a breathing timer ring as the
 * hero, a control cluster, a sound-scene card + 2-col scene picker, a cached-knowledge surface and a
 * session footer. Local ambient loops are bundled as licensed raw resources; nothing is recorded,
 * uploaded, or generated at runtime. Session lifecycle, manual segment capture, the experimental
 * system ASR, and timeline generation are all preserved.
 */
@Composable
fun LiveCompanionScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val session = ui.liveTranscript
    val status = session?.status ?: TranscriptStatus.IDLE
    val running = status == TranscriptStatus.RUNNING

    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running) {
        while (running) { nowTick = System.currentTimeMillis(); delay(1000) }
    }
    // P1-2: background-music playback is owned by the ViewModel, so leaving this page does NOT stop it.
    // The page restores its visual scene from whatever is currently playing.
    val flowMusic = ui.flowMusic
    var selectedScene by remember { mutableStateOf(flowCompSceneOf(flowMusic.sceneId).id) }
    var scenePickerOpen by remember { mutableStateOf(false) }
    var jotOpen by remember { mutableStateOf(false) }
    var asrOpen by remember { mutableStateOf(false) }
    val scene = flowCompSceneOf(selectedScene)
    val accent = scene.accent
    val ambientPlaying = flowMusic.playing
    val ambientVolume = flowMusic.volume

    val context = LocalContext.current
    fun toggleAmbient() {
        if (ambientPlaying) viewModel.flowMusicPause() else viewModel.flowMusicPlay(scene.sound)
    }
    fun selectScene(id: String) {
        selectedScene = id
        // If music is already playing, switch the loop to the newly chosen scene; otherwise just preview-select.
        if (viewModel.ui.flowMusic.playing) viewModel.flowMusicPlay(flowCompSceneOf(id).sound)
    }

    // --- experimental system ASR wiring (no raw audio saved, no upload, no background recording) ---
    val asr = ui.asrSession
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
        if (granted && viewModel.asrBegin(engine.isAvailable(), true) == AsrState.LISTENING) engine.start(listener)
        else if (!granted) viewModel.asrBegin(engine.isAvailable(), permissionGranted = false)
    }
    val startAsr = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        when {
            !engine.isAvailable() -> viewModel.asrBegin(available = false, permissionGranted = granted)
            granted -> if (viewModel.asrBegin(true, true) == AsrState.LISTENING) { engine.start(listener); AsrState.LISTENING } else AsrState.IDLE
            else -> { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); AsrState.PERMISSION_REQUIRED }
        }
    }

    var focusTargetMin by remember { mutableStateOf(FocusDurations.DEFAULT_MIN) }
    var customMinText by remember { mutableStateOf("") }
    var customMinError by remember { mutableStateOf(false) }
    val elapsedMs = session?.elapsedMs(nowTick) ?: 0L
    val minutes = (elapsedMs / 60000L).toInt()
    val progress = (elapsedMs.toFloat() / (focusTargetMin * 60_000f)).coerceIn(0f, 1f)
    val cachedCount = ui.result?.knowledgePoints?.size ?: session?.segments?.size ?: 0
    val course = ui.liveTitleDraft.ifBlank { ui.session?.title?.ifBlank { "心流学习" } ?: "心流学习" }

    // Local DARK theme so embedded Material widgets (text fields) read correctly on the dark backdrop —
    // this is scoped to the companion only and never changes the global theme.
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = androidx.compose.ui.graphics.Color(0xFF2B1F0E),
            surface = androidx.compose.ui.graphics.Color(0xFF16161D),
            onSurface = FlowCompColors.textPrimary,
            background = androidx.compose.ui.graphics.Color(0xFF111016),
            onSurfaceVariant = FlowCompColors.textSecondary,
        ),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
    ) {
        FlowCompanionBackdrop(scene, Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                FlowSessionTopBar(
                    course = course,
                    status = if (running) "专注中 · ${FlowCompanionCopy.COMPANION_TAG}" else FlowCompanionCopy.COMPANION_TAG,
                    accent = accent,
                    onBack = { viewModel.goBack() },
                )
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 28.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (scenePickerOpen) {
                        Spacer(Modifier.height(4.dp))
                        FlowCompSectionLabel("声音场景")
                        Text("选择一个内置背景音，循环播放；不联网、不录音。", style = MaterialTheme.typography.bodySmall, color = FlowCompColors.textSecondary)
                        FlowScenePicker(selectedId = selectedScene, modifier = Modifier.flowCompEnter()) { selectScene(it) }
                        Text(FlowCompanionCopy.AUDIO_DISCLAIMER, style = MaterialTheme.typography.labelSmall, color = FlowCompColors.textMuted)
                        FlowMiniPlayer(sceneName = scene.name, soundName = scene.sound.displayName, playing = ambientPlaying, volume = ambientVolume, minutes = minutes, accent = accent)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FlowPillButton(
                                if (ambientPlaying) "暂停背景音" else "播放背景音",
                                filled = true,
                                accent = accent,
                                modifier = Modifier.weight(1f),
                                onClick = { toggleAmbient() },
                            )
                            FlowPillButton("停止", filled = false, accent = accent, modifier = Modifier.weight(1f), onClick = {
                                viewModel.flowMusicStop()
                            })
                        }
                        Text("背景音量", style = MaterialTheme.typography.labelSmall, color = FlowCompColors.textMuted)
                        Slider(value = ambientVolume, onValueChange = { viewModel.flowMusicSetVolume(it) })
                        // Full-width bottom main button — large tap target, not stranded in the corner.
                        FlowPillButton("完成设置", filled = true, accent = accent, modifier = Modifier.fillMaxWidth(), onClick = { scenePickerOpen = false })
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            FlowBreathingTimer(
                                modifier = Modifier.flowCompEnter(),
                                elapsedLabel = formatElapsed(elapsedMs),
                                sublabel = "本次专注 · 目标 $focusTargetMin 分钟",
                                progress = progress,
                                running = running,
                                accent = accent,
                            )
                            Spacer(Modifier.height(10.dp))
                            // Pick the focus length (P1-2): was a fixed 75-minute target users couldn't change.
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FocusDurations.presets.forEach { m ->
                                    FlowPillButton(
                                        FocusDurations.label(m),
                                        filled = m == focusTargetMin,
                                        accent = accent,
                                        onClick = { focusTargetMin = m; customMinText = "" },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Custom focus length (F0-9): any whole number of minutes in 1..180.
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = customMinText,
                                    onValueChange = { customMinText = it.filter(Char::isDigit).take(3); customMinError = false },
                                    label = { Text("自定义分钟") },
                                    singleLine = true,
                                    isError = customMinError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(150.dp),
                                )
                                FlowPillButton(
                                    "设为目标",
                                    filled = false,
                                    accent = accent,
                                    onClick = {
                                        val parsed = FocusDurations.parseCustom(customMinText)
                                        if (parsed == null) {
                                            customMinError = true
                                        } else {
                                            focusTargetMin = parsed
                                            customMinError = false
                                        }
                                    },
                                )
                            }
                            if (customMinError) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    FocusDurations.customHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF8A80),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        FlowControlCluster(
                            paused = !running,
                            accent = accent,
                            onJot = { jotOpen = !jotOpen },
                            onToggle = {
                                when {
                                    session == null -> viewModel.startLiveClass()
                                    status == TranscriptStatus.PAUSED -> viewModel.resumeLiveClass()
                                    status == TranscriptStatus.ENDED -> viewModel.startLiveClass()
                                    else -> viewModel.pauseLiveClass()
                                }
                            },
                            onShot = { viewModel.toast("添加截图：可在「资料」用图片 / 拍照学习输入生成端侧多模态草稿。") },
                        )

                        if (jotOpen) {
                            FlowCompPanel(modifier = Modifier.flowCompEnter()) {
                                Text("记录这一刻 · 手动添加片段", style = MaterialTheme.typography.titleSmall, color = FlowCompColors.textPrimary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = ui.liveTitleDraft,
                                    onValueChange = viewModel::updateLiveTitle,
                                    label = { Text("课程标题") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = ui.liveSegmentDraft,
                                    onValueChange = viewModel::updateLiveSegment,
                                    label = { Text("记一条灵感 / 标记重点（课后并入完整时间轴）") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                // Full-width button under the input — not stranded narrow/left (P1).
                                FlowPillButton(
                                    "追加片段",
                                    filled = true,
                                    accent = accent,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { viewModel.appendLiveSegment() },
                                )
                            }
                        }

                        FlowCompSectionLabel("声音场景")
                        FlowSoundSceneCard(
                            scene = scene,
                            playingLabel = if (ambientPlaying) "循环播放 · ${scene.sound.displayName} · 音量 ${(ambientVolume * 100).toInt()}%" else "已选择 · ${scene.sound.displayName} · 点击播放/更换",
                            modifier = Modifier.flowCompEnter(40),
                            onOpenPicker = { scenePickerOpen = true },
                        )

                        FlowCompSectionLabel("本次缓存")
                        FlowKnowledgeCacheCard(
                            count = cachedCount,
                            accent = accent,
                            modifier = Modifier.flowCompEnter(80),
                            onView = {
                                if (ui.result != null) viewModel.navigateTo(Screen.KNOWLEDGE)
                                else viewModel.toast("开始专注并记录片段后，可生成知识时间线再查看阶段总结。")
                            },
                        )

                        if (viewModel.canGenerateLiveTimeline()) {
                            FlowPillButton("生成知识时间线", filled = false, accent = accent, onClick = { viewModel.analyzeLiveTranscript() })
                        }

                        // Experimental real-time transcription — preserved, folded so it never dominates.
                        FlowCompSectionLabel("实时转写（实验）")
                        FlowCompPanel(modifier = Modifier.flowCompEnter(120)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("系统语音识别 · ${asrStateZh(asr.state)}", style = MaterialTheme.typography.bodyMedium, color = FlowCompColors.textPrimary, modifier = Modifier.weight(1f))
                                Text("已确认 ${asr.confirmedCount}", style = MaterialTheme.typography.labelSmall, color = FlowCompColors.textMuted)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "效果依设备而定；不保存原始音频、不后台录音、不做声纹识别。${FlowCompanionCopy.TRANSCRIPT_NOTE}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FlowCompColors.textMuted,
                            )
                            if (asr.partialText.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("识别中：${asr.partialText}", style = MaterialTheme.typography.bodySmall, color = accent)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (asr.isActive) {
                                    FlowPillButton("暂停", filled = false, accent = accent, modifier = Modifier.weight(1f), onClick = { engine.stop(); viewModel.pauseAsr() })
                                    FlowPillButton("停止", filled = false, accent = accent, modifier = Modifier.weight(1f), onClick = { engine.stop(); viewModel.stopAsr() })
                                } else if (asr.state == AsrState.PAUSED) {
                                    FlowPillButton("继续", filled = true, accent = accent, modifier = Modifier.weight(1f), onClick = { viewModel.resumeAsr(); engine.start(listener) })
                                    FlowPillButton("停止", filled = false, accent = accent, modifier = Modifier.weight(1f), onClick = { engine.stop(); viewModel.stopAsr() })
                                } else {
                                    FlowPillButton("开始实时转写", filled = true, accent = accent, modifier = Modifier.weight(1f), onClick = { startAsr() })
                                }
                            }
                            when (asr.state) {
                                AsrState.UNSUPPORTED -> AsrHint("不支持系统语音识别，请使用手动记录或导入字幕。")
                                AsrState.PERMISSION_REQUIRED -> AsrHint("未授权麦克风，仍可手动记录或导入转写稿。")
                                AsrState.ERROR -> AsrHint(asr.errorMessage ?: "系统语音识别出错，请改用手动记录。")
                                else -> {}
                            }
                            if (asr.segments.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                asr.segments.takeLast(3).forEach {
                                    val time = it.startMs?.let { ms -> TranscriptClock.format(ms) + " · " } ?: ""
                                    val speaker = it.speaker.zhLabelOrNull()?.let { sp -> "[$sp] " } ?: ""
                                    Text("$time$speaker${it.text}", style = MaterialTheme.typography.bodySmall, color = FlowCompColors.textSecondary)
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        FlowSessionFooter(
                            minutes = minutes,
                            accent = accent,
                            onEnd = { viewModel.endLiveClass() },
                            onBack = { viewModel.goBack() },
                        )
                        if (status == TranscriptStatus.ENDED && !viewModel.canGenerateLiveTimeline()) {
                            Text(
                                "本次专注还没有任何片段，无法生成时间线。开始专注并记录要点后再试。",
                                style = MaterialTheme.typography.bodySmall,
                                color = FlowCompColors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
private fun AsrHint(text: String) {
    Spacer(Modifier.height(6.dp))
    Text(text, style = MaterialTheme.typography.bodySmall, color = FlowCompColors.textSecondary)
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
