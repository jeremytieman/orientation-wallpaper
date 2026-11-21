// Keep this Kotlin version aligned with [versions].kotlin in gradle/libs.versions.toml.
// AGP's built-in Kotlin uses whatever Kotlin Gradle plugin is on the buildscript
// classpath when it is newer than the copy AGP bundles.
buildscript {
    val kotlinVersion = "2.2.10"
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
}
