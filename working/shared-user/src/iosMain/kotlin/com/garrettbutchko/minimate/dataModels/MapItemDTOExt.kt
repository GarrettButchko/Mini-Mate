package com.garrettbutchko.minimate.dataModels

import kotlinx.cinterop.ExperimentalForeignApi
import platform.MapKit.MKMapItem
import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO

@OptIn(ExperimentalForeignApi::class)
fun MapItemDTO.toMKMapItem(): MKMapItem {
    return MKMapItem(location = this.coordinate.toCLLocation(), address = this.address?.toMKAddress())
}
