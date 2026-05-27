package com.classmate.core.adapter

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.network.HttpEngine

/**
 * Safe-wired placeholder for the vivo BlueLM (蓝心) provider.
 *
 * Status: the official API contract — endpoint URL pattern, request body
 * shape, App Key signing scheme, response envelope — is NOT in this repo.
 * Until that information is supplied through official docs, this provider
 * MUST NOT issue a network request. Inventing the signing scheme would just
 * yield a stub that fails authentication while looking like a real
 * integration, which is the opposite of what the contest expects.
 *
 * What is wired:
 *  - config reading via [com.classmate.core.adapter.BlueLmConfig];
 *  - dependency on [HttpEngine] so the eventual real implementation has no
 *    extra plumbing to add;
 *  - clear, typed failure via [ModelCallException].
 *
 * To go live, supply (and reference here):
 *   - the exact base URL for chat-style course analysis;
 *   - the request body schema (model name, temperature, message shape);
 *   - the signature algorithm (header names, payload to sign, hash);
 *   - the response envelope (where to read the JSON content from).
 */
class BlueLMProvider(
    private val config: BlueLmConfig,
    @Suppress("unused") private val httpEngine: HttpEngine,
    @Suppress("unused") private val promptBuilder: (CourseAnalysisInput) -> String = PromptBuilder::build
) : ModelProvider {

    override val name: String = "bluelm"

    override suspend fun analyzeCourse(input: CourseAnalysisInput): CourseAnalysisResult {
        if (!config.isUsable()) {
            throw ModelCallException(
                ModelCallException.Reason.CONFIG_MISSING,
                "BlueLM config is missing or contains placeholders (base_url present=${config.apiBaseUrl.isNotBlank()}, " +
                    "app_id present=${config.appId.isNotBlank()}, app_key present=${config.appKey.isNotBlank()}, " +
                    "model present=${config.model.isNotBlank()})"
            )
        }
        throw ModelCallException(
            ModelCallException.Reason.PROVIDER_NOT_IMPLEMENTED,
            "BlueLM provider is not configured or official API contract is missing. " +
                "Endpoint/signing scheme must be supplied from official docs before any real call is made."
        )
    }
}
