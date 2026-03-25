package com.garrettbutchko.minimate.dataModels

import kotlin.random.Random

data class MapRegionData(
    val latitude: Double,
    val longitude: Double,
    val latitudeDelta: Double,
    val longitudeDelta: Double,
    val updateKey: String
) {
    constructor(
        latitude: Double,
        longitude: Double,
        latitudeDelta: Double,
        longitudeDelta: Double
    ) : this(latitude, longitude, latitudeDelta, longitudeDelta, Random.nextLong().toString())

    companion object
}
