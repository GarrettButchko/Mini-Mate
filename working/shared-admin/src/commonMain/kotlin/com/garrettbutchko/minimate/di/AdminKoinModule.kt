package com.garrettbutchko.minimate.di

import com.garrettbutchko.minimate.interfaces.AppNavigationManaging
import com.garrettbutchko.minimate.managers.ViewManager
import com.garrettbutchko.minimate.viewModels.CourseListViewModel
import com.garrettbutchko.minimate.viewModels.CourseSettingsViewModel
import com.garrettbutchko.minimate.viewModels.LeaderBoardViewModel
import com.garrettbutchko.minimate.viewModels.analytics.AnalyticsViewModel
import com.garrettbutchko.minimate.viewModels.analytics.ExperienceViewModel
import com.garrettbutchko.minimate.viewModels.analytics.GrowthViewModel
import com.garrettbutchko.minimate.viewModels.analytics.OperationsViewModel
import com.garrettbutchko.minimate.viewModels.analytics.RetentionViewModel
import org.koin.dsl.module

val sharedAdminModule = module {
    // ViewManager
    single { ViewManager(authRepository = get()) }
    single<AppNavigationManaging> { get<ViewManager>() }

    // ViewModels
    factory { CourseListViewModel(courseRepo = get(), userRepo = get(), authModel = get()) }
    factory { CourseSettingsViewModel(courseRepo = get()) }
    factory { LeaderBoardViewModel(lbRepo = get()) }
    factory { AnalyticsViewModel(analyticsRepo = get()) }
}
