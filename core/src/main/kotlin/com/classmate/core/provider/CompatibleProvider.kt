package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder

/**
 * Compatible backup path: any OpenAI-compatible chat-completions endpoint, authenticated
 * with a bearer key. Same lifecycle as [BlueLMProvider] — inert until a real transport and a
 * real [Credential.ApiKey] are injected. This exists so the demo/judging is resilient if the
 * primary path is temporarily unavailable, WITHOUT becoming the headline intelligence.
 */
class CompatibleProvider(
    private val config: ProviderConfig,
    private val promptBuilder: PromptBuilder,
    private val transport: HttpTransport = NoNetworkTransport,
    private val clock: () -> Long = System::currentTimeMillis,
) : ModelProvider {

    override val kind: ProviderKind = ProviderKind.COMPATIBLE

    private val path = "/chat/completions"

    override fun isAvailable(): Boolean =
        config.enabled && config.hasRealCredential() && transport !== NoNetworkTransport

    override fun generate(request: AnalysisRequest): ProviderResult {
        val start = clock()
        if (!isAvailable()) {
            return ProviderResult.Failure(kind, 0, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        }
        return try {
            val prompt = promptBuilder.build(request)
            val body = ChatRequestFactory.build(config.model, prompt)
            val apiKey = (config.credential as? Credential.ApiKey)?.apiKey
                ?: return ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
            val headers = mapOf(
                "Authorization" to "Bearer $apiKey", // used only as a header; never logged
                "Content-Type" to "application/json",
            )
            val url = config.baseUrl.trimEnd('/') + path
            val response = transport.postJson(url, headers, body, config.timeoutMs)
            val latency = clock() - start

            if (response.status !in 200..299) {
                return ProviderResult.Failure(kind, latency, ProviderError.fromStatus(kind, response.status))
            }
            val text = VendorResponseReader.extractAssistantText(response.body)
            if (text.isNullOrBlank()) {
                ProviderResult.Failure(kind, latency, ProviderError(ProviderErrorType.EMPTY_RESPONSE, kind, response.status))
            } else {
                ProviderResult.Success(kind, latency, text)
            }
        } catch (e: TransportNotConfiguredException) {
            ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        } catch (e: Exception) {
            ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.NETWORK, kind))
        }
    }
}
