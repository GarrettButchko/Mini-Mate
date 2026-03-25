package com.garrettbutchko.minimate.dataModels

import kotlinx.cinterop.ExperimentalForeignApi
import platform.MapKit.MKMapItem
import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO
import platform.Foundation.NSURL
import platform.UIKit.UIDevice

@OptIn(ExperimentalForeignApi::class)
fun MapItemDTO.toMKMapItem(): MKMapItem {
    val version = UIDevice.currentDevice.systemVersion
    val majorVersion = version.split(".").firstOrNull()?.toIntOrNull() ?: 0

    val mapItem: MKMapItem
    if (majorVersion >= 18) {
        // Use iOS 18+ constructor if available in the bindings
        // Note: MKAddress is also iOS 18+
        mapItem = MKMapItem(location = this.coordinate.toCLLocation(), address = this.address?.toMKAddress())
    } else {
        // Fallback for older iOS versions
        mapItem = MKMapItem(placemark = platform.MapKit.MKPlacemark(coordinate = this.coordinate.toCValue(), addressDictionary = null))
    }
    
    mapItem.name = this.name
    mapItem.phoneNumber = this.phoneNumber
    mapItem.url = this.url?.let { NSURL.URLWithString(it) }
    
    return mapItem
}
