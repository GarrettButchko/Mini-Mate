package com.garrettbutchko.minimate.dataModels

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2D

@OptIn(ExperimentalForeignApi::class)
fun LocationCoordinate2D.Companion.fromCValue(cValue: CValue<CLLocationCoordinate2D>): LocationCoordinate2D {
    return cValue.useContents {
        LocationCoordinate2D(
            latitude = latitude,
            longitude = longitude
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
fun LocationCoordinate2D.toCValue(): CValue<CLLocationCoordinate2D> = cValue {
    this.latitude = this@toCValue.latitude
    this.longitude = this@toCValue.longitude
}
