package com.classmate.app.domain

import com.classmate.core.adapter.ProviderConfig
import com.classmate.core.network.HttpEngine
import com.classmate.core.network.HttpRequest
import com.classmate.core.network.HttpResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProviderResolverTest {

    @Test
    fun requestedName_normalizesDemoLocalAndBlankToLocal() {
        listOf("demo", "local", "").forEach { provider ->
            val resolver = ProviderResolver(
                providerConfig = ProviderConfig(provider = provider),
                httpEngine = NoopHttpEngine
            )

            assertEquals("local", resolver.requestedName)
            assertEquals("local", resolver.resolvePrimary().name)
            assertFalse(resolver.requestedDisplayName.contains("demo", ignoreCase = true))
        }
    }

    private object NoopHttpEngine : HttpEngine {
        override suspend fun execute(request: HttpRequest): HttpResponse =
            error("No HTTP call expected in ProviderResolver tests")
    }
}
