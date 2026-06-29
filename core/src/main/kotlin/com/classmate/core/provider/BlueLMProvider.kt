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
    /** Off-main-thread backoff between slow-READ retries; injectable so tests don't actually sleep. */
    private val sleeper: (Long) -> Unit = { if (it > 0) Thread.sleep(it) },
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
        if (!isAvailable()) {
            return ProviderResult.Failure(kind, 0, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        }
        val timeouts = request.intensity?.httpTimeouts() ?: analysisTimeouts
        val readRetries = request.intensity?.readRetries ?: 0
        var attempt = 0
        var last: ProviderResult.Failure = ProviderResult.Failure(kind, 0, ProviderError(ProviderErrorType.UNKNOWN, kind))
        while (true) {
            // On a retry we degrade the request (fewer KP/quiz + smaller token cap) so a slow/long
            // response that aborted the READ has a better chance of completing within the timeout.
            val degraded = attempt > 0
            when (val outcome = attemptGenerate(request, timeouts, degraded)) {
                is ProviderResult.Success -> return outcome
                is ProviderResult.Failure -> {
                    last = outcome
                    val sub = outcome.error.networkSubtype
                    val retryable = outcome.error.type == ProviderErrorType.SOCKET_TIMEOUT ||
                        (outcome.error.type == ProviderErrorType.NETWORK && (sub == "READ" || sub == "SOCKET_TIMEOUT"))
                    if (!retryable || attempt >= readRetries) return last
                    sleeper(BACKOFF_MS.getOrElse(attempt) { BACKOFF_MS.last() })
                    attempt++
                }
            }
        }
    }

    /** One full attempt (prompt build + POST + the documented requestId fallback). Never throws. */
    private fun attemptGenerate(request: AnalysisRequest, timeouts: HttpTimeouts, degraded: Boolean): ProviderResult {
        val start = clock()
        var activeMaxTokens = config.maxTokens
        return try {
            val effectiveRequest = if (degraded) degrade(request) else request
            val prompt = promptBuilder.build(effectiveRequest)
            val maxTokensCap = when {
                effectiveRequest.repairHint != null -> REPAIR_MAX_TOKENS
                degraded -> DEGRADED_MAX_TOKENS
                else -> null
            }
            val profile = request.intensity?.profile ?: CloudLearningTask.COURSE_ANALYSIS.qualityProfile
            val options = profile.toRequestOptions(
                config = config,
                stream = config.stream,
                maxTokensCap = maxTokensCap,
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

            val primary = postOnce(config.requestIdQueryName, requestId, headers, body, start, activeMaxTokens, timeouts)
            if (
                config.requestIdQueryName == "request_id" &&
                primary.retryWithRequestId
            ) {
                return postOnce("requestId", requestId, headers, body, start, activeMaxTokens, timeouts).result
            }
            primary.result
        } catch (e: TransportNotConfiguredException) {
            ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.CONFIG_MISSING, kind))
        } catch (e: TransportDiagnosticException) {
            ProviderResult.Failure(kind, clock() - start, networkError(e, activeMaxTokens, timeouts))
        } catch (e: UnknownHostException) {
            ProviderResult.Failure(kind, clock() - start, networkError("DNS", maxTokens = activeMaxTokens, timeouts = timeouts))
        } catch (e: SSLException) {
            ProviderResult.Failure(kind, clock() - start, networkError("TLS", maxTokens = activeMaxTokens, timeouts = timeouts))
        } catch (e: SocketTimeoutException) {
            ProviderResult.Failure(kind, clock() - start, networkError("SOCKET_TIMEOUT", ProviderErrorType.SOCKET_TIMEOUT, activeMaxTokens, timeouts))
        } catch (e: ConnectException) {
            ProviderResult.Failure(kind, clock() - start, networkError("CONNECT", maxTokens = activeMaxTokens, timeouts = timeouts))
        } catch (e: IOException) {
            ProviderResult.Failure(kind, clock() - start, networkError("UNKNOWN", maxTokens = activeMaxTokens, timeouts = timeouts))
        } catch (e: Exception) {
            // SECURITY: never propagate e.message; it can contain response content, URLs, or keys.
            ProviderResult.Failure(kind, clock() - start, networkError("UNKNOWN", maxTokens = activeMaxTokens, timeouts = timeouts))
        }
    }

    /** Degrade a request for a retry: halve KP, one quiz each — shorter prompt + output. Evidence is untouched. */
    private fun degrade(request: AnalysisRequest): AnalysisRequest = request.copy(
        maxKnowledgePoints = (request.maxKnowledgePoints / 2).coerceAtLeast(4),
        questionsPerKnowledgePoint = 1,
    )

    private fun postOnce(
        queryName: String,
        requestId: String,
        headers: Map<String, String>,
        body: String,
        start: Long,
        maxTokens: Int,
        timeouts: HttpTimeouts,
    ): PostOutcome {
        val url = requestUrl(queryName, requestId)
        val response = transport.postJson(url, headers, body, analysisProfile, timeouts)
        val latency = clock() - start

        if (response.status !in 200..299) {
            return PostOutcome(
                result = ProviderResult.Failure(kind, latency, ProviderError.fromStatus(kind, response.status, response.body).withAnalysisMetadata(maxTokens = maxTokens, timeouts = timeouts)),
                retryWithRequestId = ProviderError.shouldRetryWithRequestId(response.status, response.body),
            )
        }
        val text = responseReader.extractAssistantText(response.body)
        return if (text.isNullOrBlank()) {
            PostOutcome(ProviderResult.Failure(kind, latency, ProviderError(ProviderErrorType.EMPTY_RESPONSE, kind, response.status).withAnalysisMetadata(maxTokens = maxTokens, timeouts = timeouts)))
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

    private fun networkError(error: TransportDiagnosticException, maxTokens: Int, timeouts: HttpTimeouts): ProviderError =
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
            timeouts = timeouts,
        )

    private fun networkError(
        subtype: String,
        type: ProviderErrorType = ProviderErrorType.NETWORK,
        maxTokens: Int,
        timeouts: HttpTimeouts,
    ): ProviderError = ProviderError(type, kind).withAnalysisMetadata(networkSubtype = subtype, maxTokens = maxTokens, timeouts = timeouts)

    private fun ProviderError.withAnalysisMetadata(
        networkSubtype: String? = this.networkSubtype,
        maxTokens: Int,
        timeouts: HttpTimeouts,
    ): ProviderError =
        copy(
            requestProfile = analysisProfile.name,
            timeoutMs = timeouts.readTimeoutMs,
            networkSubtype = networkSubtype,
            model = config.model,
            maxTokens = maxTokens,
        )

    private companion object {
        const val REPAIR_MAX_TOKENS = 1600
        const val DEGRADED_MAX_TOKENS = 2048
        val BACKOFF_MS = longArrayOf(1_000L, 3_000L)
    }
}
