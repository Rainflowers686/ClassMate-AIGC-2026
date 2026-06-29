package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The analysis source report shows [ProviderError.shortCode] to the user, so a transport failure must
 * stay diagnosable: `BLUELM:NETWORK` alone cannot tell DNS from TLS from connection-refused. The
 * networkSubtype is appended so a real-device tester can act on the right cause.
 */
class ProviderErrorShortCodeTest {

    @Test fun networkSubtypeIsAppendedSoDnsAndTlsAreDistinguishable() {
        val dns = ProviderError(ProviderErrorType.NETWORK, ProviderKind.BLUELM, networkSubtype = "DNS")
        val tls = ProviderError(ProviderErrorType.NETWORK, ProviderKind.BLUELM, networkSubtype = "TLS")
        val connect = ProviderError(ProviderErrorType.NETWORK, ProviderKind.BLUELM, networkSubtype = "CONNECT")
        assertEquals("BLUELM:NETWORK:DNS", dns.shortCode)
        assertEquals("BLUELM:NETWORK:TLS", tls.shortCode)
        assertEquals("BLUELM:NETWORK:CONNECT", connect.shortCode)
    }

    @Test fun redundantSubtypeEqualToTypeIsNotDuplicated() {
        val timeout = ProviderError(ProviderErrorType.SOCKET_TIMEOUT, ProviderKind.BLUELM, networkSubtype = "SOCKET_TIMEOUT")
        assertEquals("BLUELM:SOCKET_TIMEOUT", timeout.shortCode)
    }

    @Test fun configMissingHasNoSubtypeSuffix() {
        val cfg = ProviderError(ProviderErrorType.CONFIG_MISSING, ProviderKind.BLUELM)
        assertEquals("BLUELM:CONFIG_MISSING", cfg.shortCode)
    }

    @Test fun httpStatusAndVendorCodeStillFormatAsBefore() {
        val param = ProviderError(ProviderErrorType.PARAM_ERROR, ProviderKind.BLUELM, httpStatus = 400, vendorCode = "1001")
        assertEquals("BLUELM:PARAM_ERROR:400:1001", param.shortCode)
    }
}
