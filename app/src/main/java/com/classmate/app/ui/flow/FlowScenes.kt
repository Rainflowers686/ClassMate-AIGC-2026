package com.classmate.app.ui.flow

/**
 * A Flow ambience scene. These scenes now have bundled, licensed local loops.
 * They never record audio, never stream from a third-party service, and never synthesize background
 * sound at runtime.
 */
data class FlowScene(
    val id: String,
    val name: String,
    val description: String,
)

object FlowScenes {
    val all: List<FlowScene> = AmbientSoundCatalog.all.map { sound ->
        FlowScene(
            id = sound.id,
            name = sound.displayName,
            description = sound.description,
        )
    }

    const val DISCLAIMER: String =
        "背景音来自内置授权循环素材；仅本地播放，不录音、不上传、不使用实时生成。"

    val mixerChannels: List<String> = listOf("背景音", "专注计时", "音量")

    fun byId(id: String): FlowScene? = all.firstOrNull { it.id == id }
}
