package com.classmate.core.official

/**
 * L3-readiness layer over [VivoOfficialProviderRegistry]. The provider registry says WHAT each official
 * capability is; this says how close each is to the real, device-tested main-learning-loop standard, and
 * what the honest user-facing entry / evidence / failure-action is in both languages.
 *
 * Honesty contract (enforced by CapabilityReadinessRegistryTest):
 *  - Only the genuinely device-verified main chain may be [L3Readiness.TRUE_DEVICE_READY].
 *  - Anything needing real creds / model files / a real device that has NOT been device-verified is
 *    [L3Readiness.TRUE_DEVICE_PENDING] or below — never dressed up as USED.
 *  - Experimental capabilities ([experimentalDefaultOff] = true) must stay default-off.
 *  - Every capability has a fallback route and a user action; none can dead-end at an empty placeholder
 *    unless input is empty / safety-blocked / local rule also failed.
 */
enum class L3Readiness {
    /** Real output flows into the learning loop on the main chain (cloud→edge→local) — the gold standard. */
    USED_IN_LEARNING_LOOP,
    /** Works today only through the local/manual fallback; official live path is not enabled. */
    FALLBACK_ONLY,
    /** Contract/seam is present and tested, but the end-to-end path is not wired into the product yet. */
    SEAM_READY,
    /** Needs credentials / config before it can run at all. */
    CONFIG_REQUIRED,
    /** Experimental enhancement, default-off; produces a prompt/storyboard/draft, never a faked artifact. */
    EXPERIMENTAL,
    /** Verified working on a real vivo device. */
    TRUE_DEVICE_READY,
    /** Aligned to the main-chain standard in code, but still needs a real-device / cloud-device spot check. */
    TRUE_DEVICE_PENDING,
}

/** The routing options a capability may take, mirroring the main chain. */
enum class CapabilityRoute { BLUE_LM_CLOUD, EDGE_3B, LOCAL_RULE, CONFIG_REQUIRED, SEAM_READY, EXPERIMENTAL, MANUAL }

data class CapabilityReadiness(
    val id: String, // must match a VivoOfficialProviderRegistry id
    val readiness: L3Readiness,
    val routes: List<CapabilityRoute>,
    val entryPointZh: String,
    val entryPointEn: String,
    val evidencePolicyZh: String,
    val evidencePolicyEn: String,
    val failureActionZh: String,
    val failureActionEn: String,
    val needsDeviceTest: Boolean,
    val experimentalDefaultOff: Boolean = false,
) {
    fun readinessLabelZh(): String = when (readiness) {
        L3Readiness.USED_IN_LEARNING_LOOP -> "已用于学习闭环"
        L3Readiness.FALLBACK_ONLY -> "仅本地/兜底可用"
        L3Readiness.SEAM_READY -> "接缝就绪（未接入产品主链）"
        L3Readiness.CONFIG_REQUIRED -> "需配置后可用"
        L3Readiness.EXPERIMENTAL -> "实验性（默认关闭）"
        L3Readiness.TRUE_DEVICE_READY -> "真机已验证"
        L3Readiness.TRUE_DEVICE_PENDING -> "对齐主链标准，待真机抽测"
    }

    fun readinessLabelEn(): String = when (readiness) {
        L3Readiness.USED_IN_LEARNING_LOOP -> "Used in learning loop"
        L3Readiness.FALLBACK_ONLY -> "Local/fallback only"
        L3Readiness.SEAM_READY -> "Seam ready (not in product main chain)"
        L3Readiness.CONFIG_REQUIRED -> "Config required"
        L3Readiness.EXPERIMENTAL -> "Experimental (default off)"
        L3Readiness.TRUE_DEVICE_READY -> "Verified on device"
        L3Readiness.TRUE_DEVICE_PENDING -> "Aligned, pending device spot-check"
    }

    /** The official-registry capability this readiness row describes. */
    fun capability(): OfficialCapability? = VivoOfficialProviderRegistry.byId(id)
}

object CapabilityReadinessRegistry {

    /**
     * The L3-readiness golden standard every capability is checked against (also rendered into
     * docs/current/official_18_capability_l3_readiness.md). Kept content-stable for the doc-sync test.
     */
    val goldenStandard: List<String> = listOf(
        "用户入口明确",
        "输入内容可追溯，原始内容完整保留",
        "EvidenceAsset / 来源记录完整",
        "能力路由明确：BLUE_LM_CLOUD → EDGE_3B → LOCAL_RULE / CONFIG_REQUIRED / SEAM_READY / EXPERIMENTAL",
        "云端路径有 timeout / retry / 降级",
        "端侧失败有权限、路径、重检说明",
        "云端与端侧都失败时优先自动进入 LOCAL_RULE 本地基础整理",
        "安全占位仅用于：输入为空 / 安全审核拒绝 / 本地规则也失败 / 示例课",
        "不允许无限 loading",
        "长分析有已用时、思考强度、慢响应提示、防息屏、可操作出口",
        "UI 显示实际来源与下一步",
        "能进入至少一个学习产物（知识点/微测/错题/复习/诊断/StudyPack/证据）",
        "失败仍保留证据或提供手动入口",
        "有测试覆盖",
        "有真机抽测路径",
        "文案诚实，不夸大 provider 状态",
        "中英文模式下用户产品文案跟随语言切换",
    )

    val all: List<CapabilityReadiness> = listOf(
        CapabilityReadiness(
            id = "LARGE_MODEL",
            readiness = L3Readiness.TRUE_DEVICE_READY,
            routes = listOf(CapabilityRoute.BLUE_LM_CLOUD, CapabilityRoute.EDGE_3B, CapabilityRoute.LOCAL_RULE),
            entryPointZh = "导入页「生成知识时间线」", entryPointEn = "Import → Generate knowledge timeline",
            evidencePolicyZh = "课堂原文完整保留为分段 Evidence", evidencePolicyEn = "Full source kept as segmented evidence",
            failureActionZh = "自动重试 → 端侧 → 本地基础整理 → 手动整理", failureActionEn = "Retry → edge → local rule → manual",
            needsDeviceTest = true,
        ),
        CapabilityReadiness(
            id = "FUNCTION_CALLING",
            readiness = L3Readiness.SEAM_READY,
            routes = listOf(CapabilityRoute.SEAM_READY, CapabilityRoute.MANUAL),
            entryPointZh = "内部工具编排（受控）", entryPointEn = "Internal tool orchestration (gated)",
            evidencePolicyZh = "工具计划记录，不改证据", evidencePolicyEn = "Tool plan recorded; evidence unchanged",
            failureActionZh = "回退到用户确认按钮", failureActionEn = "Fall back to user confirmation",
            needsDeviceTest = false,
        ),
        CapabilityReadiness(
            id = "IMAGE_GENERATION",
            readiness = L3Readiness.EXPERIMENTAL,
            routes = listOf(CapabilityRoute.EXPERIMENTAL, CapabilityRoute.CONFIG_REQUIRED),
            entryPointZh = "实验开关（默认关闭）", entryPointEn = "Experimental toggle (off by default)",
            evidencePolicyZh = "仅生成学习图解 prompt，不产生真实图", evidencePolicyEn = "Study-diagram prompt only; no real image",
            failureActionZh = "失败不影响主学习闭环", failureActionEn = "Failure never blocks the learning loop",
            needsDeviceTest = false, experimentalDefaultOff = true,
        ),
        CapabilityReadiness(
            id = "VIDEO_GENERATION",
            readiness = L3Readiness.EXPERIMENTAL,
            routes = listOf(CapabilityRoute.EXPERIMENTAL, CapabilityRoute.CONFIG_REQUIRED),
            entryPointZh = "实验开关（默认关闭）", entryPointEn = "Experimental toggle (off by default)",
            evidencePolicyZh = "仅生成复习短视频 storyboard，不产生真实视频", evidencePolicyEn = "Review storyboard only; no real video",
            failureActionZh = "失败不影响主学习闭环", failureActionEn = "Failure never blocks the learning loop",
            needsDeviceTest = false, experimentalDefaultOff = true,
        ),
        CapabilityReadiness(
            id = "OCR",
            readiness = L3Readiness.USED_IN_LEARNING_LOOP,
            routes = listOf(CapabilityRoute.BLUE_LM_CLOUD, CapabilityRoute.EDGE_3B, CapabilityRoute.MANUAL),
            entryPointZh = "导入页「图片/拍照」", entryPointEn = "Import → image/photo",
            evidencePolicyZh = "OCR 文本保存为 OCR Evidence，可编辑", evidencePolicyEn = "OCR text saved as editable OCR evidence",
            failureActionZh = "失败可手动编辑文本；低置信提示确认", failureActionEn = "Manual edit on failure; confirm low-confidence",
            needsDeviceTest = true,
        ),
        CapabilityReadiness(
            id = "TRANSLATION",
            readiness = L3Readiness.FALLBACK_ONLY,
            routes = listOf(CapabilityRoute.BLUE_LM_CLOUD, CapabilityRoute.LOCAL_RULE, CapabilityRoute.MANUAL),
            entryPointZh = "证据/知识点的翻译注记", entryPointEn = "Translation notes on evidence/KP",
            evidencePolicyZh = "双语注记派生，不改原始证据", evidencePolicyEn = "Bilingual note derived; original evidence kept",
            failureActionZh = "provider 不可用时保留原文学习", failureActionEn = "Keep original-language learning if unavailable",
            needsDeviceTest = false,
        ),
        CapabilityReadiness(
            id = "EMBEDDING",
            readiness = L3Readiness.FALLBACK_ONLY,
            routes = listOf(CapabilityRoute.BLUE_LM_CLOUD, CapabilityRoute.LOCAL_RULE),
            entryPointZh = "相似知识点/检索增强（后台）", entryPointEn = "Similar-KP / retrieval (background)",
            evidencePolicyZh = "本地词法索引，证据不变", evidencePolicyEn = "Local lexical index; evidence unchanged",
            failureActionZh = "官方未启用时本地检索继续可用", failureActionEn = "Local retrieval continues if official is off",
            needsDeviceTest = false,
        ),
        CapabilityReadiness(
            id = "TEXT_SIMILARITY",
            readiness = L3Readiness.FALLBACK_ONLY,
            routes = listOf(CapabilityRoute.BLUE_LM_CLOUD, CapabilityRoute.LOCAL_RULE),
            entryPointZh = "Ask/练习的证据排序（后台）", entryPointEn = "Ask/practice evidence ranking (background)",
            evidencePolicyZh = "本地证据排序，证据不变", evidencePolicyEn = "Local evidence ordering; evidence unchanged",
            failureActionZh = "官方 rerank 未启用时本地排序", failureActionEn = "Local ranking if official rerank is off",
            needsDeviceTest = false,
        ),
        CapabilityReadiness(
            id = "QUERY_REWRITE",
            readiness = L3Readiness.FALLBACK_ONLY,
            routes = listOf(CapabilityRoute.BLUE_LM_CLOUD, CapabilityRoute.LOCAL_RULE),
            entryPointZh = "Evidence-grounded Ask（后台）", entryPointEn = "Evidence-grounded Ask (background)",
            evidencePolicyZh = "仅改写检索词，证据不变", evidencePolicyEn = "Rewrites query only; evidence unchanged",
            failureActionZh = "官方未启用时用原始问题", failureActionEn = "Use original query if official is off",
            needsDeviceTest = false,
        ),
        CapabilityReadiness(
            id = "SHORT_ASR",
            readiness = L3Readiness.SEAM_READY,
            routes = listOf(CapabilityRoute.SEAM_READY, CapabilityRoute.MANUAL),
            entryPointZh = "课堂伴学（接缝）", entryPointEn = "Live Companion (seam)",
            evidencePolicyZh = "转写确认后才入库", evidencePolicyEn = "Transcript enters only after confirmation",
            failureActionZh = "不可用时手动粘贴转写", failureActionEn = "Paste transcript manually if unavailable",
            needsDeviceTest = true,
        ),
        CapabilityReadiness(
            id = "LONG_DICTATION",
            readiness = L3Readiness.SEAM_READY,
            routes = listOf(CapabilityRoute.SEAM_READY, CapabilityRoute.MANUAL),
            entryPointZh = "音频导入（候选）", entryPointEn = "Audio import (candidate)",
            evidencePolicyZh = "转写确认后才入库", evidencePolicyEn = "Transcript enters only after confirmation",
            failureActionZh = "不可用时手动粘贴转写", failureActionEn = "Paste transcript manually if unavailable",
            needsDeviceTest = true,
        ),
        CapabilityReadiness(
            id = "ASR_LONG",
            readiness = L3Readiness.TRUE_DEVICE_PENDING,
            routes = listOf(CapabilityRoute.SEAM_READY, CapabilityRoute.MANUAL),
            entryPointZh = "音频导入「长语音转写」", entryPointEn = "Audio import → long transcription",
            evidencePolicyZh = "生成可编辑 TranscriptDraft + AUDIO Evidence", evidencePolicyEn = "Editable TranscriptDraft + AUDIO evidence",
            failureActionZh = "上传/轮询未验证；手动粘贴转写兜底", failureActionEn = "Upload/poll unvalidated; manual paste fallback",
            needsDeviceTest = true,
        ),
        CapabilityReadiness(
            id = "DIALECT_ASR",
            readiness = L3Readiness.SEAM_READY,
            routes = listOf(CapabilityRoute.SEAM_READY, CapabilityRoute.MANUAL),
            entryPointZh = "音频强度/方言设置（接缝）", entryPointEn = "Audio dialect setting (seam)",
            evidencePolicyZh = "进入 capability plan；转写确认后入库", evidencePolicyEn = "Enters capability plan; confirm before storing",
            failureActionZh = "不可用时手动粘贴转写，不阻断文本闭环", failureActionEn = "Manual paste fallback; never blocks text loop",
            needsDeviceTest = true,
        ),
        CapabilityReadiness(
            id = "SIMULTANEOUS_INTERPRETATION",
            readiness = L3Readiness.EXPERIMENTAL,
            routes = listOf(CapabilityRoute.EXPERIMENTAL, CapabilityRoute.SEAM_READY),
            entryPointZh = "实验开关（默认关闭）", entryPointEn = "Experimental toggle (off by default)",
            evidencePolicyZh = "仅双语草稿/接缝，不伪装实时同传", evidencePolicyEn = "Bilingual draft/seam only; not real-time",
            failureActionZh = "失败不影响知识点/题目/复习", failureActionEn = "Failure never blocks KP/quiz/review",
            needsDeviceTest = false, experimentalDefaultOff = true,
        ),
        CapabilityReadiness(
            id = "TTS",
            readiness = L3Readiness.FALLBACK_ONLY,
            routes = listOf(CapabilityRoute.BLUE_LM_CLOUD, CapabilityRoute.LOCAL_RULE, CapabilityRoute.MANUAL),
            entryPointZh = "课程精华音频 / 朗读脚本", entryPointEn = "Course-essence audio / read-aloud script",
            evidencePolicyZh = "脚本可进入 Study Pack；不伪装 audioRef", evidencePolicyEn = "Script can enter Study Pack; no faked audioRef",
            failureActionZh = "官方 TTS 不可用时生成朗读脚本", failureActionEn = "Produce a read-aloud script if TTS is off",
            needsDeviceTest = false,
        ),
        CapabilityReadiness(
            id = "ON_DEVICE_3B",
            readiness = L3Readiness.TRUE_DEVICE_PENDING,
            routes = listOf(CapabilityRoute.EDGE_3B, CapabilityRoute.LOCAL_RULE, CapabilityRoute.MANUAL),
            entryPointZh = "云端不可用时自动接管 / 设置·能力中心", entryPointEn = "Auto edge fallback / Settings · capability center",
            evidencePolicyZh = "端侧结构化结果过同一校验器才入库", evidencePolicyEn = "Edge result must pass the same validators",
            failureActionZh = "缺权限/模型时 LOCAL_RULE 继续；给授权/路径/重检", failureActionEn = "Local rule continues; show grant/path/recheck",
            needsDeviceTest = true,
        ),
        CapabilityReadiness(
            id = "ON_DEVICE_TEXT_SAFETY",
            readiness = L3Readiness.SEAM_READY,
            routes = listOf(CapabilityRoute.EDGE_3B, CapabilityRoute.LOCAL_RULE),
            entryPointZh = "导出/分享前的文本检查（接缝）", entryPointEn = "Pre-export/share text check (seam)",
            evidencePolicyZh = "护栏阻断危险内容，不误伤正常学习文本", evidencePolicyEn = "Guard blocks risky content; spares normal text",
            failureActionZh = "不可用不阻断核心学习；温和提示", failureActionEn = "Unavailable never blocks learning; gentle warning",
            needsDeviceTest = true,
        ),
        CapabilityReadiness(
            id = "ON_DEVICE_FILES",
            readiness = L3Readiness.CONFIG_REQUIRED,
            routes = listOf(CapabilityRoute.CONFIG_REQUIRED, CapabilityRoute.LOCAL_RULE),
            entryPointZh = "设置·能力中心 模型目录诊断", entryPointEn = "Settings · capability center model-dir diagnostics",
            evidencePolicyZh = "只读固定路径文件名，不读文件内容", evidencePolicyEn = "Reads fixed-path file names only; no file content",
            failureActionZh = "缺失时给授权/路径/重检；端侧多模态保持待验证", failureActionEn = "Grant/path/recheck; multimodal stays pending",
            needsDeviceTest = true,
        ),
    )

    fun byId(id: String): CapabilityReadiness? = all.firstOrNull { it.id.equals(id, ignoreCase = true) }

    /** Capabilities whose final terminal must come from real device verification before claiming "used". */
    val deviceTestPending: List<CapabilityReadiness> = all.filter { it.needsDeviceTest }
    val experimental: List<CapabilityReadiness> = all.filter { it.experimentalDefaultOff }
}
