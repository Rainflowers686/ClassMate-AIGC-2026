package com.classmate.app.ondevice.fakesdk;

/**
 * Mirrors {@code com.vivo.llmsdk.TokenCallback}: a NO-ARG {@link #onComplete()} (not the old
 * onComplete(LlmStats)). The bridge implements this via a dynamic Proxy.
 */
public interface FakeTokenCallback {
    void onToken(String token);

    void onComplete();

    void onError(int code, String message);
}
