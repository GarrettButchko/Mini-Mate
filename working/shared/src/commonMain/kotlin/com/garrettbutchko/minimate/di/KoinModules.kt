package com.garrettbutchko.minimate.di


import org.koin.core.module.Module
import org.koin.dsl.module

import com.garrettbutchko.minimate.repositories.userRepos.LocalUserRepository
import com.garrettbutchko.minimate.repositories.userRepos.RemoteUserRepository
import com.garrettbutchko.minimate.repositories.userRepos.UserRepository
import com.garrettbutchko.minimate.room.AppDatabase
import com.garrettbutchko.minimate.repositories.gameRepos.LocalGameRepository
import com.garrettbutchko.minimate.repositories.gameRepos.RemoteGameRepository

val sharedModule = module {
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().gameDao() }

    // 2. Repositories
    // It's cleaner to keep the logic in the UserRepository and let Koin
    // handle the construction of its sub-repos.
    single { LocalUserRepository(get()) }
    single { RemoteUserRepository() }

    single { LocalGameRepository(get()) }
    single { RemoteGameRepository() }

    // Main interface/entry point for your ViewModels
    single { UserRepository(localRepo = get(), remoteRepo = get()) }
}

// Platform-specific modules (defined per platform)
expect val platformModule: Module