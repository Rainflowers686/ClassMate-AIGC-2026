package com.classmate.app.ondevice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VivoSdkReflectionTest {

    private val fakeManager = "com.classmate.app.ondevice.fakesdk.FakeLlmManager"
    private val fakeConfig = "com.classmate.app.ondevice.fakesdk.FakeLlmConfig"
    private val fakeCallback = "com.classmate.app.ondevice.fakesdk.FakeTokenCallback"
    private val legacyCallback = "com.classmate.app.ondevice.fakesdk.LegacyTokenCallback"

    @Test
    fun defaultClassNamesAreTheOfficialVivoFqns() {
        assertEquals("com.vivo.llmsdk.LlmManager", VivoSdkReflection.MANAGER_CLASS)
        assertEquals("com.vivo.llmsdk.LlmConfig", VivoSdkReflection.CONFIG_CLASS)
        assertEquals("com.vivo.llmsdk.TokenCallback", VivoSdkReflection.TOKEN_CALLBACK_CLASS)
    }

    @Test
    fun resolvesFakeSdkWithCorrectSignaturesAndMultimodalSupport() {
        val result = VivoSdkReflection.load(fakeManager, fakeConfig, fakeCallback)
        assertTrue(result is VivoSdkReflection.LoadResult.Ok)
        val refl = (result as VivoSdkReflection.LoadResult.Ok).reflection

        assertEquals(Int::class.javaPrimitiveType, refl.initMethod.returnType)
        assertNotNull(refl.callVitMethod)
        assertEquals(Int::class.javaPrimitiveType, refl.callVitMethod!!.returnType)
        assertNotNull(refl.fields.multimodal)
        assertNotNull(refl.fields.topP) // float field resolved
        assertTrue(refl.supportsMultimodal())
        assertTrue(refl.tokenCallbackClass.isInterface)
    }

    @Test
    fun absentSdkClassesYieldMissing() {
        val result = VivoSdkReflection.load(
            "com.classmate.app.ondevice.fakesdk.NoSuchManager",
            "com.classmate.app.ondevice.fakesdk.NoSuchConfig",
            "com.classmate.app.ondevice.fakesdk.NoSuchCallback",
        )
        assertTrue(result is VivoSdkReflection.LoadResult.Missing)
    }

    @Test
    fun legacyOnCompleteWithArgumentIsRejectedAsSignatureMismatch() {
        // The vendor TokenCallback.onComplete() is NO-ARG; an old onComplete(LlmStats)-style callback
        // must be rejected rather than silently mis-bound.
        val result = VivoSdkReflection.load(fakeManager, fakeConfig, legacyCallback)
        assertTrue(result is VivoSdkReflection.LoadResult.SignatureMismatch)
    }
}
