package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.datamodels.Game
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class GameInfoViewModel(val game: Game) {

    val gameId: String = game.id

    val playerCount: String = game.players.size.toString()

    val holeCount: String = game.numberOfHoles.toString()

    val dateStarted: String
        get() {
            val instant = Instant.fromEpochSeconds(game.date.seconds, game.date.nanoseconds.toLong())
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            // Basic formatting for short date (e.g., "Dec 6, 2025" or similar based on locale formatting needs in commonMain)
            val monthStr = dateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            return "$monthStr ${dateTime.day}, ${dateTime.year}"
        }

    val location: String = game.locationName ?: "No Location"

    val courseId: String = game.courseID ?: "No Course ID"
}