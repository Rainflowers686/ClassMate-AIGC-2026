package com.classmate.app

/** Safe, non-sensitive build metadata surfaced in Settings. */
object BuildInfo {
    val buildType: String = BuildConfig.BUILD_TYPE
    val builtAt: String = BuildConfig.BUILT_AT
    val gitCommitShort: String = BuildConfig.GIT_COMMIT
    val versionName: String = BuildConfig.VERSION_NAME
    val versionCode: Int = BuildConfig.VERSION_CODE
}
