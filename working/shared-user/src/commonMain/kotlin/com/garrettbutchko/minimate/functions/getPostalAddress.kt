package com.garrettbutchko.minimate.functions

import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO

fun getAddress(mapItem: MapItemDTO): String {
    return  mapItem.address?.shortAddress ?: mapItem.address?.fullAddress ?: "No Address"
}