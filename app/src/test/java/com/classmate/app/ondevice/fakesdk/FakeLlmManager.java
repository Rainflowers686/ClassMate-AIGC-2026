package com.classmate.app.ondevice.fakesdk;

/**
 * Test double mirroring {@code com.vivo.llmsdk.LlmManager}: {@code int init(LlmConfig)},
 * {@code int callVit(byte[],int,int)}, {@code void generate(String, TokenCallback)}, interrupt,
 * release. Behaviour is driven by the configured {@code modelPath} so a single class can deterministi
 * -cally exercise every bridge path without threads or a looper.
 *
 *  - modelPath contains "INIT_FAIL" -> init returns -5
 *  - modelPath contains "VIT_FAIL"  -> callVit returns -7
 *  - modelPath contains "GEN_ERROR" -> generate calls onError(42, ...)
 *  - modelPath contains "HANG"      -> generate never calls back (exercises the timeout guard)
 *  - otherwise                       -> init/callVit return 0, generate emits 2 tokens + onComplete()
 */
public class FakeLlmManager {
    private String modelPath = "";
    public volatile boolean released = false;
    public volatile boolean interrupted = false;

    public FakeLlmManager() {
    }

    public int init(FakeLlmConfig config) {
        this.modelPath = config.modelPath == null ? "" : config.modelPath;
        return modelPath.contains("INIT_FAIL") ? -5 : 0;
    }

    public int callVit(byte[] data, int width, int height) {
        if (modelPath.contains("VIT_FAIL")) return -7;
        // Validates the vendor RGB contract end-to-end: length must be width*height*3.
        if (data == null || data.length != width * height * 3) return -99;
        return 0;
    }

    public void generate(String prompt, FakeTokenCallback callback) {
        if (modelPath.contains("HANG")) return; // never calls back
        if (modelPath.contains("GEN_ERROR")) {
            callback.onError(42, "boom");
            return;
        }
        callback.onToken("端侧");
        callback.onToken("回答");
        callback.onComplete(); // NO-ARG completion
    }

    public void interrupt() {
        interrupted = true;
    }

    public void release() {
        released = true;
    }
}
