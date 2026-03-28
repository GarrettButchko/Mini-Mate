package com.garrettbutchko.minimate.di

import com.garrettbutchko.minimate.managers.GameManager
import com.garrettbutchko.minimate.repositories.UnifiedGameRepository
import com.garrettbutchko.minimate.viewModels.CourseLocationViewModel
import com.garrettbutchko.minimate.viewModels.CourseSearchViewModel
import com.garrettbutchko.minimate.viewModels.CourseViewModel
import com.garrettbutchko.minimate.viewModels.GameViewModel
import com.garrettbutchko.minimate.viewModels.HostViewModel
import com.garrettbutchko.minimate.viewModels.JoinViewModel
import com.garrettbutchko.minimate.viewModels.StatsViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

// This object acts as a provider for Swift
object KoinHelper : KoinComponent {
    fun getGameViewModel(): GameViewModel = get()
    fun getCourseViewModel(): CourseViewModel = get()
    fun getCourseLocationViewModel(): CourseLocationViewModel = get()
    fun getCourseSearchViewModel(): CourseSearchViewModel = get()
    fun getHostViewModel(): HostViewModel = get()
    fun getJoinViewModel(): JoinViewModel = get()
    fun getStatsViewModel(): StatsViewModel = get()
    fun getUnifiedGameRepo(): UnifiedGameRepository = get()
    fun getGameManager(): GameManager = get()
}
