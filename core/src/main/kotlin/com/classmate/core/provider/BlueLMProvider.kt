package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * THE PRIMARY PATH. Turns a course into an analysis using vivo AIGC cloud text generation.
 *
 * Official MVP protocol used here:
 * - POST https://api-ai.vivo.com.cn/v1/chat/completions
 * - Authorization: Bearer <AppKey>
 * - query request_id by default, with one requestId retry for the documented 1001 fallback case
 * - OpenAI-compatible chat-completions body and choices[0].message.content response.
 *
 * It remains inert unless a runtime credential and a real [HttpTransport] are injected.
 */
class BlueLMProvider(
    private val config: ProviderConfig,
    private val promptBuilder: PromptBuilder,
    private val transport: HttpTransport = NoNetworkTransport,
    @Suppress("UNUSED_PARAMETER")
    signer: BlueLmSigner = UnconfiguredBlueLmSigner,
    private val requestFactory: BlueLMRequestFactory = VivoOpenAIChatRequestFactory,
    private val responseReader: BlueLMResponseReader = VivoOpenAIChatResponseReader,
    private val requestIdFactory: () -> String = { "cm_" + UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
) : ModelProvider {

    override val kind: ProviderKind = ProviderKind.BLUELM

    private val path = "/chat/completions"

    override fun isAvailable(): Boolean =
        config.enabled &&
            config.hasRealCredential() &&
            transport !== NoNetworkTransport

    override fun generate(request: AnalysisRequest): ProviderResult {
        val start = clock()
        if (!isAvailable()) {
            return ProviderResult.Failure(kind, 0, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        }
        return try {
            val prompt = promptBuilder.build(request)
            val body = requestFactory.build(
                model = config.model,
                prompt = prompt,
                options = BlueLMRequestOptions(
                    stream = config.stream,
                    temperature = config.temperature,
                    maxTokens = config.maxTokens,
                ),
            )
            val appKey = (config.credential as? Credential.BlueLm)?.appKey
                ?: return ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
            val headers = mapOf(
                "Authorization" to "Bearer $appKey",
                "Content-Type" to "application/json; charset=utf-8",
            )
            val requestId = requestIdFactory()

            val primary = postOnce(config.requestIdQueryName, requestId, headers, body, start)
            if (
                config.requestIdQueryName == "request_id" &&
                primary.retryWithRequestId
            ) {
                return postOnce("requestId", requestId, headers, body, start).result
            }
            primary.result
        } catch (e: TransportNotConfiguredException) {
            ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        } catch (e: Exception) {
            // SECURITY: never propagate e.message; it can contain response content, URLs, or keys.
            ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.NETWORK, kind))
        }
    }

    private fun postOnce(
        queryName: String,
        requestId: String,
        headers: Map<String, String>,
        body: String,
        start: Long,
    ): PostOutcome {
        val url = requestUrl(queryName, requestId)
        val response = transport.postJson(url, headers, body, config.timeoutMs)
        val latency = clock() - start

        if (response.status !in 200..299) {
            return PostOutcome(
                result = ProviderResult.Failure(kind, latency, ProviderError.fromStatus(kind, response.status, response.body)),
                retryWithRequestId = ProviderError.shouldRetryWithRequestId(response.status, response.body),
            )
        }
        val text = responseReader.extractAssistantText(response.body)
        return if (text.isNullOrBlank()) {
            PostOutcome(ProviderResult.Failure(kind, latency, ProviderError(ProviderErrorType.EMPTY_RESPONSE, kind, response.status)))
        } else {
            PostOutcome(ProviderResult.Success(kind, latency, text))
        }
    }

    private fun requestUrl(queryName: String, requestId: String): String {
        val encodedName = URLEncoder.encode(queryName, StandardCharsets.UTF_8.name())
        val encodedId = URLEncoder.encode(requestId, StandardCharsets.UTF_8.name())
        return config.baseUrl.trimEnd('/') + path + "?$encodedName=$encodedId"
    }

    private data class PostOutcome(
        val result: ProviderResult,
        val retryWithRequestId: Boolean = false,
    )
}
