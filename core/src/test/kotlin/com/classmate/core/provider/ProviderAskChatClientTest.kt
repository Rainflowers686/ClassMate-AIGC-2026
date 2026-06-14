package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.Prompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderAskChatClientTest {

    private val prompt = Prompt("system", "user")

    @Test
    fun localOnlyProfileMakesNoNetworkCall() {
        var calls = 0
        val transport = HttpTransport { _, _, _, _ -> calls++; TransportResponse(200, "{}") }
        val bundle = ProviderConfigBundle(
            primary = ProviderKind.LOCAL_FALLBACK,
            order = listOf(ProviderKind.LOCAL_FALLBACK),
            configs = mapOf(ProviderKind.LOCAL_FALLBACK to ProviderConfig(ProviderKind.LOCAL_FALLBACK, enabled = true)),
        )
        assertNull(ProviderAskChatClient(bundle, transport).chat(prompt, null))
        assertEquals(0, calls)
    }

    @Test
    fun noNetworkTransportReturnsNullWithoutCalling() {
        // Default bundle is official_bluelm, but with no real transport there is no network chat.
        assertNull(ProviderAskChatClient(ProviderConfigBundle.defaults()).chat(prompt, null))
    }

    @Test
    fun unconfiguredBlueLmDoesNotReachNetwork() {
        var calls = 0
        val transport = HttpTransport { _, _, _, _ -> calls++; TransportResponse(200, "{}") }
        // Default BlueLM config has no real credential -> client must not attempt the call.
        assertNull(ProviderAskChatClient(ProviderConfigBundle.defaults(), transport).chat(prompt, null))
        assertEquals(0, calls)
    }
}
