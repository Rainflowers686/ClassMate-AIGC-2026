package com.classmate.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackLabelTest {

    @Test
    fun needMoreExamplesDisplaysAsNeedMorePractice() {
        // Stage 6: user-facing label changed to "需要多练"; the wire name stays for backward compat.
        assertEquals("需要多练", FeedbackType.NEED_MORE_EXAMPLES.displayZh)
        assertEquals("need_more_examples", FeedbackType.NEED_MORE_EXAMPLES.wireName)
        // round-trips from the unchanged wire name (old persisted data still maps correctly)
        assertEquals(FeedbackType.NEED_MORE_EXAMPLES, FeedbackType.fromWire("need_more_examples"))
    }
}
