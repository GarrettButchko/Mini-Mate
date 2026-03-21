package com.garrettbutchko.minimate.dataModels

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.MapKit.MKCoordinateRegion

@OptIn(ExperimentalForeignApi::class)
fun MapRegionData.Companion.fromCValue(cValue: CValue<MKCoordinateRegion>): MapRegionData {
    return cValue.useContents {
        MapRegionData(
            latitude = center.latitude,
            longitude = center.longitude,
            latitudeDelta = span.latitudeDelta,
            longitudeDelta = span.longitudeDelta
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
fun MapRegionData.toCValue(): CValue<MKCoordinateRegion> = cValue {
    this.center.latitude = latitude
    this.center.longitude = longitude
    this.span.latitudeDelta = latitudeDelta
    this.span.longitudeDelta = longitudeDelta
}
