package com.garrettbutchko.minimate.managers

import com.garrettbutchko.minimate.interfaces.AppNavigationManaging
import com.garrettbutchko.minimate.repositories.FirebaseAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ViewType {
    data class Main(val tab: Int) : ViewType()
    data object Welcome : ViewType()
    data class ScoreCard(val isGuest: Boolean = false) : ViewType()
    data class Ad(val isGuest: Boolean = false) : ViewType()
    data object SignIn : ViewType()
    data object Host : ViewType()
}

class ViewManager(
    private val authRepository: FirebaseAuthRepository
) : AppNavigationManaging {

    private val _currentView = MutableStateFlow<ViewType>(ViewType.Welcome)
    val currentView: StateFlow<ViewType> = _currentView.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        val user = authRepository.currentUser
        if (user != null && user.isEmailVerified) {
            _currentView.value = ViewType.Main(tab = 1)
        } else {
            scope.launch {
                authRepository.logout()
            }
            _currentView.value = ViewType.Welcome
        }
    }

    fun setCurrentView(view: ViewType) {
        _currentView.value = view
    }

    fun navigateToMain(tab: Int) {
        _currentView.value = ViewType.Main(tab)
    }

    override fun navigateToSignIn() {
        _currentView.value = ViewType.SignIn
    }

    override fun navigateToWelcome() {
        _currentView.value = ViewType.Welcome
    }

    fun navigateToScoreCard(isGuest: Boolean = false) {
        _currentView.value = ViewType.ScoreCard(isGuest)
    }

    fun navigateToHost() {
        _currentView.value = ViewType.Host
    }

    fun navigateToAd(isGuest: Boolean = false) {
        _currentView.value = ViewType.Ad(isGuest)
    }

    override fun navigateAfterSignIn() {
        navigateToMain(1)
    }

    // Equivalent to the Swift Equatable extension that ignores associated values.
    fun isSameViewTypeBase(other: ViewType): Boolean {
        return this.currentView.value::class == other::class
    }
}
