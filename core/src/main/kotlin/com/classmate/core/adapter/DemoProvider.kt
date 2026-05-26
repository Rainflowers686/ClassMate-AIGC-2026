package com.classmate.core.adapter

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult
import kotlinx.serialization.json.Json

/**
 * Returns canned output parsed from a JSON string.
 *
 * The caller is responsible for sourcing the JSON (in the Android app the
 * activity loads it from `assets/demo_output.json`). This class is pure-Kotlin
 * on purpose so the `core` module stays platform-independent.
 *
 * IMPORTANT: this provider does NOT pretend to be a real model call.
 * - [name] is "demo".
 * - Spec §3.2 / §13 forbid passing canned data off as a real model response;
 *   downstream loggers tag every record with the provider name.
 */
class DemoProvider(
    private val demoJson: String,
    private val json: Json = DefaultJson
) : ModelProvider {

    override val name: String = "demo"

    override suspend fun analyzeCourse(input: CourseAnalysisInput): CourseAnalysisResult {
        // The input is intentionally ignored in v0.2.5 — the probe only needs to
        // demonstrate end-to-end render of a known-good payload. v0.3's
        // BlueLMProvider will honor the input fully.
        return json.decodeFromString(CourseAnalysisResult.serializer(), demoJson)
    }

    companion object {
        val DefaultJson: Json = Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = true
        }
    }
}
