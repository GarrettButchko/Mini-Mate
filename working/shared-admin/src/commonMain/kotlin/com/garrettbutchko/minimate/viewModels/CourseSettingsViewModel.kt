package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.Platform
import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.dataModels.courseModels.SocialLink
import com.garrettbutchko.minimate.dataModels.courseModels.SocialPlatform
import com.garrettbutchko.minimate.di.platformModule
import com.garrettbutchko.minimate.repositories.CourseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.gitlive.firebase.storage.Data

enum class ColorAddTarget {
    SCORE_CARD_COLOR,
    COURSE_COLOR
}

sealed class ColorDeleteTarget(val id: Int) {
    object ScoreCardColor : ColorDeleteTarget(-1)
    data class CourseColor(val index: Int) : ColorDeleteTarget(index)
}

open class CourseSettingsViewModel(
    private val courseRepo: CourseRepository = CourseRepository(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    // MARK: - Published Properties
    private val _editCourse = MutableStateFlow(false)
    val editCourse: StateFlow<Boolean> = _editCourse.asStateFlow()

    private val _showingPickerLogo = MutableStateFlow(false)
    val showingPickerLogo: StateFlow<Boolean> = _showingPickerLogo.asStateFlow()

    private val _showingPickerAd = MutableStateFlow(false)
    val showingPickerAd: StateFlow<Boolean> = _showingPickerAd.asStateFlow()

    private val _showReviewSheet = MutableStateFlow(false)
    val showReviewSheet: StateFlow<Boolean> = _showReviewSheet.asStateFlow()

    private val _deleteTarget = MutableStateFlow<ColorDeleteTarget?>(null)
    val deleteTarget: StateFlow<ColorDeleteTarget?> = _deleteTarget.asStateFlow()

    // Password related
    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _showNewPassword = MutableStateFlow(false)
    val showNewPassword: StateFlow<Boolean> = _showNewPassword.asStateFlow()

    private val _showPassword = MutableStateFlow(false)
    val showPassword: StateFlow<Boolean> = _showPassword.asStateFlow()

    private val _showChangePasswordAlert = MutableStateFlow(false)
    val showChangePasswordAlert: StateFlow<Boolean> = _showChangePasswordAlert.asStateFlow()

    // Par Configuration related
    private val _showParConfiguration = MutableStateFlow(false)
    val showParConfiguration: StateFlow<Boolean> = _showParConfiguration.asStateFlow()

    // MARK: - Computed Properties
    val isValidPassword: Boolean
        get() = _newPassword.value.isNotEmpty() && _newPassword.value == _confirmPassword.value

    fun setEditCourse(value: Boolean) { _editCourse.value = value }
    fun setShowingPickerLogo(value: Boolean) { _showingPickerLogo.value = value }
    fun setShowingPickerAd(value: Boolean) { _showingPickerAd.value = value }
    fun setShowReviewSheet(value: Boolean) { _showReviewSheet.value = value }
    fun setDeleteTarget(value: ColorDeleteTarget?) { _deleteTarget.value = value }
    fun setNewPassword(value: String) { _newPassword.value = value }
    fun setConfirmPassword(value: String) { _confirmPassword.value = value }
    fun setShowNewPassword(value: Boolean) { _showNewPassword.value = value }
    fun setShowPassword(value: Boolean) { _showPassword.value = value }
    fun setShowChangePasswordAlert(value: Boolean) { _showChangePasswordAlert.value = value }
    fun setShowParConfiguration(value: Boolean) { _showParConfiguration.value = value }

    // MARK: - Image Upload
    
    fun uploadLogoImage(imageData: Data, course: Course, onCourseUpdated: (Course) -> Unit) {
        coroutineScope.launch {
            val url = courseRepo.uploadCourseImage(course.id, imageData, "logoImage")
            if (url != null) {
                val updatedCourse = course.copy(logo = url)
                onCourseUpdated(updatedCourse)
                courseRepo.addOrUpdateCourse(updatedCourse)
            } else {
                println("❌ Photo upload failed")
            }
        }
    }

    fun deleteLogoImage(course: Course, onCourseUpdated: (Course) -> Unit) {
        coroutineScope.launch {
            val updatedCourse = course.copy(logo = null)
            onCourseUpdated(updatedCourse)
            courseRepo.deleteCourseItem(course.id, "logo")
            courseRepo.deleteCourseImage(course.id, "logoImage")
        }
    }

    fun uploadAdImage(imageData: Data, course: Course, onCourseUpdated: (Course) -> Unit) {
        coroutineScope.launch {
            val url = courseRepo.uploadCourseImage(course.id, imageData, "adImage")
            if (url != null) {
                val updatedCourse = course.copy(adImage = url)
                onCourseUpdated(updatedCourse)
                courseRepo.addOrUpdateCourse(updatedCourse)
            } else {
                println("❌ Photo upload failed")
            }
        }
    }

    fun deleteAdImage(course: Course, onCourseUpdated: (Course) -> Unit) {
        coroutineScope.launch {
            val updatedCourse = course.copy(adImage = null)
            onCourseUpdated(updatedCourse)
            courseRepo.deleteCourseItem(course.id, "adImage")
            courseRepo.deleteCourseImage(course.id, "adImage")
        }
    }

    // MARK: - Password Change

    fun changePassword(course: Course?, userID: String?, onCourseUpdated: (Course) -> Unit) {
        if (course == null || !isValidPassword || userID == null) return
        
        val updatedCourse = course.copy(password = _newPassword.value, adminIDs = listOf(userID))
        onCourseUpdated(updatedCourse)
        
        coroutineScope.launch {
            courseRepo.addOrUpdateCourse(updatedCourse).isSuccess
        }
        
        resetPasswordFields()
    }

    fun resetPasswordFields() {
        _newPassword.value = ""
        _confirmPassword.value = ""
        _showNewPassword.value = false
        _showChangePasswordAlert.value = false
    }

    // MARK: - Color Deletion

    fun deleteColor(target: ColorDeleteTarget, course: Course?, onCourseUpdated: (Course) -> Unit) {
        if (course == null) return

        when (target) {
            is ColorDeleteTarget.ScoreCardColor -> {
                val updatedCourse = course.copy(scoreCardColorDT = null)
                onCourseUpdated(updatedCourse)
                
                coroutineScope.launch {
                    courseRepo.deleteCourseItem(updatedCourse.id, "scoreCardColorDT")
                }
            }
            is ColorDeleteTarget.CourseColor -> {
                val colors = course.courseColorsDT?.toMutableList() ?: mutableListOf()
                if (target.index in colors.indices) {
                    colors.removeAt(target.index)
                    
                    val updatedCourse = course.copy(courseColorsDT = colors)
                    onCourseUpdated(updatedCourse)
                    
                    coroutineScope.launch {
                        if (colors.isEmpty()) {
                            courseRepo.deleteCourseItem(updatedCourse.id, "courseColorsDT")
                        } else {
                            courseRepo.setCourseItem(updatedCourse.id, "courseColorsDT", colors)
                        }
                    }
                }
            }
        }
    }
}