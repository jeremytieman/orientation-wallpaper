import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val requestedVersion = (findProperty("versionName") as String?) ?: "1.0.0"
val appVersionName = requestedVersion.removePrefix("v")
val semver = Regex("""^(\d+)\.(\d+)\.(\d+)$""").matchEntire(appVersionName)
    ?: throw GradleException(
        "versionName must be MAJOR.MINOR.PATCH (optionally prefixed with v), was '$requestedVersion'.",
    )
val (major, minor, patch) = semver.destructured
if (minor.toInt() > 999 || patch.toInt() > 999) {
    throw GradleException("minor and patch must be <= 999 so versionCode stays monotonic.")
}
// Keep this expression in sync with VersionCodes.fromSemver.
val appVersionCode = major.toInt() * 1_000_000 + minor.toInt() * 1_000 + patch.toInt()
if (appVersionCode !in 1..2_100_000_000) {
    throw GradleException("versionCode $appVersionCode is outside 1..2100000000.")
}

val keystoreBase64 = System.getenv("SIGNING_KEYSTORE_BASE64").blankToNull()
val keystorePath = System.getenv("SIGNING_KEYSTORE_PATH").blankToNull()
val keystorePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD").blankToNull()
val keyAlias = System.getenv("SIGNING_KEY_ALIAS").blankToNull()
val keyPassword = System.getenv("SIGNING_KEY_PASSWORD").blankToNull()
val signingRequested = listOf(keystoreBase64, keystorePath, keystorePassword, keyAlias, keyPassword).any { it != null }

val releaseKeystore: File? = when {
    !signingRequested -> null
    keystorePassword == null || keyAlias == null || keyPassword == null ->
        throw GradleException(
            "Signing is partial. Set SIGNING_KEYSTORE_PASSWORD, SIGNING_KEY_ALIAS, and SIGNING_KEY_PASSWORD.",
        )
    keystoreBase64 == null && keystorePath == null ->
        throw GradleException("Set SIGNING_KEYSTORE_BASE64 or SIGNING_KEYSTORE_PATH.")
    keystorePath != null -> File(keystorePath).also { file ->
        if (!file.isFile) throw GradleException("Keystore not found at ${file.absolutePath}")
    }
    else -> {
        val decoded = try {
            Base64.getMimeDecoder().decode(keystoreBase64)
        } catch (invalid: IllegalArgumentException) {
            throw GradleException("SIGNING_KEYSTORE_BASE64 is not valid Base64.", invalid)
        }
        layout.buildDirectory.file("signing/release.keystore").get().asFile.apply {
            parentFile.mkdirs()
            writeBytes(decoded)
        }
    }
}

android {
    namespace = "dev.jt.orientationwallpaper"
    val compileParts = libs.versions.compileSdk.get().split(".")
    compileSdk {
        version = release(compileParts[0].toInt()) {
            minorApiLevel = compileParts.getOrElse(1) { "0" }.toInt()
        }
    }
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "dev.jt.orientationwallpaper"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                // Local builds and CI without the release secrets still produce an installable APK.
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
        // This check calls out to Maven on every lint run and fails as soon as a
        // newer Kotlin patch exists. Kotlin stays on the version AGP 9.5.2 bundles
        // so the Compose compiler and the built-in Kotlin compiler match.
        disable += "NewerVersionAvailable"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

base {
    archivesName.set("OrientationWallpaper-$appVersionName")
}

tasks.register("assertNoGooglePlayServices") {
    group = "verification"
    description = "Fails if the release runtime classpath includes Play services, Firebase, or Play libraries."
    val classpath = configurations.named("releaseRuntimeClasspath")
    doLast {
        val bannedPrefixes = listOf(
            "com.google.android.gms",
            "com.google.firebase",
            "com.google.android.play",
            "com.android.billingclient",
        )
        val banned = classpath.get().resolvedConfiguration.resolvedArtifacts
            .map { it.moduleVersion.id }
            .filter { id -> bannedPrefixes.any { prefix -> id.group.startsWith(prefix) } }
        if (banned.isNotEmpty()) {
            throw GradleException("Proprietary Google libraries are not allowed: $banned")
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.exifinterface)
    implementation(libs.coroutines.android)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
}

fun String?.blankToNull(): String? = this?.takeIf { it.isNotBlank() }
