package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.dataModels.courseModels.SocialLink
import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO
import com.garrettbutchko.minimate.interfaces.LocationFinding
import com.garrettbutchko.minimate.repositories.CourseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseLocationViewModel(
    private val locationHandler: LocationFinding
) {
    private val _courseName = MutableStateFlow("")
    val courseName: StateFlow<String> = _courseName.asStateFlow()

    private val _postalAddress = MutableStateFlow("")
    val postalAddress: StateFlow<String> = _postalAddress.asStateFlow()

    private val _phoneNumber = MutableStateFlow<String?>(null)
    val phoneNumber: StateFlow<String?> = _phoneNumber.asStateFlow()

    private val _phoneNumberURL = MutableStateFlow<String?>(null)
    val phoneNumberURL: StateFlow<String?> = _phoneNumberURL.asStateFlow()

    private val _websiteURL = MutableStateFlow<String?>(null)
    val websiteURL: StateFlow<String?> = _websiteURL.asStateFlow()

    private val _course = MutableStateFlow<Course?>(null)
    val course: StateFlow<Course?> = _course.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            locationHandler.selectedItem.collect { mapItem ->
                updateState(mapItem)
            }
        }
    }

    val isCourseSupported: Boolean
        get() = _course.value?.isSupported ?: false

    val socialLinks: List<SocialLink>
        get() = _course.value?.socialLinks ?: emptyList()

    fun setCourseName(name: String) {
        _courseName.value = name
    }

    fun setPostalAddress(address: String) {
        _postalAddress.value = address
    }

    fun setPhoneNumber(number: String?) {
        _phoneNumber.value = number
    }

    fun setPhoneNumberURL(url: String?) {
        _phoneNumberURL.value = url
    }

    fun setWebsiteURL(url: String?) {
        _websiteURL.value = url
    }

    fun setCourse(course: Course?) {
        _course.value = course
    }

    fun setIsLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun close() {
        locationHandler.setSelectedItem(null)
        locationHandler.updateCameraRegion()
    }

    fun claimCourse() {
        println("Claim course button tapped.")
    }

    private fun updateState(mapItem: MapItemDTO?) {
        val name = mapItem?.name
        if (mapItem == null || name == null) {
            clearState()
            return
        }

        _courseName.value = name
        _postalAddress.value = locationHandler.getPostalAddress(mapItem)
        
        val phone = mapItem.phoneNumber
        _phoneNumber.value = phone

        if (phone != null) {
            val digits = phone.filter { it.isDigit() }
            _phoneNumberURL.value = "tel://$digits"
        } else {
            _phoneNumberURL.value = null
        }

        _websiteURL.value = mapItem.url
        _course.value = null
        _isLoading.value = true
    }

    private fun clearState() {
        _courseName.value = ""
        _postalAddress.value = ""
        _phoneNumber.value = null
        _phoneNumberURL.value = null
        _websiteURL.value = null
        _course.value = null
        _isLoading.value = false
    }
}
