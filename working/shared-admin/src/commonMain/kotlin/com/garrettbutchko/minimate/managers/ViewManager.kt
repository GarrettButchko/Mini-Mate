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
    data object SignIn : ViewType()
    data object Welcome : ViewType()
    data object CourseList : ViewType()
    data class CourseTab(val tab: Int) : ViewType()
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
            _currentView.value = ViewType.CourseList
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

    fun navigateToCourseTab(tab: Int) {
        _currentView.value = ViewType.CourseTab(tab)
    }

    fun navigateToCourseList() {
        _currentView.value = ViewType.CourseList
    }

    override fun navigateToSignIn() {
        _currentView.value = ViewType.SignIn
    }

    override fun navigateToWelcome() {
        _currentView.value = ViewType.Welcome
    }

    override fun navigateAfterSignIn() {
        navigateToCourseList()
    }

    // Equivalent to the Swift Equatable extension that ignores associated values.
    fun isSameViewTypeBase(other: ViewType): Boolean {
        return this.currentView.value::class == other::class
    }
}
