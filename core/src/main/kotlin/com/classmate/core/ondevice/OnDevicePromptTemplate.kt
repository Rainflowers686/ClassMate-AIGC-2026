package com.classmate.core.ondevice

/**
 * The vendor-defined prompt format for the on-device BlueLM 3B model:
 *
 *     [|Human|]:用户输入
 *     [|AI|]:
 *
 * Raw prompt text is NEVER logged or persisted (it may echo course content). This object only
 * shapes the string handed to LlmManager.generate.
 */
object OnDevicePromptTemplate {
    const val HUMAN_TAG = "[|Human|]:"
    const val AI_TAG = "[|AI|]:"

    // Vendor multimodal image markers. The image embedding is supplied separately via callVit; the
    // prompt only carries these placeholder tokens between <im_start> and <im_end>.
    const val IMAGE_START = "<im_start>"
    const val IMAGE_TOKEN = "<image>"
    const val IMAGE_END = "<im_end>"

    /** Wrap a single human turn. The model continues after the trailing [|AI|]: marker. */
    fun format(human: String): String = HUMAN_TAG + human.trim() + "\n" + AI_TAG

    /**
     * Fold an optional system instruction and the user content into one human turn. The on-device
     * template has no dedicated system role, so the system text is prepended to the human turn.
     */
    fun format(system: String?, user: String): String {
        val merged = if (system.isNullOrBlank()) user.trim() else system.trim() + "\n\n" + user.trim()
        return format(merged)
    }

    /**
     * The vendor multimodal prompt shape, used AFTER a successful callVit(rgb, w, h):
     *
     *     [|Human|]:<im_start><image><im_end>用户问题
     *     [|AI|]:
     *
     * The image bytes are NOT in the prompt — they were already encoded by callVit; the template only
     * inserts the image markers ahead of the question.
     */
    fun formatMultimodal(question: String): String =
        HUMAN_TAG + IMAGE_START + IMAGE_TOKEN + IMAGE_END + question.trim() + "\n" + AI_TAG
}
