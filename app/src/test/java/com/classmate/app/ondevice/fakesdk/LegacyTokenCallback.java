package com.classmate.app.ondevice.fakesdk;

/**
 * The FORBIDDEN old callback shape — onComplete takes an argument. Used to prove the reflection
 * loader rejects it with SDK_SIGNATURE_MISMATCH instead of silently mis-binding.
 */
public interface LegacyTokenCallback {
    void onToken(String token);

    void onComplete(Object stats); // wrong: vendor onComplete() is no-arg

    void onError(int code, String message);
}
