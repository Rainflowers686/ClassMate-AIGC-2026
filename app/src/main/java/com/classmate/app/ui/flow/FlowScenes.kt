package com.classmate.app.ui.flow

/**
 * A Flow ambience scene. These are VISUAL placeholders only: ClassMate ships no bundled audio and
 * does not record or play any sound. Selecting a scene changes the on-screen mood, nothing else.
 * Keeping this as pure data (no Android types) lets the honesty contract be unit-tested.
 */
data class FlowScene(
    val id: String,
    val name: String,
    val description: String,
)

object FlowScenes {
    /** Shown on the Flow / Live Companion ambience strip. Honest about doing no audio playback. */
    val all: List<FlowScene> = listOf(
        FlowScene("rain_meadow", "雨后草地", "Soft greens after the rain — calm, open, unhurried."),
        FlowScene("window_rain", "窗边下雨", "Raindrops on the window while you study indoors."),
        FlowScene("night_desk", "夜间书桌", "A quiet desk lamp at night for deep focus."),
        FlowScene("white_noise", "纯白噪音", "A neutral, even backdrop with no distractions."),
        FlowScene("morning_cafe", "清晨咖啡馆", "Gentle early-morning café light and warmth."),
    )

    /**
     * The exact honesty line shown next to the ambience strip and the mixer placeholder. It must
     * make clear that no real audio is bundled or playing — this is a visual mood only.
     */
    const val DISCLAIMER: String =
        "场景与混音为视觉占位，未接入真实音频资源，当前不会录音、也不会播放任何声音。"

    /** Names for the (placeholder) sound mixer sliders — UI only, wired to nothing. */
    val mixerChannels: List<String> = listOf("雨声", "远雷", "风声", "白噪音")

    fun byId(id: String): FlowScene? = all.firstOrNull { it.id == id }
}
