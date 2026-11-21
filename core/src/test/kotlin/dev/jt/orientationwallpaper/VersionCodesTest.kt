package dev.jt.orientationwallpaper

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCodesTest {
    @Test
    fun derivesVersionCodeFromSemver() {
        assertEquals(1_000_000, VersionCodes.fromSemver("1.0.0"))
        assertEquals(1_000_001, VersionCodes.fromSemver("1.0.1"))
        assertEquals(1_001_000, VersionCodes.fromSemver("1.1.0"))
        assertEquals(2_003_004, VersionCodes.fromSemver("2.3.4"))
        assertEquals(1, VersionCodes.fromSemver("0.0.1"))
    }

    @Test
    fun increasesMonotonicallyAcrossEachComponent() {
        val codes = listOf(
            "1.0.0",
            "1.0.1",
            "1.0.999",
            "1.1.0",
            "1.999.999",
            "2.0.0",
        ).map(VersionCodes::fromSemver)
        assertEquals(codes.distinct().sorted(), codes)
        assertTrue(codes.zipWithNext().all { (previous, next) -> previous < next })
    }

    @Test
    fun rejectsVersionsThatWouldCollideOrOverflow() {
        assertIllegal("1.0")
        assertIllegal("v1.0.0")
        assertIllegal("1.0.0-beta")
        assertIllegal("1.0.1000")
        assertIllegal("1.1000.0")
        assertIllegal("0.0.0")
        assertIllegal("")
    }

    @Test
    fun readsVersionNameFromATag() {
        assertEquals("1.2.3", VersionCodes.versionNameFromTag("v1.2.3"))
        assertEquals("1.2.3", VersionCodes.versionNameFromTag("1.2.3"))
        assertEquals(1_002_003, VersionCodes.fromSemver(VersionCodes.versionNameFromTag("v1.2.3")))
    }

    @Test
    fun releaseBuildUsesTheSameFormula() {
        val script = locateAppBuildScript()
        val text = script.readText()
        assertTrue(
            "app/build.gradle.kts must derive versionCode with the same scales as VersionCodes",
            text.contains("major.toInt() * 1_000_000 + minor.toInt() * 1_000 + patch.toInt()"),
        )
    }

    private fun assertIllegal(version: String) {
        try {
            VersionCodes.fromSemver(version)
            throw AssertionError("expected IllegalArgumentException for $version")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    private fun locateAppBuildScript(): File {
        val candidates = listOf(
            File("app/build.gradle.kts"),
            File("../app/build.gradle.kts"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("app/build.gradle.kts not found from ${File(".").absolutePath}")
    }
}
