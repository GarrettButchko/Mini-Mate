package com.garrettbutchko.minimate.logic

import kotlin.math.round

/**
 * A platform-agnostic class designed to be easily migrated to a Kotlin Multiplatform shared module.
 * It contains pure business, calculation, and formatting rules for search results.
 */
class SearchResultBusinessLogic {
    
    // MARK: - Configuration Rules
    
    /**
     * The amount of time to wait before showing the retry button.
     */
    val retryButtonDelay: Double = 3.0
    
    // MARK: - Location Calculations
    
    /**
     * Calculates an adjusted latitude for centering/distance calculations.
     */
    fun getOffsetLatitude(baseLatitude: Double): Double {
        return baseLatitude - 0.015
    }
    
    /**
     * Converts distance in meters to miles.
     */
    fun metersToMiles(meters: Double): Double {
        return meters / 1609.34
    }
    
    // MARK: - Presentation Formatting
    
    /**
     * Formats the distance to a single decimal point.
     */
    fun formatDistance(distanceInMiles: Double): String {
        val rounded = round(distanceInMiles * 10) / 10.0
        val str = rounded.toString()
        return if (!str.contains(".")) "$str.0" else str
    }
    
    /**
     * Constructs the final subtitle string combining distance and address.
     */
    fun buildSubtitle(distanceInMiles: Double, address: String): String {
        val formattedDistance = formatDistance(distanceInMiles)
        return "$formattedDistance mi - $address"
    }
}
