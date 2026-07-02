package com.classmate.core.provider

import com.classmate.core.model.ProviderKind

/**
 * Single value-level resolver for the official BlueLM cloud config. It does not perform I/O
 * and never returns raw credentials unless the config is ready for the real provider path.
 */
sealed interface BlueLMProviderResolution {
    data object MissingConfig : BlueLMProviderResolution
    data object IncompleteConfig : BlueLMProviderResolution
    data object InvalidConfig : BlueLMProviderResolution
    data class Ready(val config: ProviderConfig) : BlueLMProviderResolution
}

object BlueLMProviderResolver {
    fun resolve(bundle: ProviderConfigBundle): BlueLMProviderResolution {
        val config = bundle.configOf(ProviderKind.BLUELM) ?: return BlueLMProviderResolution.MissingConfig
        if (!config.enabled) return BlueLMProviderResolution.MissingConfig
        val credential = config.credential as? Credential.BlueLm ?: return BlueLMProviderResolution.MissingConfig
        return when (BlueLmConfigDoctor.classify(credential.appId, credential.appKey)) {
            BlueLmConfigState.MISSING -> BlueLMProviderResolution.MissingConfig
            BlueLmConfigState.INCOMPLETE -> BlueLMProviderResolution.IncompleteConfig
            BlueLmConfigState.MASKED_KEY_INVALID -> BlueLMProviderResolution.InvalidConfig
            BlueLmConfigState.READY -> BlueLMProviderResolution.Ready(config)
        }
    }

    fun isReady(bundle: ProviderConfigBundle): Boolean =
        resolve(bundle) is BlueLMProviderResolution.Ready
}
