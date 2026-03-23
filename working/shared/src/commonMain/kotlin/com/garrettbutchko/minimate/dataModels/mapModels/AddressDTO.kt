package com.garrettbutchko.minimate.dataModels.mapModels

import kotlinx.serialization.Serializable

@Serializable
data class AddressDTO(
    val fullAddress: String,
    val shortAddress: String?,
)
