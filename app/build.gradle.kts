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
// These names must not match SigningConfig properties. Inside signingConfigs { },
// an unqualified `keyAlias` or `keyPassword` is the config property (still null),
// so `keyAlias ?: "missing"` would store the literal placeholder and the real
// SIGNING_KEY_ALIAS would never be used.
val signingKeyAlias = System.getenv("SIGNING_KEY_ALIAS").blankToNull()
val signingKeyPassword = System.getenv("SIGNING_KEY_PASSWORD").blankToNull()
val decodedKeystore = layout.buildDirectory.file("signing/release.keystore")

// The keystore file is written only when a release packaging task runs.
val prepareReleaseKeystore = tasks.register("prepareReleaseKeystore") {
    group = "build"
    description = "Checks release signing secrets and writes the keystore if it was provided as Base64."
    doLast {
        val missing = buildList {
            if (keystorePassword == null) add("SIGNING_KEYSTORE_PASSWORD")
            if (signingKeyAlias == null) add("SIGNING_KEY_ALIAS")
            if (signingKeyPassword == null) add("SIGNING_KEY_PASSWORD")
            if (keystoreBase64 == null && keystorePath == null) {
                add("SIGNING_KEYSTORE_BASE64 or SIGNING_KEYSTORE_PATH")
            }
        }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Refusing to sign a release APK without the release keystore. " +
                    "Missing: ${missing.joinToString(", ")}. " +
                    "Debug signing is not used for release builds.",
            )
        }
        if (keystorePath != null) {
            val file = File(keystorePath)
            if (!file.isFile) throw GradleException("Keystore not found at ${file.absolutePath}")
            return@doLast
        }
        val decoded = try {
            Base64.getMimeDecoder().decode(keystoreBase64)
        } catch (invalid: IllegalArgumentException) {
            throw GradleException("SIGNING_KEYSTORE_BASE64 is not valid Base64.", invalid)
        }
        val out = decodedKeystore.get().asFile
        out.parentFile.mkdirs()
        out.writeBytes(decoded)
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
        create("release") {
            storeType = "PKCS12"
        }
    }
    // Assigned outside the SigningConfig receiver so the env values are not
    // shadowed by storePassword / keyAlias / keyPassword on that receiver.
    signingConfigs.getByName("release").apply {
        storeFile = if (keystorePath != null) File(keystorePath) else decodedKeystore.get().asFile
        storePassword = keystorePassword
        keyAlias = signingKeyAlias
        keyPassword = signingKeyPassword
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
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

tasks.configureEach {
    if (
        name == "validateSigningRelease" ||
        name == "packageRelease" ||
        name == "signRelease" ||
        name == "bundleRelease" ||
        name == "packageReleaseBundle" ||
        name == "signReleaseBundle"
    ) {
        dependsOn(prepareReleaseKeystore)
    }
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
