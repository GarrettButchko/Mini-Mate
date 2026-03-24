package com.garrettbutchko.minimate.dataModels

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2D
import com.garrettbutchko.minimate.dataModels.mapModels.CoordinateDTO


@OptIn(ExperimentalForeignApi::class)
fun CLLocation.toDTO(): CoordinateDTO {
    return CoordinateDTO(
        latitude = this.coordinate.useContents { latitude },
        longitude = this.coordinate.useContents { longitude }
    )
}

@OptIn(ExperimentalForeignApi::class)
fun CoordinateDTO.toCValue(): CValue<CLLocationCoordinate2D> = cValue {
    this.latitude = this@toCValue.latitude
    this.longitude = this@toCValue.longitude
}

@OptIn(ExperimentalForeignApi::class)
fun CoordinateDTO.toCLLocation(): CLLocation {
    return CLLocation(latitude = this.latitude, longitude = this.longitude)
}
