package com.garrettbutchko.minimate.dataModels

data class MapRegionData(
    val latitude: Double,
    val longitude: Double,
    val latitudeDelta: Double,
    val longitudeDelta: Double
) {
    companion object
}
