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

    /** vivo BlueLM uses appId as the app_id header and appKey as the bearer token. */
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
    val temperature: Double = 0.3,
    val maxTokens: Int = 4096,
    val stream: Boolean = false,
    val requestIdQueryName: String = "request_id",
) {
    /**
     * Whether this config carries usable, *non-placeholder* credentials. The local fallback
     * needs none; networked providers need real secrets (not "YOUR_BLUELM_APP_ID").
     */
    fun hasRealCredential(): Boolean = when (val c = credential) {
        is Credential.None -> kind == ProviderKind.LOCAL_FALLBACK
        is Credential.BlueLm ->
            ProviderConfigSafetyCheck.isRealSecret(c.appId) &&
                ProviderConfigSafetyCheck.isRealSecret(c.appKey)
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
 * The learner-facing model mode. Drives the resolver order so the UI/logs can always say which
 * mode is active. DEMO_COMPATIBLE is an explicitly-labelled "showcase enhancement" path; it must
 * never be presented as the official BlueLM.
 */
enum class LearnerProfile(val wireName: String, val displayName: String) {
    OFFICIAL_BLUELM("official_bluelm", "官方 BlueLM"),
    DEMO_COMPATIBLE("demo_compatible", "展示增强 · Compatible Demo"),
    LOCAL_ONLY("local_only", "本地兜底");

    companion object {
        fun fromWire(value: String?): LearnerProfile? =
            value?.trim()?.lowercase()?.let { v -> entries.firstOrNull { it.wireName == v } }
    }
}

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
    val profile: LearnerProfile = LearnerProfile.OFFICIAL_BLUELM,
) {
    fun configOf(kind: ProviderKind): ProviderConfig? = configs[kind]

    companion object {
        /** The fallback order for each profile (the source of truth for resolver ordering). */
        fun orderFor(profile: LearnerProfile): List<ProviderKind> = when (profile) {
            LearnerProfile.OFFICIAL_BLUELM -> listOf(ProviderKind.BLUELM, ProviderKind.LOCAL_FALLBACK)
            LearnerProfile.DEMO_COMPATIBLE -> listOf(ProviderKind.COMPATIBLE, ProviderKind.BLUELM, ProviderKind.LOCAL_FALLBACK)
            LearnerProfile.LOCAL_ONLY -> listOf(ProviderKind.LOCAL_FALLBACK)
        }

        /** Builds a bundle whose order/primary are derived deterministically from [profile]. */
        fun forProfile(
            profile: LearnerProfile,
            configs: Map<ProviderKind, ProviderConfig>,
            policy: ResolverPolicy = ResolverPolicy(),
        ): ProviderConfigBundle {
            val order = orderFor(profile)
            return ProviderConfigBundle(
                primary = order.first(),
                order = order,
                configs = configs,
                policy = policy,
                profile = profile,
            )
        }

        /**
         * Round-1 default: official BlueLM mode, primary BlueLM (unconfigured), local fallback as
         * the safety net. With no real keys, the resolver cleanly lands on the local fallback.
         */
        fun defaults(): ProviderConfigBundle = forProfile(
            profile = LearnerProfile.OFFICIAL_BLUELM,
            configs = mapOf(
                ProviderKind.BLUELM to ProviderConfig(
                    kind = ProviderKind.BLUELM,
                    enabled = true,
                    baseUrl = "https://api-ai.vivo.com.cn/v1",
                    model = "Doubao-Seed-2.0-pro",
                    temperature = 0.1,
                    maxTokens = 2200,
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
