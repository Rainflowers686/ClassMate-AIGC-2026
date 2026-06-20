package com.classmate.app.l3

import com.classmate.app.data.AppCaptureTransport
import com.classmate.app.platform.CaptureConfigLoader
import com.classmate.core.capture.CaptureTransport
import com.classmate.core.capture.VivoEmbeddingProvider
import com.classmate.core.capture.VivoQueryRewriteProvider
import com.classmate.core.capture.VivoTextSimilarityProvider
import com.classmate.core.retrieval.VivoEmbeddingLearningProvider
import com.classmate.core.retrieval.VivoQueryRewriteLearningProvider
import com.classmate.core.retrieval.VivoTextSimilarityLearningProvider

/**
 * Production factory for official runtime wiring. It may read local config at app runtime through
 * [CaptureConfigLoader], but it never logs or exposes credential values; missing config still returns
 * ConfigMissing through the Vivo providers and the L3 pipeline falls back locally.
 */
object OfficialRuntimeGatewayFactory {
    fun production(
        configLoader: CaptureConfigLoader = CaptureConfigLoader(),
        transport: CaptureTransport = AppCaptureTransport(),
    ): OfficialRuntimeGateway {
        val config = configLoader.load()
        return ProviderBackedOfficialRuntimeGateway(
            queryRewriteProvider = VivoQueryRewriteLearningProvider(
                VivoQueryRewriteProvider(config = config, transport = transport),
            ),
            embeddingProvider = VivoEmbeddingLearningProvider(
                VivoEmbeddingProvider(config = config, transport = transport),
            ),
            textSimilarityProvider = VivoTextSimilarityLearningProvider(
                VivoTextSimilarityProvider(config = config, transport = transport),
            ),
        )
    }
}
