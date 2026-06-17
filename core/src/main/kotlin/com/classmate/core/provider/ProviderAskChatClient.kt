package com.classmate.core.provider

import com.classmate.core.ask.AskChatResult
import com.classmate.core.ask.AskChatSeam
import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.Prompt
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Routes an "Ask This Lesson" chat call through the SAME profile order and credentials the analyzer
 * uses. It does NOT bypass the resolver philosophy: it walks [ProviderConfigBundle.order]
 * (official_bluelm: BlueLM→Local; demo_compatible: Compatible→BlueLM→Local; local_only: Local only),
 * reusing [VivoOpenAIChatRequestFactory] so qwen3.5-plus keeps `enable_thinking=false`.
 *
 * Returns null when only the local fallback remains (e.g. local_only, or no real transport), which
 * tells the engine to answer from local evidence instead of the network. It never logs or returns the
 * prompt, request body, or raw vendor response — only assistant text + provider kind + model label.
 */
class ProviderAskChatClient(
    private val bundle: ProviderConfigBundle,
    private val transport: HttpTransport = NoNetworkTransport,
    private val requestIdFactory: () -> String = { "cm_ask_" + UUID.randomUUID() },
) : AskChatSeam {

    override fun chat(prompt: Prompt, repairHint: String?): AskChatResult? {
        if (transport === NoNetworkTransport) return null
        for (kind in bundle.order) {
            val config = bundle.configOf(kind) ?: continue
            if (!config.enabled) continue
            val result = when (kind) {
                ProviderKind.BLUELM -> callBlueLm(config, prompt)
                ProviderKind.COMPATIBLE -> callCompatible(config, prompt)
                ProviderKind.LOCAL_FALLBACK -> null // no network chat; caller uses local evidence
            }
            if (result != null) return result
        }
        return null
    }

    private fun callBlueLm(config: ProviderConfig, prompt: Prompt): AskChatResult? {
        if (!config.hasRealCredential()) return null
        val cred = config.credential as? Credential.BlueLm ?: return null
        return try {
            val body = VivoOpenAIChatRequestFactory.build(
                model = config.model,
                prompt = prompt,
                options = CloudLearningTask.ASK_WITH_EVIDENCE.qualityProfile.toRequestOptions(
                    config = config,
                    stream = false,
                    maxTokensCap = ASK_MAX_TOKENS,
                ),
            )
            val headers = mapOf(
                "Authorization" to "Bearer ${cred.appKey}",
                "app_id" to cred.appId,
                "Content-Type" to "application/json; charset=utf-8",
            )
            val name = URLEncoder.encode(config.requestIdQueryName, StandardCharsets.UTF_8.name())
            val id = URLEncoder.encode(requestIdFactory(), StandardCharsets.UTF_8.name())
            val url = config.baseUrl.trimEnd('/') + "/chat/completions?$name=$id"
            val response = transport.postJson(url, headers, body, HttpRequestProfile.ANALYSIS, HttpTimeouts.BLUE_LM_ANALYSIS)
            if (response.status !in 200..299) return null
            VivoOpenAIChatResponseReader.extractAssistantText(response.body)
                ?.takeIf { it.isNotBlank() }
                ?.let { AskChatResult(it, "BLUELM", config.model) }
        } catch (e: Exception) {
            // SECURITY: never surface e.message; it may contain response content or a URL.
            null
        }
    }

    private fun callCompatible(config: ProviderConfig, prompt: Prompt): AskChatResult? {
        if (!config.hasRealCredential()) return null
        val cred = config.credential as? Credential.ApiKey ?: return null
        return try {
            val body = ChatRequestFactory.build(config.model, prompt)
            val headers = mapOf(
                "Authorization" to "Bearer ${cred.apiKey}",
                "Content-Type" to "application/json; charset=utf-8",
            )
            val base = config.baseUrl.trimEnd('/')
            val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
            val response = transport.postJson(url, headers, body, config.timeoutMs)
            if (response.status !in 200..299) return null
            VendorResponseReader.extractAssistantText(response.body)
                ?.takeIf { it.isNotBlank() }
                ?.let { AskChatResult(it, "COMPATIBLE", config.model) }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val ASK_MAX_TOKENS = 1800
    }
}
