package com.garrettbutchko.minimate.functions

import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO

fun getAddress(mapItem: MapItemDTO): String {
    return mapItem.address?.fullAddress ?: mapItem.address?.shortAddress?: ""
}