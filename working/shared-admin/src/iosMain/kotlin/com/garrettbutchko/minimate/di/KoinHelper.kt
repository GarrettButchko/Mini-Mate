package com.garrettbutchko.minimate.di

import com.garrettbutchko.minimate.managers.ViewManager
import com.garrettbutchko.minimate.viewModels.AuthViewModel
import com.garrettbutchko.minimate.viewModels.ProfileViewModel
import com.garrettbutchko.minimate.viewModels.CourseListViewModel
import com.garrettbutchko.minimate.viewModels.CourseSettingsViewModel
import com.garrettbutchko.minimate.viewModels.LeaderBoardViewModel
import com.garrettbutchko.minimate.viewModels.analytics.AnalyticsViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

// This object acts as a provider for Swift
object KoinHelper : KoinComponent {
    fun getAuthViewModel(): AuthViewModel = get()
    fun getProfileViewModel(): ProfileViewModel = get()
    fun getCourseListViewModel(): CourseListViewModel = get()
    fun getCourseSettingsViewModel(): CourseSettingsViewModel = get()
    fun getLeaderBoardViewModel(): LeaderBoardViewModel = get()
    fun getAnalyticsViewModel(): AnalyticsViewModel = get()
    fun getViewManager(): ViewManager = get()
}
