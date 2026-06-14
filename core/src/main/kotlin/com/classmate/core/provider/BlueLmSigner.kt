package com.classmate.core.provider

/**
 * Seam for vivo BlueLM request authentication.
 *
 * vivo's open API signs each request (an HMAC over the appId/appKey plus a nonce and
 * timestamp) and sends the result in auth headers. That real implementation needs the
 * secret appKey and therefore lives OUTSIDE the repo — it is constructed at runtime from
 * the injected [Credential.BlueLm] (via config.local.json or the debug-only import entry).
 *
 * This interface is the clearly-reserved place for it. The default
 * [UnconfiguredBlueLmSigner] throws, which keeps [BlueLMProvider] honest: with no real
 * signer it reports CONFIG_MISSING and the resolver falls back. Nothing here ever logs the
 * key.
 */
fun interface BlueLmSigner {
    /**
     * @return the auth headers for the request (e.g. Sign / Timestamp / Nonce / AppId).
     */
    fun authHeaders(
        method: String,
        path: String,
        query: String,
        body: String,
    ): Map<String, String>
}

/** Thrown by the default signer so an unconfigured BlueLM provider fails cleanly to fallback. */
class BlueLmNotConfiguredException : RuntimeException("BlueLM signer not configured")

object UnconfiguredBlueLmSigner : BlueLmSigner {
    override fun authHeaders(method: String, path: String, query: String, body: String): Map<String, String> =
        throw BlueLmNotConfiguredException()
}
