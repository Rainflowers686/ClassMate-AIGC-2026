package com.classmate.app.domain

import com.classmate.core.adapter.BlueLMProvider
import com.classmate.core.adapter.CompatibleProvider
import com.classmate.core.adapter.LocalRuleProvider
import com.classmate.core.adapter.ModelCallException
import com.classmate.core.adapter.ModelProvider
import com.classmate.core.adapter.ProviderConfig
import com.classmate.core.network.HttpEngine

/**
 * Maps `config.provider` to a concrete [ModelProvider].
 *
 * Routing (v0.4):
 *  - "demo" / "local" / any unknown value → [LocalRuleProvider]
 *  - "compatible" → [CompatibleProvider]
 *  - "bluelm" → [BlueLMProvider]
 *
 * Resolution itself never fails. Whether the provider can actually run is
 * checked at call time and surfaces as [ModelCallException]. The caller
 * (AnalyzeCourseUseCase) is responsible for the fallback decision.
 */
class ProviderResolver(
    private val providerConfig: ProviderConfig,
    private val httpEngine: HttpEngine,
    private val localRuleProvider: ModelProvider = LocalRuleProvider()
) {
    val requestedName: String = providerConfig.provider.ifBlank { "demo" }

    fun resolvePrimary(): ModelProvider = when (requestedName.lowercase()) {
        "compatible" -> CompatibleProvider(providerConfig.compatible, httpEngine)
        "bluelm" -> BlueLMProvider(providerConfig.bluelm, httpEngine)
        // "demo", "local", or anything else → local rule provider.
        else -> localRuleProvider
    }

    fun localFallback(): ModelProvider = localRuleProvider
}
