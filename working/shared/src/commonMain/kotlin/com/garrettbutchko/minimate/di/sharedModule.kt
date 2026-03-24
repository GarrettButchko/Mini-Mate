package com.garrettbutchko.minimate.di


import com.garrettbutchko.minimate.repositories.AnalyticsRepository
import com.garrettbutchko.minimate.repositories.CourseLeaderboardRepository
import com.garrettbutchko.minimate.repositories.CourseRepository
import com.garrettbutchko.minimate.repositories.FirebaseAuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

import com.garrettbutchko.minimate.repositories.userRepos.LocalUserRepository
import com.garrettbutchko.minimate.repositories.userRepos.RemoteUserRepository
import com.garrettbutchko.minimate.repositories.userRepos.UserRepository
import com.garrettbutchko.minimate.room.AppDatabase
import com.garrettbutchko.minimate.repositories.gameRepos.LocalGameRepository
import com.garrettbutchko.minimate.repositories.gameRepos.RemoteGameRepository
import com.garrettbutchko.minimate.viewModels.AuthViewModel
import com.garrettbutchko.minimate.viewModels.GameReviewViewModel
import com.garrettbutchko.minimate.viewModels.ProfileViewModel
import com.garrettbutchko.minimate.viewModels.WelcomeViewModel
import com.garrettbutchko.minimate.utilities.NetworkChecker

val sharedModule = module {
    // Daos
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().gameDao() }

    // Repositories
    // User
    single { LocalUserRepository(userDao = get()) }
    single { RemoteUserRepository() }
    single { UserRepository(localRepo = get(), remoteRepo = get()) }

    // Games
    single { LocalGameRepository(gameDao = get()) }
    single { RemoteGameRepository() }

    // Course
    single { CourseRepository() }
    single { CourseLeaderboardRepository() }

    // Auth
    single { FirebaseAuthRepository() }

    // Analytics
    single{ AnalyticsRepository() }

    // ViewModels (Using factory or viewModel keyword)
    single { AuthViewModel(authRepository = get(), viewManager = get()) }

    factory { ProfileViewModel(
        authModel = get(),
        userRepo = get(),
        userRemoteRepo = get(),
        localGameRepo = get(),
        viewManager = get()
    ) }
    
    // WelcomeViewModel
    factory { params ->
        WelcomeViewModel(
            viewManager = get(),
            welcomeText = params.get(),
            networkChecker = NetworkChecker.shared
        )
    }

    factory { params ->
        GameReviewViewModel(
            game = params.get(),
            courseRepository = get()
        )
    }
}

// Platform-specific modules (defined per platform)
expect val platformModule: Module
