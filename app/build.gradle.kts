import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

// BuildInfo values, computed at configuration time. Git is best-effort and NEVER fails the build
// (CI-safe): if git is missing or errors, the commit is "unknown".
val builtAtValue: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
val gitCommitValue: String = try {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
    if (process.waitFor() == 0 && output.isNotBlank()) output else "unknown"
} catch (e: Exception) {
    "unknown"
}

// Optional on-device SDK: app/libs/llm-sdk-release.aar is a LOCAL binary that stays gitignored and
// is never committed. When present, it is linked in and only its arm64-v8a native libraries ship.
// When absent (CI / fresh machines), the build still succeeds and the app falls back to the honest
// missing-SDK seam — never a build failure.
val onDeviceSdkAar = file("libs/llm-sdk-release.aar")
val hasOnDeviceSdk = onDeviceSdkAar.exists()

android {
    namespace = "com.classmate.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.classmate.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 107
        versionName = "1.12.5"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "BUILT_AT", "\"$builtAtValue\"")
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommitValue\"")
        // The on-device SDK ships ONLY arm64-v8a native libraries; restrict packaged ABIs to match
        // (avoids shipping a half-supported ABI). Applied only when the AAR is actually present.
        if (hasOnDeviceSdk) {
            ndk { abiFilters += "arm64-v8a" }
        }
    }

    buildTypes {
        debug {
            // Debug-only config import entry (see SettingsScreen / DebugConfigImporter) is
            // gated on BuildConfig.DEBUG.
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))

    // Local-only on-device BlueLM SDK (gitignored). Linked when present; the bridge uses reflection
    // so app/core compile even without it. Native libs come from the AAR — no manual .so copying.
    if (hasOnDeviceSdk) {
        implementation(files("libs/llm-sdk-release.aar"))
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Runtime JSON parsing for the debug-only config import preview (no codegen needed).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
