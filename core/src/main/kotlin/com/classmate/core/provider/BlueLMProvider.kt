package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.net.ssl.SSLException

/**
 * THE PRIMARY PATH. Turns a course into an analysis using vivo AIGC cloud text generation.
 *
 * Official MVP protocol used here:
 * - POST https://api-ai.vivo.com.cn/v1/chat/completions
 * - Authorization: Bearer <AppKey>
 * - app_id: <AppID>
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
    private val analysisProfile = HttpRequestProfile.ANALYSIS
    private val analysisTimeouts = HttpTimeouts.BLUE_LM_ANALYSIS

    override fun isAvailable(): Boolean =
        config.enabled &&
            config.hasRealCredential() &&
            transport !== NoNetworkTransport

    override fun generate(request: AnalysisRequest): ProviderResult {
        val start = clock()
        var activeMaxTokens = config.maxTokens
        if (!isAvailable()) {
            return ProviderResult.Failure(kind, 0, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        }
        return try {
            val prompt = promptBuilder.build(request)
            val options = CloudLearningTask.COURSE_ANALYSIS.qualityProfile.toRequestOptions(
                config = config,
                stream = config.stream,
                maxTokensCap = if (request.repairHint != null) REPAIR_MAX_TOKENS else null,
            )
            activeMaxTokens = options.maxTokens
            val body = requestFactory.build(
                model = config.model,
                prompt = prompt,
                options = options,
            )
            val credential = config.credential as? Credential.BlueLm
                ?: return ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
            val headers = mapOf(
                "Authorization" to "Bearer ${credential.appKey}",
                "app_id" to credential.appId,
                "Content-Type" to "application/json; charset=utf-8",
            )
            val requestId = requestIdFactory()

            val primary = postOnce(config.requestIdQueryName, requestId, headers, body, start, activeMaxTokens)
            if (
                config.requestIdQueryName == "request_id" &&
                primary.retryWithRequestId
            ) {
                return postOnce("requestId", requestId, headers, body, start, activeMaxTokens).result
            }
            primary.result
        } catch (e: TransportNotConfiguredException) {
            ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        } catch (e: TransportDiagnosticException) {
            ProviderResult.Failure(kind, clock() - start, networkError(e, activeMaxTokens))
        } catch (e: UnknownHostException) {
            ProviderResult.Failure(kind, clock() - start, networkError("DNS", maxTokens = activeMaxTokens))
        } catch (e: SSLException) {
            ProviderResult.Failure(kind, clock() - start, networkError("TLS", maxTokens = activeMaxTokens))
        } catch (e: SocketTimeoutException) {
            ProviderResult.Failure(kind, clock() - start, networkError("SOCKET_TIMEOUT", ProviderErrorType.SOCKET_TIMEOUT, activeMaxTokens))
        } catch (e: ConnectException) {
            ProviderResult.Failure(kind, clock() - start, networkError("CONNECT", maxTokens = activeMaxTokens))
        } catch (e: IOException) {
            ProviderResult.Failure(kind, clock() - start, networkError("UNKNOWN", maxTokens = activeMaxTokens))
        } catch (e: Exception) {
            // SECURITY: never propagate e.message; it can contain response content, URLs, or keys.
            ProviderResult.Failure(kind, clock() - start, networkError("UNKNOWN", maxTokens = activeMaxTokens))
        }
    }

    private fun postOnce(
        queryName: String,
        requestId: String,
        headers: Map<String, String>,
        body: String,
        start: Long,
        maxTokens: Int,
    ): PostOutcome {
        val url = requestUrl(queryName, requestId)
        val response = transport.postJson(url, headers, body, analysisProfile, analysisTimeouts)
        val latency = clock() - start

        if (response.status !in 200..299) {
            return PostOutcome(
                result = ProviderResult.Failure(kind, latency, ProviderError.fromStatus(kind, response.status, response.body).withAnalysisMetadata(maxTokens = maxTokens)),
                retryWithRequestId = ProviderError.shouldRetryWithRequestId(response.status, response.body),
            )
        }
        val text = responseReader.extractAssistantText(response.body)
        return if (text.isNullOrBlank()) {
            PostOutcome(ProviderResult.Failure(kind, latency, ProviderError(ProviderErrorType.EMPTY_RESPONSE, kind, response.status).withAnalysisMetadata(maxTokens = maxTokens)))
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

    private fun networkError(error: TransportDiagnosticException, maxTokens: Int): ProviderError =
        networkError(
            subtype = when (error.subtype) {
                BlueLMDiagnosticSubtype.UNKNOWN_HOST -> "DNS"
                BlueLMDiagnosticSubtype.SSL -> "TLS"
                BlueLMDiagnosticSubtype.CONNECT_EXCEPTION -> "CONNECT"
                BlueLMDiagnosticSubtype.SOCKET_TIMEOUT -> "SOCKET_TIMEOUT"
                BlueLMDiagnosticSubtype.IO -> when (error.stage) {
                    BlueLMDiagnosticStage.CONNECT -> "CONNECT"
                    BlueLMDiagnosticStage.WRITE -> "WRITE"
                    BlueLMDiagnosticStage.READ -> "READ"
                    else -> "UNKNOWN"
                }
                else -> "UNKNOWN"
            },
            type = if (error.subtype == BlueLMDiagnosticSubtype.SOCKET_TIMEOUT) {
                ProviderErrorType.SOCKET_TIMEOUT
            } else {
                ProviderErrorType.NETWORK
            },
            maxTokens = maxTokens,
        )

    private fun networkError(
        subtype: String,
        type: ProviderErrorType = ProviderErrorType.NETWORK,
        maxTokens: Int,
    ): ProviderError = ProviderError(type, kind).withAnalysisMetadata(networkSubtype = subtype, maxTokens = maxTokens)

    private fun ProviderError.withAnalysisMetadata(
        networkSubtype: String? = this.networkSubtype,
        maxTokens: Int,
    ): ProviderError =
        copy(
            requestProfile = analysisProfile.name,
            timeoutMs = analysisTimeouts.readTimeoutMs,
            networkSubtype = networkSubtype,
            model = config.model,
            maxTokens = maxTokens,
        )

    private companion object {
        const val REPAIR_MAX_TOKENS = 1600
    }
}
