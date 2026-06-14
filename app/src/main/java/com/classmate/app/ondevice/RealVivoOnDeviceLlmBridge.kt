package com.classmate.app.ondevice

import com.classmate.core.ondevice.OnDeviceGenerationResult
import com.classmate.core.ondevice.OnDeviceLlmConfig
import com.classmate.core.ondevice.OnDeviceLlmDiagnostic
import com.classmate.core.ondevice.OnDeviceLlmProvider
import com.classmate.core.ondevice.OnDeviceLlmStatus
import com.classmate.core.ondevice.OnDeviceLlmTaskProfile
import com.classmate.core.ondevice.OnDeviceMultimodalDiagnostic
import com.classmate.core.ondevice.OnDeviceMultimodalProbe
import com.classmate.core.ondevice.OnDevicePromptTemplate
import com.classmate.core.ondevice.OnDeviceProbeState
import com.classmate.core.ondevice.RgbImage
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The REAL on-device bridge over the vivo BlueLM SDK, driven entirely by reflection (see
 * [VivoSdkReflection]) so the module compiles without the AAR. It implements both pure-text
 * generation ([OnDeviceLlmProvider]) and the experimental multimodal VIT diagnostic
 * ([OnDeviceMultimodalProbe]).
 *
 * Safety:
 *  - Blocking SDK calls (init / generate / callVit) MUST run off the UI thread — the
 *    [OnDeviceLlmController] confines them to Dispatchers.IO.
 *  - It NEVER throws for expected failures and NEVER logs the prompt or full output. Only a bounded
 *    (≤80 char) preview and safe enum/codes are surfaced.
 *  - A signature mismatch (e.g. an old stats-carrying onComplete) yields SDK_SIGNATURE_MISMATCH, not
 *    a crash.
 */
class RealVivoOnDeviceLlmBridge(
    initialConfig: OnDeviceLlmConfig = OnDeviceLlmConfig(),
    loadResult: VivoSdkReflection.LoadResult = VivoSdkReflection.load(),
    private val generateTimeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) : OnDeviceLlmProvider, OnDeviceMultimodalProbe {

    private val lock = Any()
    private val reflection: VivoSdkReflection? = (loadResult as? VivoSdkReflection.LoadResult.Ok)?.reflection
    private val signatureMismatch: Boolean = loadResult is VivoSdkReflection.LoadResult.SignatureMismatch

    @Volatile private var config: OnDeviceLlmConfig = initialConfig
    @Volatile private var textManager: Any? = null
    @Volatile private var currentStatus: OnDeviceLlmStatus = when {
        reflection != null -> OnDeviceLlmStatus.SDK_PRESENT
        signatureMismatch -> OnDeviceLlmStatus.SDK_SIGNATURE_MISMATCH
        else -> OnDeviceLlmStatus.SDK_MISSING
    }
    @Volatile private var lastErrorCode: String? = null

    override fun status(): OnDeviceLlmStatus = currentStatus

    override fun diagnostic(): OnDeviceLlmDiagnostic = OnDeviceLlmDiagnostic(
        status = currentStatus,
        sdkPresent = reflection != null,
        modelDir = config.modelPath,
        modelDirChecked = false,
        modelDirPresent = null,
        initAttempted = textManager != null || currentStatus == OnDeviceLlmStatus.INIT_FAILED,
        initSucceeded = textManager != null,
        signatureOk = if (signatureMismatch) false else reflection != null,
        initState = initStateFor(currentStatus),
        generateState = OnDeviceProbeState.GENERATE_NOT_TESTED,
        errorCode = lastErrorCode,
        fallbackAvailable = true,
    )

    /** Real self-test for Settings: init (if needed) + ONE fixed-question generation, with preview. */
    override fun runTextProbe(question: String): OnDeviceLlmDiagnostic {
        val r = reflection ?: return diagnostic()
        val start = clock()
        val manager = ensureTextManager(r)
        if (manager == null) {
            return diagnostic().copy(
                initState = OnDeviceProbeState.INIT_FAILED,
                generateState = OnDeviceProbeState.GENERATE_NOT_TESTED,
                latencyMs = clock() - start,
            )
        }
        val prompt = OnDevicePromptTemplate.format(question)
        val outcome = aggregateGenerate(r, manager, prompt)
        return when (outcome) {
            is GenOutcome.Ok -> {
                currentStatus = OnDeviceLlmStatus.AVAILABLE
                lastErrorCode = null
                diagnostic().copy(
                    status = OnDeviceLlmStatus.AVAILABLE,
                    initState = OnDeviceProbeState.INIT_SUCCESS,
                    generateState = OnDeviceProbeState.GENERATE_SUCCESS,
                    outputPreview = OnDeviceLlmDiagnostic.safePreview(outcome.text),
                    latencyMs = clock() - start,
                )
            }
            else -> {
                lastErrorCode = outcome.safeCode()
                diagnostic().copy(
                    initState = OnDeviceProbeState.INIT_SUCCESS,
                    generateState = OnDeviceProbeState.GENERATE_FAILED,
                    errorCode = outcome.safeCode(),
                    latencyMs = clock() - start,
                )
            }
        }
    }

    override fun generate(profile: OnDeviceLlmTaskProfile, prompt: String): OnDeviceGenerationResult {
        val r = reflection ?: return OnDeviceGenerationResult.Unavailable(currentStatus)
        val manager = ensureTextManager(r) ?: return OnDeviceGenerationResult.Unavailable(currentStatus)
        val start = clock()
        return when (val outcome = aggregateGenerate(r, manager, prompt)) {
            is GenOutcome.Ok -> {
                currentStatus = OnDeviceLlmStatus.AVAILABLE
                // Full text goes to the parser/validator/redaction chain (never logged here).
                OnDeviceGenerationResult.Success(outcome.text, outcome.tokenCount, clock() - start)
            }
            else -> OnDeviceGenerationResult.Error(outcome.safeCode(), "端侧生成失败")
        }
    }

    override fun supportsMultimodal(): Boolean = reflection?.supportsMultimodal() == true

    override fun probeMultimodal(image: RgbImage, question: String): OnDeviceMultimodalDiagnostic {
        val r = reflection
        val supportsField = r?.fields?.multimodal != null
        val hasCallVit = r?.callVitMethod != null
        val base = { state: OnDeviceProbeState ->
            OnDeviceMultimodalDiagnostic(
                state = state,
                sdkSupportsMultimodalField = supportsField,
                callVitMethodPresent = hasCallVit,
                modelDir = config.modelPath,
                testImageWidth = image.width,
                testImageHeight = image.height,
                rgbByteLength = image.bytes.size,
            )
        }
        if (r == null || !supportsField || !hasCallVit) {
            return base(OnDeviceProbeState.MULTIMODAL_UNAVAILABLE)
        }

        // Fresh, one-shot multimodal manager so we never clobber the cached text manager.
        val init = reflectInit(r, config.copy(multimodal = true))
        if (init !is InitOutcome.Ok) {
            return base(OnDeviceProbeState.MULTIMODAL_SUPPORTED).copy(errorCode = init.safeCode())
        }
        val manager = init.manager
        try {
            val ret = try {
                r.callVitMethod!!.invoke(manager, image.bytes, image.width, image.height) as Int
            } catch (e: Throwable) {
                return base(OnDeviceProbeState.CALL_VIT_FAILED).copy(errorCode = safeClass(e))
            }
            if (ret != 0) {
                // callVit failed -> STOP before multimodal generate (vendor contract).
                return base(OnDeviceProbeState.CALL_VIT_FAILED).copy(callVitReturnCode = ret)
            }
            val outcome = aggregateGenerate(r, manager, OnDevicePromptTemplate.formatMultimodal(question))
            return when (outcome) {
                is GenOutcome.Ok -> base(OnDeviceProbeState.CALL_VIT_SUCCESS).copy(
                    callVitReturnCode = 0,
                    generateState = OnDeviceProbeState.GENERATE_SUCCESS,
                    outputPreview = OnDeviceLlmDiagnostic.safePreview(outcome.text),
                )
                else -> base(OnDeviceProbeState.CALL_VIT_SUCCESS).copy(
                    callVitReturnCode = 0,
                    generateState = OnDeviceProbeState.GENERATE_FAILED,
                    errorCode = outcome.safeCode(),
                )
            }
        } finally {
            releaseQuietly(r, manager)
        }
    }

    override fun describeImage(image: RgbImage, question: String): OnDeviceGenerationResult {
        val r = reflection ?: return OnDeviceGenerationResult.Unavailable(currentStatus)
        if (r.fields.multimodal == null || r.callVitMethod == null) {
            return OnDeviceGenerationResult.Unavailable(OnDeviceLlmStatus.DEVICE_UNSUPPORTED)
        }
        // Fresh, one-shot multimodal manager; init MUST succeed before callVit (callVit crash guard).
        val init = reflectInit(r, config.copy(multimodal = true))
        if (init !is InitOutcome.Ok) return OnDeviceGenerationResult.Error(init.safeCode(), "端侧多模态初始化失败")
        val manager = init.manager
        return try {
            val ret = try {
                r.callVitMethod!!.invoke(manager, image.bytes, image.width, image.height) as Int
            } catch (e: Throwable) {
                return OnDeviceGenerationResult.Error(safeClass(e), "端侧 VIT 编码异常")
            }
            if (ret != 0) return OnDeviceGenerationResult.Error("CALLVIT_$ret", "端侧 VIT 编码失败")
            when (val outcome = aggregateGenerate(r, manager, OnDevicePromptTemplate.formatMultimodal(question))) {
                is GenOutcome.Ok -> OnDeviceGenerationResult.Success(outcome.text, outcome.tokenCount, 0)
                else -> OnDeviceGenerationResult.Error(outcome.safeCode(), "端侧多模态生成失败")
            }
        } finally {
            releaseQuietly(r, manager)
        }
    }

    override fun interrupt() {
        val r = reflection ?: return
        val mgr = textManager ?: return
        runCatching { r.interruptMethod?.invoke(mgr) }
    }

    override fun release() {
        synchronized(lock) {
            val r = reflection
            val mgr = textManager
            if (r != null && mgr != null) releaseQuietly(r, mgr)
            textManager = null
            if (currentStatus == OnDeviceLlmStatus.AVAILABLE) currentStatus = OnDeviceLlmStatus.RELEASED
        }
    }

    /** Change the model path (P5). Releases any cached manager so the next probe re-inits. */
    override fun updateModelPath(path: String) {
        synchronized(lock) {
            val trimmed = path.trim().ifBlank { OnDeviceLlmConfig.DEFAULT_MODEL_DIR }
            if (trimmed == config.modelPath) return
            reflection?.let { r -> textManager?.let { releaseQuietly(r, it) } }
            textManager = null
            config = config.copy(modelPath = trimmed)
            if (reflection != null) currentStatus = OnDeviceLlmStatus.SDK_PRESENT
            lastErrorCode = null
        }
    }

    // --- internals ---

    private fun ensureTextManager(r: VivoSdkReflection): Any? = synchronized(lock) {
        textManager?.let { return it }
        when (val init = reflectInit(r, config.copy(multimodal = false))) {
            is InitOutcome.Ok -> {
                textManager = init.manager
                currentStatus = OnDeviceLlmStatus.AVAILABLE
                lastErrorCode = null
                init.manager
            }
            is InitOutcome.Failed -> {
                currentStatus = OnDeviceLlmStatus.INIT_FAILED
                lastErrorCode = "INIT_${init.code}"
                null
            }
            is InitOutcome.Threw -> {
                currentStatus = OnDeviceLlmStatus.INIT_FAILED
                lastErrorCode = init.code
                null
            }
        }
    }

    private sealed interface InitOutcome {
        data class Ok(val manager: Any) : InitOutcome
        data class Failed(val code: Int) : InitOutcome
        data class Threw(val code: String) : InitOutcome
        fun safeCode(): String = when (this) {
            is Ok -> "OK"
            is Failed -> "INIT_$code"
            is Threw -> code
        }
    }

    private fun reflectInit(r: VivoSdkReflection, cfg: OnDeviceLlmConfig): InitOutcome = try {
        val configObj = buildConfig(r, cfg)
        val manager = r.managerClass.getDeclaredConstructor().newInstance()
        val ret = r.initMethod.invoke(manager, configObj) as Int
        if (ret == 0) InitOutcome.Ok(manager) else InitOutcome.Failed(ret)
    } catch (e: Throwable) {
        InitOutcome.Threw(safeClass(e))
    }

    private fun buildConfig(r: VivoSdkReflection, cfg: OnDeviceLlmConfig): Any {
        val obj = r.configClass.getDeclaredConstructor().newInstance()
        val f = r.fields
        f.modelPath.set(obj, cfg.modelPath)
        f.nPredict?.setInt(obj, cfg.nPredict)
        f.nCtx?.setInt(obj, cfg.nCtx)
        f.nThreads?.setInt(obj, cfg.nThreads)
        f.topK?.setInt(obj, cfg.topK)
        f.topP?.setFloat(obj, cfg.topP.toFloat())
        f.temperature?.setFloat(obj, cfg.temperature.toFloat())
        f.npuPower?.setInt(obj, cfg.npuPower)
        f.multimodal?.setBoolean(obj, cfg.multimodal)
        return obj
    }

    private sealed interface GenOutcome {
        data class Ok(val text: String, val tokenCount: Int) : GenOutcome
        data class Err(val code: Int) : GenOutcome
        data object Timeout : GenOutcome
        data class Threw(val code: String) : GenOutcome
        fun safeCode(): String = when (this) {
            is Ok -> "OK"
            is Err -> "ONDEVICE_$code"
            Timeout -> "TIMEOUT"
            is Threw -> code
        }
    }

    private fun aggregateGenerate(r: VivoSdkReflection, manager: Any, prompt: String): GenOutcome {
        val tokens = StringBuilder()
        val tokenCount = AtomicInteger(0)
        val errored = AtomicBoolean(false)
        val errorCode = AtomicInteger(-1)
        val latch = CountDownLatch(1)

        val handler = InvocationHandler { proxy, method, args ->
            when (method.name) {
                "onToken" -> {
                    (args?.getOrNull(0) as? String)?.let { tokens.append(it); tokenCount.incrementAndGet() }
                    null
                }
                "onComplete" -> { latch.countDown(); null } // vendor: NO-ARG onComplete()
                "onError" -> {
                    errored.set(true)
                    errorCode.set((args?.getOrNull(0) as? Int) ?: -1)
                    latch.countDown()
                    null
                }
                "toString" -> "VivoTokenCallbackProxy"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                else -> null
            }
        }
        val callback = Proxy.newProxyInstance(
            r.tokenCallbackClass.classLoader,
            arrayOf(r.tokenCallbackClass),
            handler,
        )

        try {
            r.generateMethod.invoke(manager, prompt, callback)
        } catch (e: Throwable) {
            return GenOutcome.Threw(safeClass(e))
        }
        val done = latch.await(generateTimeoutMs, TimeUnit.MILLISECONDS)
        if (!done) {
            runCatching { r.interruptMethod?.invoke(manager) }
            return GenOutcome.Timeout
        }
        if (errored.get()) return GenOutcome.Err(errorCode.get())
        return GenOutcome.Ok(tokens.toString(), tokenCount.get())
    }

    private fun releaseQuietly(r: VivoSdkReflection, manager: Any) {
        runCatching { r.releaseMethod?.invoke(manager) }
    }

    private fun initStateFor(status: OnDeviceLlmStatus): OnDeviceProbeState = when (status) {
        OnDeviceLlmStatus.AVAILABLE -> OnDeviceProbeState.INIT_SUCCESS
        OnDeviceLlmStatus.INIT_FAILED -> OnDeviceProbeState.INIT_FAILED
        OnDeviceLlmStatus.SDK_MISSING -> OnDeviceProbeState.SDK_MISSING
        OnDeviceLlmStatus.SDK_SIGNATURE_MISMATCH -> OnDeviceProbeState.SDK_SIGNATURE_MISMATCH
        else -> OnDeviceProbeState.INIT_NOT_TESTED
    }

    /** Class name only — never the exception message (which may echo a path or content). */
    private fun safeClass(e: Throwable): String {
        val cause = (e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e
        return cause.javaClass.simpleName.ifBlank { "Throwable" }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 30_000L
    }
}
