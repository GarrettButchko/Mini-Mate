package com.garrettbutchko.minimate.di

import org.koin.dsl.module

import com.garrettbutchko.minimate.repositories.UnifiedGameRepository
import com.garrettbutchko.minimate.managers.ViewManager
import com.garrettbutchko.minimate.interfaces.AppNavigationManaging
import com.garrettbutchko.minimate.utilities.NetworkChecker

val sharedUserModule = module {
    // It can safely call get() for LocalGameRepository and RemoteGameRepository
    // as long as the core sharedModule is loaded alongside it.
    single { UnifiedGameRepository(local = get(), remote = get()) }
    single { ViewManager() }
    single<AppNavigationManaging> { get<ViewManager>() }
}
