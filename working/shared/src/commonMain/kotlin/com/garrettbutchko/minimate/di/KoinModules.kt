package com.garrettbutchko.minimate.di

import com.garrettbutchko.minimate.Repositories.GameRepos.LocalGameRepository
import org.koin.core.module.Module
import org.koin.dsl.module

import com.garrettbutchko.minimate.Repositories.UserRepos.LocalUserRepository
import com.garrettbutchko.minimate.Repositories.UserRepos.RemoteUserRepository
import com.garrettbutchko.minimate.Repositories.UserRepos.UserRepository
import com.garrettbutchko.minimate.Room.AppDatabase
import com.garrettbutchko.minimate.repositories.RemoteGameRepository

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