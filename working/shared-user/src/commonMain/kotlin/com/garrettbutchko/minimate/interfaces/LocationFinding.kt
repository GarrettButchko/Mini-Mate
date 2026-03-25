package com.garrettbutchko.minimate.interfaces

import com.garrettbutchko.minimate.dataModels.mapModels.CoordinateDTO
import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO
import com.garrettbutchko.minimate.dataModels.MapRegionData
import kotlinx.coroutines.flow.StateFlow

interface LocationFinding {

    val mapItems: StateFlow<List<MapItemDTO>>
    val selectedItem: StateFlow<MapItemDTO?>
    val userLocation: StateFlow<CoordinateDTO?>
    val hasLocationAccess: StateFlow<Boolean>

    fun setMapItems(items: List<MapItemDTO>)
    fun setSelectedItem(item: MapItemDTO?)

    fun requestLocationAccess() {}

    fun getPostalAddress(mapItem: MapItemDTO): String
    fun updateCameraRegion(): MapRegionData?
    fun findClosestMiniGolf(completion: (MapItemDTO?) -> Unit)
    fun searchNearbyCourses(upwardOffset: Double = 0.03,
                            latitudeDelta: Double = 0.1,
                            longitudeDelta: Double = 0.1,
                            completion: (Boolean, MapRegionData?) -> Unit)
}
