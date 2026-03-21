package com.garrettbutchko.minimate.di


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
import com.garrettbutchko.minimate.viewModels.ProfileViewModel

val sharedModule = module {
    // Daos
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().gameDao() }

    // Repositories (Internal helpers)
    single { LocalUserRepository(userDao = get()) }
    single { RemoteUserRepository() }
    single { LocalGameRepository(gameDao = get()) }
    single { RemoteGameRepository() }

    // Public Repositories (Interface based)
    single { UserRepository(localRepo = get(), remoteRepo = get()) }
    single { FirebaseAuthRepository() }

    // ViewModels (Using factory or viewModel keyword)
    factory { AuthViewModel(authRepository = get()) }
    factory { ProfileViewModel(
        authModel = get(),
        userRepo = get(),
        userRemoteRepo = get(),
        localGameRepo = get(),
        viewManager = get()
    ) }
}

// Platform-specific modules (defined per platform)
expect val platformModule: Module