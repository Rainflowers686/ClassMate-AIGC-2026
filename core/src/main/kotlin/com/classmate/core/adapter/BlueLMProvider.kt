package com.classmate.core.adapter

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult

/**
 * Placeholder for the vivo BlueLM (蓝心) provider.
 *
 * v0.2.5 status: NOT WIRED. The probe deliberately avoids issuing any real
 * network request. Calling [analyzeCourse] throws so we never accidentally
 * ship a half-done integration that looks like it works.
 *
 * v0.3 will:
 *  1. Build the prompt from PromptBuilder + input;
 *  2. POST to [config].api_base_url with App ID / App Key signing;
 *  3. Parse the response, validate against the JSON schema;
 *  4. Return CourseAnalysisResult.
 */
class BlueLMProvider(
    private val config: ProviderConfig
) : ModelProvider {

    override val name: String = "bluelm"

    override suspend fun analyzeCourse(input: CourseAnalysisInput): CourseAnalysisResult {
        throw NotImplementedError(
            "BlueLM provider not wired in v0.2.5 probe. " +
                "Configured base_url=${config.apiBaseUrl}, app_id_present=${config.appId.isNotBlank()}. " +
                "Fall back to DemoProvider or wait for v0.3."
        )
    }
}

/**
 * Credentials + endpoint loaded from `config.local.json`.
 *
 * Held as plain data so the core module stays free of Android dependencies.
 * The app module is responsible for reading the file and instantiating this.
 */
data class ProviderConfig(
    val provider: String,
    val apiBaseUrl: String,
    val appId: String,
    val appKey: String
)
