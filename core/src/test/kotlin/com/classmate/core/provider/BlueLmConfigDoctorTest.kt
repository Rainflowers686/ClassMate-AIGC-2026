package com.classmate.core.provider

import org.junit.Assert.assertEquals
import org.junit.Test

class BlueLmConfigDoctorTest {

    @Test fun bothBlankIsMissing() {
        assertEquals(BlueLmConfigState.MISSING, BlueLmConfigDoctor.classify(null, null))
        assertEquals(BlueLmConfigState.MISSING, BlueLmConfigDoctor.classify("", "   "))
    }

    @Test fun onlyOneFieldIsIncomplete() {
        assertEquals(BlueLmConfigState.INCOMPLETE, BlueLmConfigDoctor.classify("appid12345", ""))
        assertEquals(BlueLmConfigState.INCOMPLETE, BlueLmConfigDoctor.classify("", "realappkey123"))
    }

    @Test fun tooShortIsIncomplete() {
        assertEquals(BlueLmConfigState.INCOMPLETE, BlueLmConfigDoctor.classify("x", "y"))
    }

    @Test fun maskedKeyIsRejected() {
        assertEquals(BlueLmConfigState.MASKED_KEY_INVALID, BlueLmConfigDoctor.classify("appid12345", "ab***yz"))
        assertEquals(BlueLmConfigState.MASKED_KEY_INVALID, BlueLmConfigDoctor.classify("appid12345", "••••1234"))
    }

    @Test fun completeRealCredentialIsReady() {
        assertEquals(BlueLmConfigState.READY, BlueLmConfigDoctor.classify("2026374747", "real-unit-app-key-value"))
    }

    @Test fun configRequiredStatesShareTheConfigRequiredCode() {
        assertEquals("CONFIG_REQUIRED", BlueLmConfigState.MISSING.code)
        assertEquals("CONFIG_REQUIRED", BlueLmConfigState.INCOMPLETE.code)
        assertEquals("READY", BlueLmConfigState.READY.code)
    }
}
