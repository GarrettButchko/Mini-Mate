package com.garrettbutchko.minimate.di

import org.koin.dsl.module

import com.garrettbutchko.minimate.repositories.UnifiedGameRepository
import com.garrettbutchko.minimate.managers.ViewManager
import com.garrettbutchko.minimate.managers.GameManager
import com.garrettbutchko.minimate.interfaces.AppNavigationManaging
import com.garrettbutchko.minimate.repositories.LiveGameRepository
import com.garrettbutchko.minimate.viewModels.CourseLocationViewModel
import com.garrettbutchko.minimate.viewModels.GameViewModel
import com.garrettbutchko.minimate.viewModels.CourseViewModel
import com.garrettbutchko.minimate.viewModels.CourseSearchViewModel
import com.garrettbutchko.minimate.viewModels.HostViewModel
import com.garrettbutchko.minimate.viewModels.JoinViewModel
import com.garrettbutchko.minimate.viewModels.StatsViewModel

val sharedUserModule = module {
    // It can safely call get() for LocalGameRepository and RemoteGameRepository
    // as long as the core sharedModule is loaded alongside it.

    // Repositories
    // Games
    single { UnifiedGameRepository(local = get(), remote = get()) }
    single { LiveGameRepository() }

    // ViewManager
    single { ViewManager(authRepository = get()) }
    single<AppNavigationManaging> { get<ViewManager>() }

    // GameManager
    single { GameManager(authModel = get(), localGameRepo = get(), remoteGameRepo = get()) }

    //ViewModels
    single {
        GameViewModel(
            authModel = get(),
            liveGameRepo = get(),
            unifiedGameRepository = get(),
            localGameRepository = get(),
            courseRepo = get(),
            analyticsRepo = get(),
            remoteUserRepo = get(),
            locationHandler = get()
        )
    }

    single {
        CourseViewModel(
            courseRepo = get(),
            mapActions = get(),
            locationHandler = get()
        )
    }

    factory {
        CourseSearchViewModel(
            locationHandler = get(),
            courseViewModel = get()
        )
    }

    factory {
        CourseLocationViewModel(
            locationHandler = get()
        )
    }

    factory {
        HostViewModel(
            gameModel = get(),
            courseRepo = get(),
            viewManager = get()
        )
    }

    factory {
        JoinViewModel(
            gameModel = get(),
            authModel = get()
        )
    }

    factory {
        StatsViewModel(
            localGameRepo = get(),
            remoteGameRepo = get(),
            authModel = get(),
            gameManager = get()
        )
    }
}
