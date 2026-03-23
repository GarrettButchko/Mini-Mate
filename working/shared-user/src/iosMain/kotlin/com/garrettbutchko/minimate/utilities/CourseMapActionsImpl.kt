package com.garrettbutchko.minimate.utilities

import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO
import com.garrettbutchko.minimate.dataModels.toMKMapItem
import com.garrettbutchko.minimate.viewModels.CourseMapActions
import platform.MapKit.MKLaunchOptionsDirectionsModeDriving
import platform.MapKit.MKLaunchOptionsDirectionsModeKey

class CourseMapActionsImpl : CourseMapActions {
    override fun getDirections(mapItem: MapItemDTO) {
        val mkMapItem = mapItem.toMKMapItem()
        val launchOptions = mapOf<Any?, Any>(
            MKLaunchOptionsDirectionsModeKey to MKLaunchOptionsDirectionsModeDriving
        )
        mkMapItem.openInMapsWithLaunchOptions(launchOptions)
    }
}
