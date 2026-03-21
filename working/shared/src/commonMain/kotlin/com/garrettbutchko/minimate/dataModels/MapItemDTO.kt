package com.garrettbutchko.minimate.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class MapItemDTO(
    val placeID: String? = null,
    val name: String?,
    val phoneNumber: String?,
    val url: String?,
    val address: AddressDTO?,
    val coordinate: CoordinateDTO
){
}

@Serializable
data class AddressDTO(
    val fullAddress: String,
    val shortAddress: String?,
)

@Serializable
data class CoordinateDTO(
    val latitude: Double,
    val longitude : Double
)