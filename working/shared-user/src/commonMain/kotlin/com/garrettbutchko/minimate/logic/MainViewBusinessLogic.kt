package com.garrettbutchko.minimate.logic

import com.garrettbutchko.minimate.dataModels.gameModels.Game

/**
 * A platform-agnostic class designed to be easily migrated to a Kotlin Multiplatform shared module.
 * It contains pure business, calculation, and formatting rules for the main view.
 */
class MainViewBusinessLogic {
    
    // MARK: - Access Rules
    
    /**
     * Determines if the user is restricted from playing based on their pro status and game count.
     */
    fun isPlayDisabled(isPro: Boolean?, gameCount: Int): Boolean {
        val proStatus = isPro ?: false
        return !proStatus && gameCount >= 2
    }
    
    /**
     * Determines if the Pro upgrade stopper message should be shown.
     */
    fun shouldShowProStopper(isPro: Boolean?, gameCount: Int): Boolean {
        return isPlayDisabled(isPro = isPro, gameCount = gameCount)
    }
    
    // MARK: - Data Filtering
    
    // MARK: - Presentation Formatting
    
    /**
     * Provides a fallback string for the user's greeting name.
     */
    fun getGreetingName(name: String?): String {
        return name ?: "User"
    }
    
    /**
     * Provides the correct header title based on the online mode state.
     */
    fun getHeaderTitle(isOnlineMode: Boolean): String {
        return if (isOnlineMode) "Online Options" else "Start a Round"
    }
    
    /**
     * Provides the correct informational message based on the online mode state.
     */
    fun getInfoMessage(isOnlineMode: Boolean): String {
        return if (isOnlineMode) {
            "Host starts a server game. Join connects to an existing one. Multiple devices sync in real time."
        } else {
            "Quick starts a local game. Online lets you host or join a networked game."
        }
    }
    
    /**
     * Formats the winner's name to include a medal emoji.
     */
    fun formatWinnerName(name: String?): String {
        val baseName = name ?: "N/A"
        return "$baseName 🥇"
    }
    
    // MARK: - Configuration Rules
    
    /**
     * The duration in seconds that the Pro promotion text should be displayed before hiding.
     */
    val proPromotionDisplayDuration: Double = 7.0
}
