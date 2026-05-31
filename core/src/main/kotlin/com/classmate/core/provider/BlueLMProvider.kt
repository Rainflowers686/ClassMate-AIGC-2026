package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder

/**
 * THE PRIMARY PATH. Turns a course into an analysis using vivo's BlueLM (蓝心大模型).
 *
 * The structure here is real (build prompt -> build request body -> sign -> POST -> read
 * envelope -> map status). It is deliberately *inert in round 1*: with the default
 * [NoNetworkTransport] and [UnconfiguredBlueLmSigner] (and placeholder credentials),
 * [isAvailable] is false and [generate] returns CONFIG_MISSING so the resolver falls back.
 * It never fabricates a model response.
 *
 * To go live, inject: a real [HttpTransport], a real [BlueLmSigner], and a [ProviderConfig]
 * whose [Credential.BlueLm] holds the appId/appKey loaded from config.local.json or the
 * debug-only import entry. None of those values belong in the repository.
 */
class BlueLMProvider(
    private val config: ProviderConfig,
    private val promptBuilder: PromptBuilder,
    private val transport: HttpTransport = NoNetworkTransport,
    private val signer: BlueLmSigner = UnconfiguredBlueLmSigner,
    private val clock: () -> Long = System::currentTimeMillis,
) : ModelProvider {

    override val kind: ProviderKind = ProviderKind.BLUELM

    // TODO(bluelm): confirm the exact vivo path & query parameters when wiring the real endpoint.
    private val path = "/vivogpt/completions"

    override fun isAvailable(): Boolean =
        config.enabled &&
            config.hasRealCredential() &&
            transport !== NoNetworkTransport &&
            signer !== UnconfiguredBlueLmSigner

    override fun generate(request: AnalysisRequest): ProviderResult {
        val start = clock()
        if (!isAvailable()) {
            return ProviderResult.Failure(kind, 0, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        }
        return try {
            val prompt = promptBuilder.build(request)
            val body = ChatRequestFactory.build(config.model, prompt)
            val headers = buildMap {
                putAll(signer.authHeaders("POST", path, "", body))
                put("Content-Type", "application/json")
            }
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
        } catch (e: BlueLmNotConfiguredException) {
            ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        } catch (e: Exception) {
            // SECURITY: never propagate e.message — it can contain response content / URLs / keys.
            ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.NETWORK, kind))
        }
    }
}
