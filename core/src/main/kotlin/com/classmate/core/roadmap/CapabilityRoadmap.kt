package com.classmate.core.roadmap

enum class CapabilityStage {
    CONNECTED,
    PLANNED,
    DEMO_ENHANCEMENT,
}

data class CapabilityCard(
    val title: String,
    val stage: CapabilityStage,
    val description: String,
)

object CapabilityRoadmap {
    val cards: List<CapabilityCard> = listOf(
        CapabilityCard("BlueLM text generation / qwen3.5-plus", CapabilityStage.CONNECTED, "Connected through the official vivo AIGC text model path."),
        CapabilityCard("General OCR", CapabilityStage.PLANNED, "Planned seam: slides, board photos, or textbook pages to text, then existing analysis."),
        CapabilityCard("Long speech transcription", CapabilityStage.PLANNED, "Planned seam: classroom audio to transcript, then existing analysis. Not implemented yet."),
        CapabilityCard("Realtime short ASR", CapabilityStage.PLANNED, "Planned seam for Live Companion realtime transcript. Not implemented yet."),
        CapabilityCard("Text vectors / similarity", CapabilityStage.PLANNED, "Planned for knowledge de-duplication, similar lessons, and review aggregation."),
        CapabilityCard("Text moderation", CapabilityStage.PLANNED, "Planned safety check before export or share."),
        CapabilityCard("On-device 3B model", CapabilityStage.PLANNED, "Planned offline fallback and privacy mode."),
        CapabilityCard("On-device text moderation", CapabilityStage.PLANNED, "Planned local safety filtering."),
        CapabilityCard("Compatible demo mode", CapabilityStage.DEMO_ENHANCEMENT, "OpenAI-compatible demo enhancement only; not the official compliance path."),
    )

    fun grouped(): Map<CapabilityStage, List<CapabilityCard>> = cards.groupBy { it.stage }
}
