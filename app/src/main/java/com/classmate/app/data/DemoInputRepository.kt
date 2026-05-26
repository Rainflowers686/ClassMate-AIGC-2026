package com.classmate.app.data

import android.content.Context
import com.classmate.core.adapter.DemoProvider
import com.classmate.core.model.CourseAnalysisInput

/**
 * Reads bundled demo files from `assets/`. The home screen uses this to feed
 * the DemoProvider during the probe smoke test.
 */
object DemoInputRepository {

    private const val INPUT_ASSET = "demo_input.json"
    private const val OUTPUT_ASSET = "demo_output.json"

    fun loadDemoInput(context: Context): CourseAnalysisInput {
        val raw = readAsset(context, INPUT_ASSET)
        return DemoProvider.DefaultJson.decodeFromString(
            CourseAnalysisInput.serializer(),
            raw
        )
    }

    /**
     * Returns demo_output.json raw text — DemoProvider takes a JSON string so
     * we don't deserialize twice. The validator handles structural checks.
     */
    fun loadDemoOutputRaw(context: Context): String = readAsset(context, OUTPUT_ASSET)

    private fun readAsset(context: Context, name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }
}
