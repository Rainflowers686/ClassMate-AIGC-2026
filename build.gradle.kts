// Root build file. Plugins are declared here with `apply false` so that
// version alignment lives in a single place (the version catalog), and each
// module opts in to the plugins it needs.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
