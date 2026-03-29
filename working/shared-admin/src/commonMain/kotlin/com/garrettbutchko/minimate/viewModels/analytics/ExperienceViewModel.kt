package com.garrettbutchko.minimate.viewModels.analytics

import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.dataModels.courseModels.DailyDoc
import com.garrettbutchko.minimate.generateUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.iterator
import kotlin.math.round

open class ExperienceViewModel {
    var rangeDailyDocs: List<DailyDoc> = emptyList()
    var currentCourse: Course? = null

    // MARK: - Experience Metrics

    fun getHardestHole(): DataPointObject {
        val avgStrokesPerHole = getHoleCombined()
        val hardestHoleID = avgStrokesPerHole.maxByOrNull { it.value }?.key ?: "N/A"
        val valueStr = if (hardestHoleID != "N/A") "Hole $hardestHoleID" else "N/A"
        return DataPointObject(value = valueStr, delta = null, deltaColor = "MAIN_OPP")
    }

    fun getEasiestHole(): DataPointObject {
        val avgStrokesPerHole = getHoleCombined()
        val easiestHoleID = avgStrokesPerHole.minByOrNull { it.value }?.key ?: "N/A"
        val valueStr = if (easiestHoleID != "N/A") "Hole $easiestHoleID" else "N/A"
        return DataPointObject(value = valueStr, delta = null, deltaColor = "MAIN_OPP")
    }

    fun getHoleCombined(): Map<String, Double> {
        val combinedTotalStrokes = mutableMapOf<String, Int>()
        val combinedPlays = mutableMapOf<String, Int>()

        for (doc in rangeDailyDocs) {
            for ((holeID, strokes) in doc.holeAnalytics.totalStrokesPerHole) {
                combinedTotalStrokes[holeID] = (combinedTotalStrokes[holeID] ?: 0) + strokes
            }
            for ((holeID, plays) in doc.holeAnalytics.playsPerHole) {
                combinedPlays[holeID] = (combinedPlays[holeID] ?: 0) + plays
            }
        }

        return combinedTotalStrokes.mapValues { (key, value) ->
            value.toDouble() / (combinedPlays[key] ?: 1).toDouble()
        }
    }

    suspend fun getHoleDifficultyData(): List<HoleDifficultyData> = withContext(Dispatchers.Default) {
        val results = getHoleCombined()
        results.mapNotNull { (key, value) ->
            val holeNum = key.toIntOrNull() ?: return@mapNotNull null
            HoleDifficultyData(generateUUID(), holeNum, value)
        }.sortedBy { it.holeNumber }
    }

    suspend fun getHoleHeatmapForParData(course: Course): List<HoleHeatmapData> = withContext(Dispatchers.Default) {
        val combinedResults = getHoleCombined()
        combinedResults.mapNotNull { (key, avgStrokes) ->
            val holeNum = key.toIntOrNull() ?: return@mapNotNull null
            val index = holeNum - 1
            if (index < 0 || index >= course.pars.size) return@mapNotNull null

            val par = course.pars[index].toDouble()
            val offset = avgStrokes - par

            HoleHeatmapData(
                id = generateUUID(),
                holeNumber = holeNum,
                relativeToPar = offset,
                holePar = course.pars[index]
            )
        }.sortedBy { it.holeNumber }
    }

    fun getAvgRelativeToPar(): DataPointObject {
        val course = currentCourse ?: return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        val holeCombined = getHoleCombined()
        var totalOffset = 0.0
        var validHoleCount = 0

        for ((holeID, avgStrokes) in holeCombined) {
            val holeNum = holeID.toIntOrNull()
            if (holeNum == null || holeNum <= 0 || holeNum > course.pars.size) continue

            val par = course.pars[holeNum - 1].toDouble()
            totalOffset += (avgStrokes - par)
            validHoleCount += 1
        }

        if (validHoleCount <= 0) return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")

        val avgOffset = totalOffset / validHoleCount.toDouble()
        val sign = if (avgOffset > 0) "+" else ""
        // Round to 2 decimals
        val roundedOffset = round(avgOffset * 100) / 100.0
        val valueString = "$sign$roundedOffset"

        return DataPointObject(value = valueString, delta = null, deltaColor = if (avgOffset <= 0) "GREEN" else "RED")
    }

    fun getMostBeatenPar(): DataPointObject {
        val course = currentCourse ?: return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        val holeCombined = getHoleCombined()
        var bestHole = 0
        var bestOffset = Double.MAX_VALUE

        for ((holeID, avgStrokes) in holeCombined) {
            val holeNum = holeID.toIntOrNull()
            if (holeNum == null || holeNum <= 0 || holeNum > course.pars.size) continue

            val par = course.pars[holeNum - 1].toDouble()
            val offset = avgStrokes - par

            if (offset < bestOffset) {
                bestOffset = offset
                bestHole = holeNum
            }
        }

        if (bestHole <= 0) return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        return DataPointObject(value = "Hole $bestHole", delta = null, deltaColor = "MAIN_OPP")
    }

    fun getUnderParPercentage(): DataPointObject {
        val course = currentCourse ?: return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        var totalHolesPlayed = 0
        var underParCount = 0

        for (doc in rangeDailyDocs) {
            for ((holeID, totalStrokes) in doc.holeAnalytics.totalStrokesPerHole) {
                val holeNum = holeID.toIntOrNull()
                val plays = doc.holeAnalytics.playsPerHole[holeID]
                if (holeNum == null || holeNum <= 0 || holeNum > course.pars.size || plays == null || plays <= 0) continue

                val par = course.pars[holeNum - 1].toDouble()
                val avgStrokes = totalStrokes.toDouble() / plays.toDouble()

                if (avgStrokes < par) {
                    underParCount += plays
                }
                totalHolesPlayed += plays
            }
        }

        if (totalHolesPlayed <= 0) return DataPointObject(value = "0%", delta = null, deltaColor = "MAIN_OPP")

        val percentage = (underParCount.toDouble() / totalHolesPlayed.toDouble()) * 100.0
        val formatted = if (percentage == 0.0) "0%" else "${percentage.format(1)}%"
        return DataPointObject(value = formatted, delta = null, deltaColor = "MAIN_OPP")
    }

    fun getOverParPercentage(): DataPointObject {
        val course = currentCourse ?: return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        var totalHolesPlayed = 0
        var overParCount = 0

        for (doc in rangeDailyDocs) {
            for ((holeID, totalStrokes) in doc.holeAnalytics.totalStrokesPerHole) {
                val holeNum = holeID.toIntOrNull()
                val plays = doc.holeAnalytics.playsPerHole[holeID]
                if (holeNum == null || holeNum <= 0 || holeNum > course.pars.size || plays == null || plays <= 0) continue

                val par = course.pars[holeNum - 1].toDouble()
                val avgStrokes = totalStrokes.toDouble() / plays.toDouble()

                if (avgStrokes > par) {
                    overParCount += plays
                }
                totalHolesPlayed += plays
            }
        }

        if (totalHolesPlayed <= 0) return DataPointObject(value = "0%", delta = null, deltaColor = "MAIN_OPP")

        val percentage = (overParCount.toDouble() / totalHolesPlayed.toDouble()) * 100.0
        val formatted = if (percentage == 0.0) "0%" else "${percentage.format(1)}%"
        return DataPointObject(value = formatted, delta = null, deltaColor = "MAIN_OPP")
    }

    fun getHoleInOneCount(): DataPointObject {
        var holeInOneCount = 0

        for (doc in rangeDailyDocs) {
            for ((holeID, totalStrokes) in doc.holeAnalytics.totalStrokesPerHole) {
                val plays = doc.holeAnalytics.playsPerHole[holeID] ?: continue
                if (plays > 0 && totalStrokes == plays) {
                    holeInOneCount += plays
                }
            }
        }

        return DataPointObject(value = holeInOneCount.toString(), delta = null, deltaColor = "YELLOW")
    }
}
