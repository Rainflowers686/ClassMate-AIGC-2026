package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.validation.ProviderConfigSafetyCheck

/**
 * Opaque credential holder. `toString()` is overridden everywhere so an accidental log or
 * crash dump shows `***`, never the secret. Credentials are injected at runtime from
 * config.local.json or the debug-only import entry — never from the repo.
 */
sealed interface Credential {
    data object None : Credential {
        override fun toString(): String = "None"
    }

    /** vivo BlueLM uses an appId + appKey pair (HMAC signing happens in the signer seam). */
    data class BlueLm(val appId: String, val appKey: String) : Credential {
        override fun toString(): String = "BlueLm(appId=***, appKey=***)"
    }

    /** OpenAI-compatible endpoints use a bearer key. */
    data class ApiKey(val apiKey: String) : Credential {
        override fun toString(): String = "ApiKey(***)"
    }
}

/** Per-provider configuration. Never serialised with real values into the repo. */
data class ProviderConfig(
    val kind: ProviderKind,
    val enabled: Boolean,
    val baseUrl: String = "",
    val model: String = "",
    val timeoutMs: Long = 30_000,
    val credential: Credential = Credential.None,
) {
    /**
     * Whether this config carries usable, *non-placeholder* credentials. The local fallback
     * needs none; networked providers need real secrets (not "YOUR_BLUELM_APP_ID").
     */
    fun hasRealCredential(): Boolean = when (val c = credential) {
        is Credential.None -> kind == ProviderKind.LOCAL_FALLBACK
        is Credential.BlueLm ->
            ProviderConfigSafetyCheck.isRealSecret(c.appId) && ProviderConfigSafetyCheck.isRealSecret(c.appKey)
        is Credential.ApiKey -> ProviderConfigSafetyCheck.isRealSecret(c.apiKey)
    }
}

/** How the resolver decides when to fall back to the next provider. */
data class ResolverPolicy(
    val fallbackOnNon2xx: Boolean = true,
    val fallbackOnParseError: Boolean = true,
    val fallbackOnValidationError: Boolean = true,
)

/**
 * The whole provider configuration: which providers exist, the fallback [order] (BlueLM
 * first), and the active/primary provider. Loaded from config.local.json or built from
 * [defaults] when nothing is configured.
 */
data class ProviderConfigBundle(
    val primary: ProviderKind,
    val order: List<ProviderKind>,
    val configs: Map<ProviderKind, ProviderConfig>,
    val policy: ResolverPolicy = ResolverPolicy(),
) {
    fun configOf(kind: ProviderKind): ProviderConfig? = configs[kind]

    companion object {
        /**
         * Round-1 default: BlueLM is primary but unconfigured (placeholder creds), then the
         * compatible endpoint (disabled), then the always-on local fallback. With no real
         * keys, the resolver cleanly lands on the local fallback.
         */
        fun defaults(): ProviderConfigBundle = ProviderConfigBundle(
            primary = ProviderKind.BLUELM,
            order = listOf(ProviderKind.BLUELM, ProviderKind.COMPATIBLE, ProviderKind.LOCAL_FALLBACK),
            configs = mapOf(
                ProviderKind.BLUELM to ProviderConfig(
                    kind = ProviderKind.BLUELM,
                    enabled = true,
                    baseUrl = "https://api-ai.vivo.com.cn",
                    model = "vivo-BlueLM-TB-Pro",
                    credential = Credential.None, // injected at runtime; never in repo
                ),
                ProviderKind.COMPATIBLE to ProviderConfig(
                    kind = ProviderKind.COMPATIBLE,
                    enabled = false,
                    credential = Credential.None,
                ),
                ProviderKind.LOCAL_FALLBACK to ProviderConfig(
                    kind = ProviderKind.LOCAL_FALLBACK,
                    enabled = true,
                ),
            ),
        )
    }
}
