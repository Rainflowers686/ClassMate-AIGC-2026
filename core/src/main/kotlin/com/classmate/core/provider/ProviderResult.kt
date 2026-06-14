package com.classmate.core.provider

import com.classmate.core.model.ProviderKind

/**
 * The raw outcome of a single provider call.
 *
 * On [Success], [rawModelText] is the model's text output (expected to contain JSON). It is
 * handed to the parser/validator pipeline but is NEVER logged — it may echo course content.
 * The redacted logger only ever sees the metadata ([provider], [latencyMs], status).
 */
sealed interface ProviderResult {
    val provider: ProviderKind
    val latencyMs: Long

    data class Success(
        override val provider: ProviderKind,
        override val latencyMs: Long,
        val rawModelText: String,
    ) : ProviderResult

    data class Failure(
        override val provider: ProviderKind,
        override val latencyMs: Long,
        val error: ProviderError,
    ) : ProviderResult
}
