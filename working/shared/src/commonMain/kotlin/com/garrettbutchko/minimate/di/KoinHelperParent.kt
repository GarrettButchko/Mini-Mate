package com.garrettbutchko.minimate.di

import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.interfaces.AppNavigationManaging
import com.garrettbutchko.minimate.repositories.gameRepos.LocalGameRepository
import com.garrettbutchko.minimate.repositories.gameRepos.RemoteGameRepository
import com.garrettbutchko.minimate.repositories.userRepos.LocalUserRepository
import com.garrettbutchko.minimate.repositories.userRepos.RemoteUserRepository
import com.garrettbutchko.minimate.repositories.userRepos.UserRepository
import com.garrettbutchko.minimate.viewModels.AuthViewModel
import com.garrettbutchko.minimate.viewModels.GameReviewViewModel
import com.garrettbutchko.minimate.viewModels.ProfileViewModel
import com.garrettbutchko.minimate.viewModels.WelcomeViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

// This object acts as a provider for Swift
object KoinHelperParent : KoinComponent {
    fun getGameReviewViewModel(game: Game): GameReviewViewModel {
        return get { parametersOf(game) }
    }

    fun getAuthViewModel(): AuthViewModel = get()
    fun getProfileViewModel(): ProfileViewModel = get()

    fun getWelcomeViewModel(welcomeText: String): WelcomeViewModel {
        return get { parametersOf(welcomeText) }
    }

    fun getViewManager(): AppNavigationManaging = get()

    fun getLocalGameRepo(): LocalGameRepository = get()
    fun getRemoteGameRepo(): RemoteGameRepository = get()

    fun getLocalUserRepo(): LocalUserRepository = get()
    fun getRemoteUserRepo(): RemoteUserRepository = get()

    fun getUserRepo(): UserRepository = get()
}
