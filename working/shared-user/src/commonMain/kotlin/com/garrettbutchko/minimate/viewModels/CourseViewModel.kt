package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.dataModels.MapRegionData
import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO
import com.garrettbutchko.minimate.functions.getAddress
import com.garrettbutchko.minimate.interfaces.LocationFinding
import com.garrettbutchko.minimate.repositories.CourseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface CourseMapActions {
    fun getDirections(mapItem: MapItemDTO)
}

class CourseViewModel(
    private val courseRepo: CourseRepository,
    private val mapActions: CourseMapActions,
    private val locationHandler: LocationFinding
) {
    private val _nameExists = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val nameExists: StateFlow<Map<String, Boolean>> = _nameExists.asStateFlow()

    private val _position = MutableStateFlow<MapRegionData?>(null)
    val position: StateFlow<MapRegionData?> = _position.asStateFlow()

    private val _isUpperHalf = MutableStateFlow(false)
    val isUpperHalf: StateFlow<Boolean> = _isUpperHalf.asStateFlow()

    private val _hasAppeared = MutableStateFlow(false)
    val hasAppeared: StateFlow<Boolean> = _hasAppeared.asStateFlow()

    private val _isLoadingCourses = MutableStateFlow(false)
    val isLoadingCourses: StateFlow<Boolean> = _isLoadingCourses.asStateFlow()

    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    fun setNameExists(value: Map<String, Boolean>) {
        _nameExists.value = value
    }

    fun setPosition(value: MapRegionData?) {
        _position.value = value
    }

    fun setIsUpperHalf(value: Boolean) {
        _isUpperHalf.value = value
    }

    fun setHasAppeared(value: Boolean) {
        _hasAppeared.value = value
    }

    fun setIsLoadingCourses(value: Boolean) {
        _isLoadingCourses.value = value
    }

    fun setCourse(course: Course?) {
        if (course != null) {
            _selectedCourse.value = course
        }
    }

    fun preloadNameChecks(items: List<MapItemDTO>) {
        items.forEach { item ->
            val name = item.name ?: return@forEach
            if (_nameExists.value.containsKey(name)) return@forEach

            scope.launch {
                val exists = courseRepo.courseNameExistsAndSupported(name)
                val updatedMap = _nameExists.value.toMutableMap()
                updatedMap[name] = exists
                _nameExists.value = updatedMap
            }
        }
    }

    fun onAppearance() {
        if (!_hasAppeared.value) {
            _hasAppeared.value = true
            _isUpperHalf.value = false
            locationHandler.setMapItems(emptyList())
            locationHandler.setSelectedItem(null)
            _position.value = locationHandler.updateCameraRegion(null)
        }
    }

    fun setPosition(position: MapRegionData) {
        _position.value = position
    }

    fun searchNearby() {
        _isLoadingCourses.value = true
        _isUpperHalf.value = true
        
        locationHandler.searchNearbyCourses(0.03, 0.1, 0.1) { success, newPosition ->
            if (newPosition != null) {
                _position.value = newPosition
            }
            _isLoadingCourses.value = !success
        }
    }

    fun cancel() {
        _isUpperHalf.value = false
        locationHandler.setMapItems(emptyList())
        _position.value = locationHandler.updateCameraRegion(null)
    }

    fun updatePosition(mapItem: MapItemDTO) {
        locationHandler.setSelectedItem(mapItem)
        _position.value = locationHandler.updateCameraRegion(locationHandler.selectedItem.value)
    }

    fun getDirections(mapItem: MapItemDTO) {
        if (locationHandler.selectedItem.value != null) {
            mapActions.getDirections(locationHandler.selectedItem.value!!)
        }
    }

    fun getPostalAddress(mapItem: MapItemDTO): String {
        return getAddress(mapItem)
    }
}
