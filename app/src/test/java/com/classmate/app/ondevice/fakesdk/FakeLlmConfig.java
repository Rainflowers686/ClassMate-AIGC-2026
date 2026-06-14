package com.classmate.app.ondevice.fakesdk;

/**
 * Test double mirroring the public-field shape of {@code com.vivo.llmsdk.LlmConfig} so the
 * reflection bridge can be exercised without bundling the real AAR. Field names/types match the
 * javap signature exactly (topP/temperature are float).
 */
public class FakeLlmConfig {
    public String modelPath = "";
    public int nPredict;
    public int nCtx;
    public int nThreads;
    public int topK;
    public float topP;
    public float temperature;
    public int npuPower;
    public boolean multimodal;

    public FakeLlmConfig() {
    }
}
