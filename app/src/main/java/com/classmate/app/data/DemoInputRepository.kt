package com.classmate.app.data

import android.content.Context
import com.classmate.core.model.CourseAnalysisInput
import kotlinx.serialization.json.Json

/**
 * Reads the bundled demo input from `assets/demo_input.json`.
 *
 * v0.4 removed `loadDemoOutputRaw` — DemoProvider's static replay is gone;
 * LocalRuleProvider now produces a result from the real input instead.
 */
object DemoInputRepository {

    private const val INPUT_ASSET = "demo_input.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadDemoInput(context: Context): CourseAnalysisInput {
        val raw = context.assets.open(INPUT_ASSET).bufferedReader().use { it.readText() }
        return json.decodeFromString(CourseAnalysisInput.serializer(), raw)
    }
}
