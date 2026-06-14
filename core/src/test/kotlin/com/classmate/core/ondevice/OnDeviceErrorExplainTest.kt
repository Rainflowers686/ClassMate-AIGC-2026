package com.classmate.core.ondevice

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceErrorExplainTest {

    @Test
    fun mapsKnownInitCodesToChinese() {
        assertTrue(OnDeviceErrorExplain.explain("INIT_-2105")!!.contains("Tokenizer"))
        assertTrue(OnDeviceErrorExplain.explain("INIT_-2104")!!.contains("VIT"))
        assertTrue(OnDeviceErrorExplain.explain("INIT_-2101")!!.contains("基座模型"))
        assertTrue(OnDeviceErrorExplain.explain("INIT_-1001")!!.contains("config"))
        assertTrue(OnDeviceErrorExplain.explain("TIMEOUT")!!.contains("超时"))
    }

    @Test
    fun minus2105MentionsTokenizerFileAndPermissionHint() {
        val text = OnDeviceErrorExplain.explain("INIT_-2105")!!
        assertTrue(text.contains("bluelm_3b_model_vocab.bin"))
        assertTrue(text.contains("/sdcard/1225"))
        assertTrue(text.contains("权限"))
    }

    @Test
    fun mapsPermissionAndTextInitSentinels() {
        assertNotNull(OnDeviceErrorExplain.explain(OnDeviceErrorExplain.ALL_FILES_ACCESS_REQUIRED))
        assertNotNull(OnDeviceErrorExplain.explain(OnDeviceErrorExplain.TEXT_INIT_REQUIRED))
    }

    @Test
    fun unknownOrBlankReturnsNullNoRawStack() {
        assertNull(OnDeviceErrorExplain.explain(null))
        assertNull(OnDeviceErrorExplain.explain(""))
        assertNull(OnDeviceErrorExplain.explain("   "))
        assertNull(OnDeviceErrorExplain.explain("ONDEVICE_99"))
    }
}
