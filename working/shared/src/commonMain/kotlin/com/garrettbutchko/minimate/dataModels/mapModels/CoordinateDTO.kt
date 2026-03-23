package com.garrettbutchko.minimate.dataModels.mapModels

import kotlinx.serialization.Serializable

@Serializable
data class CoordinateDTO(
    val latitude: Double,
    val longitude: Double
)
