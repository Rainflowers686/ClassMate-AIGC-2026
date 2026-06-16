package com.classmate.app.ui.flow

import androidx.annotation.RawRes
import com.classmate.app.R

data class AmbientSound(
    val id: String,
    val displayName: String,
    val sceneName: String,
    val description: String,
    val fileName: String,
    @RawRes val rawResId: Int,
    val sourceSite: String,
    val originalUrl: String,
    val licenseName: String,
    val licenseUrl: String,
    val attributionRequired: Boolean,
    val commercialUseAllowed: Boolean,
)

object AmbientSoundCatalog {
    const val LICENSE_URL = "https://mixkit.co/license/"
    const val LICENSE_NAME = "Mixkit Sound Effects Free License"
    const val SOURCE_SITE = "Mixkit"

    val all: List<AmbientSound> = listOf(
        AmbientSound(
            id = "rain",
            displayName = "轻雨声",
            sceneName = "窗边细雨",
            description = "轻柔雨声，适合整理笔记和低压力复习。",
            fileName = "flow_rain_loop.mp3",
            rawResId = R.raw.flow_rain_loop,
            sourceSite = SOURCE_SITE,
            originalUrl = "https://assets.mixkit.co/active_storage/sfx/2393/2393-preview.mp3",
            licenseName = LICENSE_NAME,
            licenseUrl = LICENSE_URL,
            attributionRequired = false,
            commercialUseAllowed = true,
        ),
        AmbientSound(
            id = "forest",
            displayName = "森林鸟鸣",
            sceneName = "森林晨读",
            description = "鸟鸣和森林氛围，适合晨间记忆和轻阅读。",
            fileName = "flow_forest_loop.mp3",
            rawResId = R.raw.flow_forest_loop,
            sourceSite = SOURCE_SITE,
            originalUrl = "https://assets.mixkit.co/active_storage/sfx/1212/1212-preview.mp3",
            licenseName = LICENSE_NAME,
            licenseUrl = LICENSE_URL,
            attributionRequired = false,
            commercialUseAllowed = true,
        ),
        AmbientSound(
            id = "ocean",
            displayName = "海浪",
            sceneName = "海边复盘",
            description = "平稳海浪循环，适合长时间回看知识地图。",
            fileName = "flow_ocean_loop.mp3",
            rawResId = R.raw.flow_ocean_loop,
            sourceSite = SOURCE_SITE,
            originalUrl = "https://assets.mixkit.co/active_storage/sfx/1196/1196-preview.mp3",
            licenseName = LICENSE_NAME,
            licenseUrl = LICENSE_URL,
            attributionRequired = false,
            commercialUseAllowed = true,
        ),
        AmbientSound(
            id = "stream",
            displayName = "溪流",
            sceneName = "溪边专注",
            description = "水流环境音，适合做题和错题复盘。",
            fileName = "flow_stream_loop.mp3",
            rawResId = R.raw.flow_stream_loop,
            sourceSite = SOURCE_SITE,
            originalUrl = "https://assets.mixkit.co/active_storage/sfx/3126/3126-preview.mp3",
            licenseName = LICENSE_NAME,
            licenseUrl = LICENSE_URL,
            attributionRequired = false,
            commercialUseAllowed = true,
        ),
        AmbientSound(
            id = "cafe",
            displayName = "咖啡馆环境",
            sceneName = "清晨咖啡馆",
            description = "低声环境氛围，适合轻量整理和导出前预览。",
            fileName = "flow_cafe_ambience_loop.mp3",
            rawResId = R.raw.flow_cafe_ambience_loop,
            sourceSite = SOURCE_SITE,
            originalUrl = "https://assets.mixkit.co/active_storage/sfx/444/444-preview.mp3",
            licenseName = LICENSE_NAME,
            licenseUrl = LICENSE_URL,
            attributionRequired = false,
            commercialUseAllowed = true,
        ),
        AmbientSound(
            id = "night_crickets",
            displayName = "夜间虫鸣",
            sceneName = "夜间书桌",
            description = "夜间虫鸣循环，适合睡前回顾课程精华脚本。",
            fileName = "flow_night_crickets_loop.mp3",
            rawResId = R.raw.flow_night_crickets_loop,
            sourceSite = SOURCE_SITE,
            originalUrl = "https://assets.mixkit.co/active_storage/sfx/1782/1782-preview.mp3",
            licenseName = LICENSE_NAME,
            licenseUrl = LICENSE_URL,
            attributionRequired = false,
            commercialUseAllowed = true,
        ),
    )

    fun byId(id: String): AmbientSound? = all.firstOrNull { it.id == id }
}
