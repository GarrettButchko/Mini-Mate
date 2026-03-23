package com.garrettbutchko.minimate.logic

import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * A platform-agnostic class designed to be easily migrated to a Kotlin Multiplatform shared module.
 * It contains pure business and presentation logic, stripped of any UI or platform-specific imports.
 */
class HostViewBusinessLogic {

    // MARK: - Presentation Logic
    
    fun getHeaderTitle(isOnline: Boolean): String {
        return if (isOnline) "Hosting Game" else "Game Setup"
    }
    
    fun getPlayersHeaderText(playerCount: Int): String {
        return "Players: $playerCount"
    }
    
    // MARK: - View Logic
    
    /**
     * Determines if the game should be dismissed when the view disappears.
     */
    fun shouldDismissGameOnDisappear(isStarted: Boolean, isDismissed: Boolean, showHost: Boolean): Boolean {
        return !isStarted && !isDismissed && !showHost
    }
    
    /**
     * Orchestrates the actions required when a guest clicks the back button.
     */
    fun handleGuestBackAction(dismissGame: () -> Unit, navigateToSignIn: () -> Unit) {
        navigateToSignIn()
        dismissGame()
    }
    
    /**
     * Orchestrates the process of starting a game, conditionally handling guest storage.
     */
    fun handleStartGame(isGuest: Boolean, deleteGuestGame: () -> Unit, performStart: () -> Unit) {
        performStart()
        if (isGuest) {
            deleteGuestGame()
        }
    }
    
    /**
     * Evaluates user deletion and delegates the removal if valid.
     */
    fun handleDeletePlayer(playerId: String?, removePlayer: (String) -> Unit) {
        playerId?.let {
            removePlayer(it)
        }
    }
    
    // MARK: - ViewModel Timer & Formatting Logic
    
    /**
     * Calculates the remaining time based on the last updated date and time-to-live (TTL).
     */
    fun calculateTimeRemaining(lastUpdated: Instant, ttlSeconds: Double, currentTime: Instant): Double {
        val expire = lastUpdated.plus(ttlSeconds.seconds)
        val remaining = expire - currentTime
        return if (remaining.isPositive()) remaining.toDouble(kotlin.time.DurationUnit.SECONDS) else 0.0
    }
    
    /**
     * Validates if enough time has passed to allow a timer reset (spam prevention).
     */
    fun canResetTimer(lastResetTime: Instant?, cooldownSeconds: Double, currentTime: Instant): Boolean {
        if (lastResetTime == null) return true
        return (currentTime - lastResetTime) >= cooldownSeconds.seconds
    }
    
    /**
     * Formats an integer representing seconds into a "MM:SS" string.
     */
    fun formatTimeString(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "$minutes:${secs.toString().padStart(2, '0')}"
    }
    
    /**
     * Determines the number of holes to set for a course, defaulting to 18 if pars aren't provided.
     */
    fun determineDefaultHoles(parsCount: Int?): Int {
        return parsCount ?: 18
    }
}
