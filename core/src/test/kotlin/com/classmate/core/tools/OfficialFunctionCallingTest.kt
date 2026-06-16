package com.classmate.core.tools

import com.classmate.core.capture.CaptureProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialFunctionCallingTest {
    @Test
    fun parserAdaptsOfficialToolProposalToInternalTool() {
        val raw = """{"tool":"searchEvidence","arguments":{"query":"series","courseTitle":"math"}}"""
        val parsed = OfficialFunctionCallParser.parse(raw)

        assertEquals(FunctionCallingStatus.TOOL_PROPOSED, parsed.status)
        val call = OfficialToolAdapter.adapt(parsed.proposal!!)
        assertNotNull(call)
        assertEquals(InternalToolName.SEARCH_EVIDENCE, call!!.name)
        assertEquals("series", call.query)
        assertFalse(OfficialToolAdapter.requiresConfirmation(call))
    }

    @Test
    fun stateChangingOfficialToolRequiresConfirmation() {
        val raw = """{"tool":"createPractice","arguments":{"courseTitle":"math","now":"1700000000000"}}"""
        val parsed = OfficialFunctionCallParser.parse(raw)
        val call = OfficialToolAdapter.adapt(parsed.proposal!!)!!

        assertEquals(InternalToolName.CREATE_PRACTICE, call.name)
        assertTrue(OfficialToolAdapter.requiresConfirmation(call))
    }

    @Test
    fun invalidToolIsRejectedBeforeInternalRouter() {
        val raw = """{"tool":"deleteCourse","arguments":{"courseTitle":"math"}}"""
        val parsed = OfficialFunctionCallParser.parse(raw)

        assertEquals(FunctionCallingStatus.INVALID_TOOL, parsed.status)
        assertTrue(parsed.proposal == null)
    }

    @Test
    fun configMissingProviderDoesNotCallModelOutput() {
        var invoked = false
        val provider = VivoFunctionCallingProvider(
            config = CaptureProviderConfig.ABSENT,
            modelOutput = { _, _ -> invoked = true; "{}" },
        )

        val result = provider.propose("make practice", InternalToolName.entries)

        assertEquals(FunctionCallingStatus.CONFIG_MISSING, result.status)
        assertFalse(invoked)
    }
}
