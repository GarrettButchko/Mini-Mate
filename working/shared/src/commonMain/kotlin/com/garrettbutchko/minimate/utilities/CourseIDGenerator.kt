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


}
