package com.garrettbutchko.minimate.extensions

import com.garrettbutchko.minimate.dataModels.AddressDTO
import com.garrettbutchko.minimate.dataModels.CoordinateDTO
import com.garrettbutchko.minimate.dataModels.MapItemDTO
import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.utilities.CourseIDGenerator
import com.garrettbutchko.minimate.utilities.PasswordGenerator
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.MapKit.MKMapItem
import platform.UIKit.UIDevice

@OptIn(ExperimentalForeignApi::class)
val MKMapItem.idString: String
    get() {
        val version = UIDevice.currentDevice.systemVersion
        val majorVersion = version.split(".").firstOrNull()?.toIntOrNull() ?: 0

        val lat: Double
        val lon: Double
        if (majorVersion >= 26) {
            lat = this.location.coordinate.useContents { latitude }
            lon = this.location.coordinate.useContents { longitude }
        } else {
            lat = this.placemark.coordinate.useContents { latitude }
            lon = this.placemark.coordinate.useContents { longitude }
        }
        return "$lat-$lon-${this.name ?: ""}"
    }

@OptIn(ExperimentalForeignApi::class)
val MKMapItem.newAddress: AddressDTO?
    get() {
        val version = UIDevice.currentDevice.systemVersion
        val majorVersion = version.split(".").firstOrNull()?.toIntOrNull() ?: 0

        if (majorVersion >= 26) {
            val address = this.address
            if (address != null) {
                return AddressDTO(
                    fullAddress = address.fullAddress,
                    shortAddress = address.shortAddress
                )
            }
        } else {
            val placemark = this.placemark
            
            val sub = placemark.subThoroughfare
            val thoroughfare = placemark.thoroughfare
            val streetParts = listOfNotNull(sub, thoroughfare)
            val street = if (streetParts.isNotEmpty()) streetParts.joinToString(" ") else "Unknown Street"
            
            val city = placemark.locality ?: "Unknown City"
            val state = placemark.administrativeArea ?: "Unknown State"
            val postalCode = placemark.postalCode ?: ""
            val country = placemark.country ?: "Unknown Country"

            val longAddress = """
                $street
                $city, $state $postalCode
                $country
            """.trimIndent()

            val shortAddress = "$street, $city"

            return AddressDTO(
                fullAddress = longAddress,
                shortAddress = shortAddress
            )
        }
        return null
    }

@OptIn(ExperimentalForeignApi::class)
fun MKMapItem.toDTO(): MapItemDTO {
    val version = UIDevice.currentDevice.systemVersion
    val majorVersion = version.split(".").firstOrNull()?.toIntOrNull() ?: 0

    val lat: Double
    val lon: Double
    if (majorVersion >= 26) {
        lat = this.location.coordinate.useContents { latitude }
        lon = this.location.coordinate.useContents { longitude }
    } else {
        lat = this.placemark.coordinate.useContents { latitude }
        lon = this.placemark.coordinate.useContents { longitude }
    }

    return MapItemDTO(
        name = this.name,
        phoneNumber = this.phoneNumber,
        url = this.url?.absoluteString,
        address = this.newAddress,
        coordinate = CoordinateDTO(
            latitude = lat,
            longitude = lon
        )
    )
}

@OptIn(ExperimentalForeignApi::class)
fun MKMapItem.toCourse(isSupported: Boolean = false): Course {
    val dto = this.toDTO()
    val version = UIDevice.currentDevice.systemVersion
    val majorVersion = version.split(".").firstOrNull()?.toIntOrNull() ?: 0

    val lat: Double
    val lon: Double
    if (majorVersion >= 26) {
        lat = this.location.coordinate.useContents { latitude }
        lon = this.location.coordinate.useContents { longitude }
    } else {
        lat = this.placemark.coordinate.useContents { latitude }
        lon = this.placemark.coordinate.useContents { longitude }
    }

    return Course.create(
        id = CourseIDGenerator.generateCourseID(dto),
        name = this.name ?: "",
        password = PasswordGenerator.generateStrong(length = 20, useSymbols = true),
        latitude = lat,
        longitude = lon,
        isSupported = isSupported
    )
}
