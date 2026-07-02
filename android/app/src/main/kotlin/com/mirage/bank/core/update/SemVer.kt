package com.mirage.bank.core.update

/** Minimal semver comparator -- major.minor.patch only, no prerelease handling needed for this app. */
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        return patch - other.patch
    }

    companion object {
        fun parse(raw: String): SemVer? {
            val cleaned = raw.trim().removePrefix("android-v").removePrefix("v")
            val parts = cleaned.split(".", "-").take(3)
            if (parts.size < 3) return null
            val (maj, min, patch) = parts
            return SemVer(
                maj.toIntOrNull() ?: return null,
                min.toIntOrNull() ?: return null,
                patch.toIntOrNull() ?: return null,
            )
        }
    }
}
