package com.garrettbutchko.minimate.di

import org.koin.dsl.module

import com.garrettbutchko.minimate.interfaces.LocationFinding
import com.garrettbutchko.minimate.utilities.LocationHandler
import com.garrettbutchko.minimate.viewModels.CourseMapActions
import com.garrettbutchko.minimate.utilities.CourseMapActionsImpl

val sharedUserAndroidModule = module {
    single<LocationFinding> { LocationHandler() }
    single<CourseMapActions> { CourseMapActionsImpl() }
}
