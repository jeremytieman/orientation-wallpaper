package dev.jt.orientationwallpaper

/**
 * Play-installable versionCode derived from MAJOR.MINOR.PATCH.
 *
 * `versionCode = major * 1_000_000 + minor * 1_000 + patch`.
 * Minor and patch stay below 1000 so each component has room to grow without
 * colliding with the next one: 1.0.999 is still less than 1.1.0, and 1.999.999
 * is still less than 2.0.0. The same expression is used in `app/build.gradle.kts`.
 */
object VersionCodes {
    const val MAJOR_SCALE = 1_000_000
    const val MINOR_SCALE = 1_000
    const val MAX_COMPONENT = 999
    const val MAX_VERSION_CODE = 2_100_000_000

    fun fromSemver(versionName: String): Int {
        val match = SEMVER.matchEntire(versionName)
            ?: throw IllegalArgumentException(
                "versionName must be MAJOR.MINOR.PATCH, was '$versionName'",
            )
        val major = match.groupValues[1].toInt()
        val minor = match.groupValues[2].toInt()
        val patch = match.groupValues[3].toInt()
        if (minor > MAX_COMPONENT || patch > MAX_COMPONENT) {
            throw IllegalArgumentException("minor and patch must be <= $MAX_COMPONENT")
        }
        val code = try {
            Math.addExact(
                Math.addExact(
                    Math.multiplyExact(major, MAJOR_SCALE),
                    Math.multiplyExact(minor, MINOR_SCALE),
                ),
                patch,
            )
        } catch (overflow: ArithmeticException) {
            throw IllegalArgumentException("versionCode overflow for $versionName", overflow)
        }
        if (code !in 1..MAX_VERSION_CODE) {
            throw IllegalArgumentException("versionCode $code is outside 1..$MAX_VERSION_CODE")
        }
        return code
    }

    /** Accepts `v1.2.3` or `1.2.3` and returns the versionName. */
    fun versionNameFromTag(tag: String): String {
        val name = if (tag.startsWith("v")) tag.removePrefix("v") else tag
        fromSemver(name)
        return name
    }

    private val SEMVER = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
}
