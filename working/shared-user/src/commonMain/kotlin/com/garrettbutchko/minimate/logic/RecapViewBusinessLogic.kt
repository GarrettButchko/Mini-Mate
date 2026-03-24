package com.garrettbutchko.minimate.logic

import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.dataModels.playerModels.Player

enum class PlayerStanding {
    FIRST,
    SECOND,
    THIRD
}

/**
 * A platform-agnostic class designed to be easily migrated to a Kotlin Multiplatform shared module.
 * It contains pure business and presentation logic for the recap screen.
 */
class RecapViewBusinessLogic {

    // MARK: - Business Logic

    /**
     * Sorts players by their total strokes in ascending order
     */
    fun sortPlayers(game: Game?): List<Player> {
        return game?.players?.sortedBy { it.totalStrokes } ?: emptyList()
    }

    // MARK: - Presentation Logic

    /**
     * Returns the medal emoji associated with a standing
     */
    fun getMoji(place: PlayerStanding?): String {
        return when (place) {
            PlayerStanding.FIRST -> "🥇"
            PlayerStanding.SECOND -> "🥈"
            PlayerStanding.THIRD -> "🥉"
            else -> ""
        }
    }

    /**
     * Returns the platform-agnostic image size based on standing
     */
    fun getImageSize(place: PlayerStanding?): Double {
        return when (place) {
            PlayerStanding.FIRST -> 70.0
            PlayerStanding.SECOND, PlayerStanding.THIRD -> 40.0
            else -> 30.0
        }
    }

    /**
     * Formats the player's name based on their placement and whether they are the only player
     */
    fun formatPlayerName(name: String, place: PlayerStanding?, onlyPlayer: Boolean): String {
        return if (place != null && !onlyPlayer) {
            name + getMoji(place)
        } else {
            name
        }
    }
}
