package com.garrettbutchko.minimate.logic

/**
 * A platform-agnostic class designed to be easily migrated to a Kotlin Multiplatform shared module.
 * It contains pure business, logic, and formatting rules for the scorecard view.
 */
class ScoreCardBusinessLogic {
    
    // MARK: - Game Calculations
    
    /**
     * Determines the correct number of holes to display on the scorecard.
     */
    fun getHoleCount(hasCustomPar: Boolean?, courseNumHoles: Int?, gameNumHoles: Int): Int {
        return if (hasCustomPar == false && courseNumHoles != null) {
            courseNumHoles
        } else {
            gameNumHoles
        }
    }
    
    /**
     * Calculates the total par for a course based on its individual hole pars.
     */
    fun calculateTotalPar(pars: List<Int?>): Int {
        return pars.filterNotNull().sum()
    }
    
    // MARK: - UI Presentation Rules
    
    /**
     * Determines if ad banners should be displayed to the current user.
     */
    fun shouldShowAds(isConnected: Boolean, isGuest: Boolean, isPro: Boolean?): Boolean {
        val isUserPro = isPro ?: false
        return isConnected && (isGuest || !isUserPro)
    }
    
    /**
     * Provides the correct back button label based on the user's login state.
     */
    fun getBackButtonText(isGuest: Boolean): String {
        return if (isGuest) "Back to Sign In Menu" else "Go Back to Main Menu"
    }
    
    /**
     * Provides the correct back button system icon name based on the user's login state.
     */
    fun getBackButtonIcon(isGuest: Boolean): String {
        return if (isGuest) "person.crop.circle" else "house.fill"
    }
    
    /**
     * Formats a raw seconds integer into a MM:SS string.
     */
    fun formatTimeString(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "$minutes:${secs.toString().padStart(2, '0')}"
    }
    
    // MARK: - Action Handlers
    
    /**
     * Safely processes the end game action, ensuring it is only uploaded/persisted once.
     */
    fun processEndGame(hasUploaded: Boolean, persistAction: () -> Unit, markAsUploaded: () -> Unit) {
        if (!hasUploaded) {
            persistAction()
            markAsUploaded()
        }
    }
}
