import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// `core` is a PURE Kotlin/JVM library on purpose:
//  - It holds the domain (models, providers, prompt, parser, validation, review).
//  - It has NO Android dependency, so it builds fast and is unit-tested with plain
//    JUnit via the `:core:test` task (see README / verification commands).
//  - Networking is abstracted behind HttpTransport so this module pulls in no HTTP
//    client; real transports are injected from the app/data layer.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
