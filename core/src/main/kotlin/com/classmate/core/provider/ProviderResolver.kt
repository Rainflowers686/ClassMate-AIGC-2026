package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder

/**
 * Constructs the configured providers and exposes them in fallback priority order — and
 * NOTHING ELSE leaves this class. The presentation layer never holds a [ModelProvider] (and
 * therefore never holds a credential); it only ever calls the higher-level CourseAnalyzer,
 * which asks the resolver for the ordered list. This is how "no raw provider leaks to the UI"
 * is enforced structurally.
 *
 * The networked providers are wired with the injected [transport]/[blueLmSigner]. In round 1
 * these default to the no-op stand-ins, so [providersInOrder] yields BlueLM (inert) ->
 * Compatible (inert) -> LocalFallback (always works).
 */
class ProviderResolver(
    private val bundle: ProviderConfigBundle,
    private val promptBuilder: PromptBuilder,
    private val transport: HttpTransport = NoNetworkTransport,
    private val blueLmSigner: BlueLmSigner = UnconfiguredBlueLmSigner,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val primary: ProviderKind get() = bundle.primary

    fun providersInOrder(): List<ModelProvider> = bundle.order.mapNotNull { build(it) }

    /** True when the configured primary (BlueLM) could actually run right now. */
    fun isPrimaryReady(): Boolean =
        providersInOrder().firstOrNull { it.kind == primary }?.isAvailable() == true

    private fun build(kind: ProviderKind): ModelProvider? {
        val config = bundle.configOf(kind) ?: return null
        return when (kind) {
            ProviderKind.BLUELM ->
                if (!config.enabled) null
                else BlueLMProvider(config, promptBuilder, transport, blueLmSigner, clock)

            ProviderKind.COMPATIBLE ->
                if (!config.enabled) null
                else CompatibleProvider(config, promptBuilder, transport, clock)

            ProviderKind.LOCAL_FALLBACK ->
                if (!config.enabled) null
                else LocalFallbackProvider(clock = clock)
        }
    }
}
