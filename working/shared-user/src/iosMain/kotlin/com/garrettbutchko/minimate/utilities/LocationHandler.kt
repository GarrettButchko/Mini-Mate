package com.garrettbutchko.minimate.utilities

import com.garrettbutchko.minimate.dataModels.MapItemDTO
import com.garrettbutchko.minimate.extensions.toDTO
import com.garrettbutchko.minimate.dataModels.LocationCoordinate2D
import com.garrettbutchko.minimate.dataModels.MapRegionData
import com.garrettbutchko.minimate.dataModels.fromCValue
import com.garrettbutchko.minimate.dataModels.toCValue
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreLocation.*
import platform.Foundation.*
import platform.MapKit.*
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen


@OptIn(ExperimentalForeignApi::class)
class LocationHandler {
    private var currentSearch: MKLocalSearch? = null
    private val _mapItems = MutableStateFlow<List<MKMapItem>>(emptyList())
    val mapItems: StateFlow<List<MKMapItem>> = _mapItems.asStateFlow()

    private val _selectedItem = MutableStateFlow<MKMapItem?>(null)
    val selectedItem: StateFlow<MKMapItem?> = _selectedItem.asStateFlow()

    private val _userLocation = MutableStateFlow<CLLocation?>(null)
    val userLocation: StateFlow<CLLocation?> = _userLocation.asStateFlow()

    private val _hasLocationAccess = MutableStateFlow(false)
    val hasLocationAccess: StateFlow<Boolean> = _hasLocationAccess.asStateFlow()

    fun setMapItems(items: List<MKMapItem>) {
        _mapItems.value = items
    }

    fun setSelectedItem(item: MKMapItem?) {
        _selectedItem.value = item
    }

    fun setUserLocation(location: CLLocation?) {
        _userLocation.value = location
    }

    fun setHasLocationAccess(hasAccess: Boolean) {
        _hasLocationAccess.value = hasAccess
    }

    val version = UIDevice.currentDevice.systemVersion
    val majorVersion = version.split(".").firstOrNull()?.toIntOrNull() ?: 0

    init {
        val status = CLLocationManager.authorizationStatus()
        _hasLocationAccess.value = (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways)
    }

    fun performSearch(
        region: CValue<MKCoordinateRegion>,
        completion: (Boolean) -> Unit
    ) {
        currentSearch?.cancel()

        val request = MKLocalSearchRequest()
        request.naturalLanguageQuery = "mini golf"
        request.region = region
        request.pointOfInterestFilter = MKPointOfInterestFilter(includingCategories = listOf("MKPOICategoryMiniGolf"))

        val search = MKLocalSearch(request = request)
        currentSearch = search

        search.startWithCompletionHandler { response, error ->
            if (error != null) {
                println("Error during search: ${error.localizedDescription}")
                completion(false)
                return@startWithCompletionHandler
            }

            val items = response?.mapItems
            if (items == null) {
                println("No response or no mapItems.")
                completion(false)
                return@startWithCompletionHandler
            }

            val userLoc = _userLocation.value
            val sorted = if (userLoc != null) {
                items.mapNotNull { it as? MKMapItem }.sortedBy { mapItem ->

                    val mapLoc: CLLocation = if (majorVersion >= 26) {
                        CLLocation(
                            latitude = mapItem.location.coordinate.useContents { latitude },
                            longitude = mapItem.location.coordinate.useContents { longitude }
                        )
                    } else {
                        CLLocation(
                            latitude = mapItem.placemark.coordinate.useContents { latitude },
                            longitude = mapItem.placemark.coordinate.useContents { longitude }
                        )
                    }

                    mapLoc.distanceFromLocation(userLoc)
                }
            } else {
                items.mapNotNull { it as? MKMapItem }
            }

            _mapItems.value = sorted
            completion(true)
            currentSearch = null
        }
    }

    // NOTE: Returns `MKCoordinateRegion?` instead of SwiftUI's `MapCameraPosition`. 
    // You can wrap the returned region in `MapCameraPosition.region(...)` from the Swift side.
    fun searchNearbyCourses(
        upwardOffset: Double = 0.03,
        latitudeDelta: Double = 0.1,
        longitudeDelta: Double = 0.1,
        completion: (Boolean, MapRegionData?) -> Unit
    ) {
        val networkChecker = NetworkChecker.shared
        if (!networkChecker.isConnected) {
            completion(false, null)
            return
        }
        
        val userLoc = _userLocation.value
        if (userLoc == null) {
            completion(false, null)
            return
        }

        val region = cValue<MKCoordinateRegion> {
            this.center.latitude = userLoc.coordinate.useContents { latitude } + upwardOffset
            this.center.longitude = userLoc.coordinate.useContents { longitude }
            this.span.latitudeDelta = latitudeDelta
            this.span.longitudeDelta = longitudeDelta
        }

        performSearch(region) { success ->
            val newPosition = if (success) updateCameraRegion(null) else null
            completion(success, newPosition)
        }
    }

    fun findClosestMiniGolf(completion: (MapItemDTO?) -> Unit) {
        val userLoc = _userLocation.value
        if (userLoc == null) {
            completion(null)
            return
        }

        val region = makeRegion(userLoc.coordinate, 8046.72) // 5 miles in meters

        val request = MKLocalSearchRequest()
        request.naturalLanguageQuery = "mini golf"
        request.region = region
        request.pointOfInterestFilter = MKPointOfInterestFilter(includingCategories = listOf("MKPOICategoryMiniGolf"))

        val search = MKLocalSearch(request = request)
        search.startWithCompletionHandler { response, error ->
            val items = response?.mapItems
            if (error != null || items.isNullOrEmpty()) {
                completion(null)
                return@startWithCompletionHandler
            }

            val sorted = items.mapNotNull { (it as? MKMapItem)?.toDTO() }.sortedBy { mapItem ->

                var mapLoc: CLLocation =  CLLocation(
                        latitude = mapItem.coordinate.latitude,
                        longitude = mapItem.coordinate.longitude
                    )

                mapLoc.distanceFromLocation(userLoc)
            }

            completion(sorted.firstOrNull())
        }
    }

    fun updateCameraRegion(selectedResult: MKMapItem? = null): MapRegionData? {
        if (selectedResult != null) {
            val original:  LocationCoordinate2D = if (majorVersion >= 26) {
                LocationCoordinate2D(
                    latitude = selectedResult.location.coordinate.useContents { latitude },
                    longitude = selectedResult.location.coordinate.useContents { longitude }
                )
            } else {
                LocationCoordinate2D(
                    latitude = selectedResult.placemark.coordinate.useContents { latitude },
                    longitude = selectedResult.placemark.coordinate.useContents { longitude }
                )
            }

            MapRegionData.fromCValue(makeRegion(original.toCValue()))
        } else if (_mapItems.value.isNotEmpty()) {
            val region = computeBoundingRegion(_mapItems.value, true)

            if (region != null) {
                return MapRegionData.fromCValue(region)
            }
        } else if (_userLocation.value != null) {
            val userLoc = _userLocation.value

            return MapRegionData(
                latitude = userLoc?.coordinate?.useContents { latitude } ?: 0.0,
                longitude = userLoc?.coordinate?.useContents { longitude } ?: 0.0,
                latitudeDelta = 0.05,
                longitudeDelta = 0.05

            )
        }
        return null
    }

    private fun computeBoundingRegion(items: List<MKMapItem>, offsetDownward: Boolean = false): CValue<MKCoordinateRegion>? {
        if (items.isEmpty()) return null



        val coords:   List<CValue<CLLocationCoordinate2D>> = if (majorVersion >= 26) {
            items.map { it.location.coordinate }
        } else {
            items.map { it.placemark.coordinate }
        }

        val minLat = coords.minOfOrNull { it.useContents { latitude } } ?: 0.0
        val maxLat = coords.maxOfOrNull { it.useContents { latitude } } ?: 0.0
        val minLon = coords.minOfOrNull { it.useContents { longitude } } ?: 0.0
        val maxLon = coords.maxOfOrNull { it.useContents { longitude } } ?: 0.0

        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon
        val topPaddingFactor = 0.15
        val bottomPaddingFactor = 0.1

        val paddedLatDelta = latRange / 0.5
        val topPadding = paddedLatDelta * topPaddingFactor
        val bottomPadding = paddedLatDelta * bottomPaddingFactor
        val latitudeDelta = paddedLatDelta + topPadding + bottomPadding

        val horizontalPaddingPoints = 50.0
        val screenWidthPoints = UIScreen.mainScreen.bounds.useContents { size.width }
        val horizontalPaddingFraction = horizontalPaddingPoints / screenWidthPoints

        val longitudeDelta = lonRange * (1.0 + horizontalPaddingFraction * 2.0)

        val regionTop = maxLat + topPadding
        val centerLat = regionTop - latitudeDelta * 0.5
        val centerLon = (minLon + maxLon) / 2.0

        return cValue<MKCoordinateRegion> {
            this.center.latitude = centerLat
            this.center.longitude = centerLon
            this.span.latitudeDelta = latitudeDelta
            this.span.longitudeDelta = longitudeDelta
        }
    }

    fun getPostalAddress(mapItem: MapItemDTO): String {
        return mapItem.address?.fullAddress ?: mapItem.address?.shortAddress?: ""
    }

    fun makeRegion(coord: CValue<CLLocationCoordinate2D>, radiusInMeters: Double = 5000.0): CValue<MKCoordinateRegion> {
        return MKCoordinateRegionMakeWithDistance(coord, radiusInMeters * 2, radiusInMeters * 2)
    }
}