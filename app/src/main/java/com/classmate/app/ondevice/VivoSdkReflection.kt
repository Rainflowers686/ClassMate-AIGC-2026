package com.classmate.app.ondevice

import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Reflective handle onto the vivo on-device SDK (`com.vivo.llmsdk.*`). We NEVER `import
 * com.vivo.llmsdk.*` (constraint #17) so the module compiles in CI even when the AAR is absent —
 * every class/method/field is resolved at runtime through reflection.
 *
 * [load] returns:
 *  - [LoadResult.Missing] when the SDK classes are not on the classpath (no AAR), and
 *  - [LoadResult.SignatureMismatch] when classes load but a REQUIRED member is absent or has the
 *    wrong shape (e.g. an old stats-carrying onComplete instead of the no-arg onComplete), and
 *  - [LoadResult.Ok] with cached handles otherwise.
 *
 * The class names default to the official FQNs but are injectable so the reflection contract can be
 * unit-tested against a fake SDK without bundling the real binary.
 */
class VivoSdkReflection private constructor(
    val managerClass: Class<*>,
    val configClass: Class<*>,
    val tokenCallbackClass: Class<*>,
    val initMethod: Method,
    val generateMethod: Method,
    val callVitMethod: Method?,
    val interruptMethod: Method?,
    val releaseMethod: Method?,
    val fields: ConfigFields,
) {
    /** Resolved (possibly null) public fields of `LlmConfig`. `modelPath` is required. */
    class ConfigFields(
        val modelPath: Field,
        val nPredict: Field?,
        val nCtx: Field?,
        val nThreads: Field?,
        val topK: Field?,
        val topP: Field?,
        val temperature: Field?,
        val npuPower: Field?,
        val multimodal: Field?,
    )

    /** True when the SDK exposes both the `multimodal` config field and the `callVit` method. */
    fun supportsMultimodal(): Boolean = fields.multimodal != null && callVitMethod != null

    sealed interface LoadResult {
        data class Ok(val reflection: VivoSdkReflection) : LoadResult
        data object Missing : LoadResult
        data class SignatureMismatch(val detail: String) : LoadResult
    }

    companion object {
        const val MANAGER_CLASS = "com.vivo.llmsdk.LlmManager"
        const val CONFIG_CLASS = "com.vivo.llmsdk.LlmConfig"
        const val TOKEN_CALLBACK_CLASS = "com.vivo.llmsdk.TokenCallback"

        fun load(
            managerName: String = MANAGER_CLASS,
            configName: String = CONFIG_CLASS,
            callbackName: String = TOKEN_CALLBACK_CLASS,
            loader: ClassLoader = VivoSdkReflection::class.java.classLoader!!,
        ): LoadResult {
            // initialize=false: do NOT run static initializers (which may System.loadLibrary the
            // native .so). Native loading is deferred until a manager is actually instantiated.
            val managerClass = try {
                Class.forName(managerName, false, loader)
            } catch (e: Throwable) {
                return LoadResult.Missing
            }
            val configClass = try {
                Class.forName(configName, false, loader)
            } catch (e: Throwable) {
                return LoadResult.Missing
            }
            val callbackClass = try {
                Class.forName(callbackName, false, loader)
            } catch (e: Throwable) {
                return LoadResult.Missing
            }

            if (!callbackClass.isInterface) {
                return LoadResult.SignatureMismatch("TokenCallback is not an interface")
            }
            // The vendor callback uses a NO-ARG onComplete(); reject any stats-carrying variant.
            val onComplete = callbackClass.methods.firstOrNull { it.name == "onComplete" }
                ?: return LoadResult.SignatureMismatch("TokenCallback.onComplete missing")
            if (onComplete.parameterCount != 0) {
                return LoadResult.SignatureMismatch("TokenCallback.onComplete must take no arguments")
            }

            val initMethod = try {
                managerClass.getMethod("init", configClass)
            } catch (e: NoSuchMethodException) {
                return LoadResult.SignatureMismatch("LlmManager.init(LlmConfig) missing")
            }
            if (initMethod.returnType != Int::class.javaPrimitiveType) {
                return LoadResult.SignatureMismatch("LlmManager.init must return int")
            }
            val generateMethod = try {
                managerClass.getMethod("generate", String::class.java, callbackClass)
            } catch (e: NoSuchMethodException) {
                return LoadResult.SignatureMismatch("LlmManager.generate(String, TokenCallback) missing")
            }

            // callVit is optional (text-only SDKs may lack it). It must return int when present.
            val callVitMethod = try {
                managerClass.getMethod(
                    "callVit",
                    ByteArray::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                ).takeIf { it.returnType == Int::class.javaPrimitiveType }
            } catch (e: NoSuchMethodException) {
                null
            }
            val interruptMethod = runCatching { managerClass.getMethod("interrupt") }.getOrNull()
            val releaseMethod = runCatching { managerClass.getMethod("release") }.getOrNull()

            val modelPathField = try {
                configClass.getField("modelPath")
            } catch (e: NoSuchFieldException) {
                return LoadResult.SignatureMismatch("LlmConfig.modelPath missing")
            }
            val fields = ConfigFields(
                modelPath = modelPathField,
                nPredict = optionalField(configClass, "nPredict"),
                nCtx = optionalField(configClass, "nCtx"),
                nThreads = optionalField(configClass, "nThreads"),
                topK = optionalField(configClass, "topK"),
                topP = optionalField(configClass, "topP"),
                temperature = optionalField(configClass, "temperature"),
                npuPower = optionalField(configClass, "npuPower"),
                multimodal = optionalField(configClass, "multimodal"),
            )

            return LoadResult.Ok(
                VivoSdkReflection(
                    managerClass = managerClass,
                    configClass = configClass,
                    tokenCallbackClass = callbackClass,
                    initMethod = initMethod,
                    generateMethod = generateMethod,
                    callVitMethod = callVitMethod,
                    interruptMethod = interruptMethod,
                    releaseMethod = releaseMethod,
                    fields = fields,
                ),
            )
        }

        private fun optionalField(cls: Class<*>, name: String): Field? =
            runCatching { cls.getField(name) }.getOrNull()
    }
}
