@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.garrettbutchko.minimate.viewModelsiOSAddOns

import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.viewModels.CourseSettingsViewModel
import dev.gitlive.firebase.storage.Data
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

private val log = Logger.withTag("CourseSettingsViewModelIOS")

fun CourseSettingsViewModel.uploadLogoImageIOS(newImage: UIImage?, course: Course?, onCourseUpdated: (Course) -> Unit) {

    if (course == null) {
        log.e { "Missing Course" }
        return
    }


    if (newImage == null) {
        log.e { "Photo upload failed: Missing image" }
        return
    }

    val jpegData = UIImageJPEGRepresentation(newImage, 0.8)
    if (jpegData == null) {
        log.e { "Photo upload failed: Could not process image" }
        return
    }

    val data = Data(jpegData)
    uploadLogoImage(data, course, onCourseUpdated)
}

fun CourseSettingsViewModel.uploadAdImageIOS(newImage: UIImage?, course: Course?, onCourseUpdated: (Course) -> Unit) {
    if (course == null) {
        log.e { "Missing Course" }
        return
    }

    if (newImage == null) {
        log.e { "Photo upload failed: Missing image" }
        return
    }

    val jpegData = UIImageJPEGRepresentation(newImage, 0.8)
    if (jpegData == null) {
        log.e { "Photo upload failed: Could not process image" }
        return
    }

    val data = Data(jpegData)
    uploadAdImage(data, course, onCourseUpdated)
}
