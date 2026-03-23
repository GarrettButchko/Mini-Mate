package com.garrettbutchko.minimate.dataModels.mapModels

import kotlinx.serialization.Serializable

@Serializable
data class MapItemDTO(
    val placeID: String? = null,
    val name: String?,
    val phoneNumber: String?,
    val url: String?,
    val address: AddressDTO?,
    val coordinate: CoordinateDTO
)
