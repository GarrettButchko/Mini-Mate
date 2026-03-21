package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.dataModels.holeModels.Hole
import com.garrettbutchko.minimate.repositories.CourseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

open class GameReviewViewModel(
    val game: Game,
    val courseRepository: CourseRepository = CourseRepository(),
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _course = MutableStateFlow<Course?>(null)
    val course: StateFlow<Course?> = _course.asStateFlow()

    fun loadCourse() {
        val id = game.courseID ?: return
        coroutineScope.launch {
            _course.value = courseRepository.fetchCourse(id)
        }
    }

    val holeCount: Int
        get() = if (_course.value?.customPar == true) {
            game.numberOfHoles
        } else {
            _course.value?.numHoles ?: 18
        }

    val shouldShowCustomAd: Boolean?
        get() = _course.value?.customAdActive

    val shareText: String
        get() {
            val instant = Instant.fromEpochSeconds(game.date.seconds, game.date.nanoseconds.toLong())
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            // Format to a string like "2025-12-06 14:30"
            val dateStr = "${dateTime.date} ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"

            val lines = mutableListOf(
                "MiniMate Scorecard",
                "Date: $dateStr",
                ""
            )

            for (player in game.players) {
                var holeLine = ""
                for (hole in player.holes) {
                    holeLine += "|${hole.strokes}"
                }
                lines.add("${player.name}: ${player.totalStrokes} strokes (${player.totalStrokes})")
                lines.add("Holes $holeLine")
            }

            lines.add("")
            // TODO add new link
            lines.add("Download MiniMate: [link here]")
            return lines.joinToString(separator = "\n")
        }

    fun timeString(from: Int): String {
        val minutes = from / 60
        val secs = from % 60
        return "$minutes:${secs.toString().padStart(2, '0')}"
    }

    fun averageStrokes(): List<Hole> {
        val holeCount = game.numberOfHoles
        val playerCount = game.players.size
        if (playerCount == 0) return emptyList()

        val sums = IntArray(holeCount)
        for (player in game.players) {
            for (hole in player.holes) {
                val idx = hole.number - 1
                if (idx in sums.indices) {
                    sums[idx] += hole.strokes
                }
            }
        }

        return sums.mapIndexed { idx, total ->
            val avg = total / playerCount
            Hole(number = idx + 1, strokes = avg)
        }
    }
}
