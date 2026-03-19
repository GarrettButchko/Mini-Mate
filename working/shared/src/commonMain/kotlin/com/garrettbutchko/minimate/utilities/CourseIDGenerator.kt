package com.garrettbutchko.minimate.utilities

import com.garrettbutchko.minimate.datamodels.MapItemDTO

object CourseIDGenerator {

    /**
     * Public API to generate a course ID.
     */
    fun generateCourseID(item: MapItemDTO): String {
        val namePart = slugify(item.name ?: "unknown")

        val hashInput = "${item.coordinate.latitude}-${item.coordinate.longitude}-${item.name ?: ""}"
        val hash = shortHash(hashInput)

        return "$namePart-$hash"
    }

    /**
     * Transforms a string into a URL-friendly slug.
     */
    private fun slugify(text: String): String {
        return text.lowercase()
            .trim()
            // Replace non-alphanumeric characters with hyphens
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() }
            .joinToString("-")
    }

    /**
     * Generates a short SHA-256 hash.
     * Note: For pure KMP (iOS/Android), you'd typically use a library like
     * 'Kotlin Crypto' or expect/actual for Platform-specific SHA256.
     */
    private fun shortHash(text: String): String {
        // Use a simple hash for commonMain to avoid JVM-specific MessageDigest
        val hash = text.fold(0L) { acc, c -> acc * 31 + c.code.toLong() }
        return hash.toString(16).lowercase().take(8)
    }
}