package com.garrettbutchko.minimate.modelKotlinExtensions

import com.google.android.libraries.places.api.model.Place
import com.garrettbutchko.minimate.datamodels.MapItemDTO
import com.garrettbutchko.minimate.datamodels.CoordinateDTO
import com.garrettbutchko.minimate.datamodels.AddressDTO
import com.garrettbutchko.minimate.datamodels.Course
import com.garrettbutchko.minimate.utilities.CourseIDGenerator
import com.garrettbutchko.minimate.utilities.PasswordGenerator


fun Place.idString(): String {
    // 1. Get latitude and longitude from the latLng object
    val lat = this.location?.latitude ?: 0.0
    val lng = this.location?.longitude ?: 0.0

    // 2. Use 'id' (which is the unique Place ID) instead of resourceName
    val placeId = this.id ?: "unknown"

    return "$lat-$lng-$placeId"
}

fun Place.toDTO(): MapItemDTO {
    return MapItemDTO(
        name = this.resourceName, // Returns String?
        phoneNumber = this.nationalPhoneNumber, // Returns String?
        url = this.websiteUri?.toString(), // Converts Android Uri? to String?
        address = AddressDTO(
            fullAddress = this.formattedAddress ?: "", // Handle null with empty string
            shortAddress = this.shortFormattedAddress // Place object doesn't have a direct "short" address
        ),
        coordinate = CoordinateDTO(
            latitude = this.location?.latitude ?: 0.0,
            longitude = this.location?.longitude ?: 0.0
        )
    )
}

fun Place.toCourse(isSupported: Boolean): Course {
    return Course(
        id = CourseIDGenerator.generateCourseID(this.toDTO()),
        name = this.resourceName ?: "",
        password = PasswordGenerator.generate(PasswordGenerator.Style.Strong()),
        latitude = this.location?.latitude ?: 0.0,
        longitude = this.location?.longitude ?: 0.0,
        isSupported = isSupported
    )
}
