package com.garrettbutchko.minimate.analyzers

import com.garrettbutchko.minimate.datamodels.Game
import com.garrettbutchko.minimate.datamodels.Hole
import com.garrettbutchko.minimate.datamodels.Player
import com.garrettbutchko.minimate.datamodels.UserModel
import com.garrettbutchko.minimate.extensions.toInstant
import kotlin.math.round

class UserStatsAnalyzer(
    userModel: UserModel,
    private val games: List<Game>
) {
    val gameIDs: List<String> = userModel.gameIDs
    val userID: String = userModel.googleId

    // MARK: - Basic Stats

    fun totalGamesPlayed(): Int {
        return games.size
    }

    fun totalPlayersFaced(): Int {
        val userIDSet = games.flatMap { it.players.map { player -> player.userId } }.toSet()
        var count = 0
        for (id in userIDSet) {
            if (id != userID || id.contains("guest")) {
                count += 1
            }
        }
        return count
    }

    fun totalStrokes(): Int {
        return games
            .flatMap { it.players }
            .filter { it.userId == userID || it.userId.contains("guest") }
            .flatMap { it.holes }
            .sumOf { it.strokes }
    }

    fun totalHolesPlayed(): Int {
        return games
            .flatMap { it.players }
            .filter { it.userId == userID || it.userId.contains("guest") }
            .flatMap { it.holes }
            .size
    }

    fun averageStrokesPerGame(): Double {
        if (totalGamesPlayed() == 0) return 0.0
        return totalStrokes().toDouble() / totalGamesPlayed()
    }

    fun averageStrokesPerHole(): Double {
        if (totalHolesPlayed() == 0) return 0.0
        return totalStrokes().toDouble() / totalHolesPlayed()
    }

    // MARK: - Performance Stats

    fun bestGameStrokes(): Int? {
        return games.mapNotNull { game ->
            game.players
                .firstOrNull { it.userId == userID || it.userId.contains("guest") }
                ?.holes
                ?.sumOf { it.strokes }
        }.minOrNull()
    }

    fun worstGameStrokes(): Int? {
        return games.mapNotNull { game ->
            game.players
                .firstOrNull { it.userId == userID || it.userId.contains("guest") }
                ?.holes
                ?.sumOf { it.strokes }
        }.maxOrNull()
    }

    fun holeInOneCount(): Int {
        return games
            .flatMap { it.players }
            .filter { it.userId == userID || it.userId.contains("guest") }
            .flatMap { it.holes }
            .count { it.strokes == 1 }
    }

    // MARK: - Average Holes Maps

    fun averageHoles9(): List<Hole> {
        return averageHolesMap(9)
    }

    fun averageHoles18(): List<Hole> {
        return averageHolesMap(18)
    }

    private fun averageHolesMap(numberOfHoles: Int): List<Hole> {
        val holeStrokesDict = mutableMapOf<Int, MutableList<Int>>()

        for (game in games) {
            val player = game.players.firstOrNull { it.userId == userID || it.userId.contains("guest") }
            if (player != null) {
                for (hole in player.holes) {
                    if (hole.number <= numberOfHoles) {
                        holeStrokesDict.getOrPut(hole.number) { mutableListOf() }.add(hole.strokes)
                    }
                }
            }
        }

        val averageHoles = mutableListOf<Hole>()
        for (holeNumber in 1..numberOfHoles) {
            val strokesList = holeStrokesDict[holeNumber] ?: emptyList()
            val average = if (strokesList.isEmpty()) 0.0 else strokesList.sum().toDouble() / strokesList.size
            
            // Following the Swift behavior of rounding the average value.
            // Since Hole takes an Int for strokes, we round it.
            val roundedAverage = round(average).toInt()
            averageHoles.add(Hole(number = holeNumber, strokes = roundedAverage))
        }

        return averageHoles
    }

    fun latestGame(): Game? {
        return games.maxByOrNull { it.date.toInstant() }
    }

    fun winnerOfLatestGame(): Player? {
        return latestGame()?.players?.minByOrNull { it.totalStrokes }
    }

    fun usersScoreOfLatestGame(): Int {
        return latestGame()?.players?.firstOrNull { it.userId == userID || it.userId.contains("guest") }?.totalStrokes ?: 0
    }

    fun usersHolesOfLatestGame(): List<Hole> {
        return latestGame()?.players?.firstOrNull { it.userId == userID || it.userId.contains("guest") }?.holes ?: emptyList()
    }

    fun hasGames(): Boolean {
        return games.isNotEmpty()
    }
}
