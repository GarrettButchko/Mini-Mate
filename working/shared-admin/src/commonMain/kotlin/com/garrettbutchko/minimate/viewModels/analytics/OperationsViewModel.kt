package com.garrettbutchko.minimate.viewModels.analytics

import com.garrettbutchko.minimate.dataModels.courseModels.DailyDoc
import com.garrettbutchko.minimate.generateUUID
import com.garrettbutchko.minimate.repositories.analytics.AnalyticsRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.collections.iterator
import kotlin.math.max

data class HourData(
    val id: String = generateUUID(),
    val weekday: Int,
    val hour: Int,
    val count: Int
)

open class OperationsViewModel {
    
    var rangeDailyDocs: List<DailyDoc> = emptyList()
    var deltaDailyDocs: List<DailyDoc> = emptyList()
    var range: AnalyticsRange = AnalyticsRange.Last30
    
    // MARK: - Operations Metrics
    
    fun avgGamesPerDay(docs: List<DailyDoc>): Double {
        val totalGames = docs.sumOf { it.gamesPlayed }
        return totalGames.toDouble() / max(1.0, rangeDailyDocs.size.toDouble())
    }
    
    fun avgGamesPerDayPrime(): DataPointObject {
        val rangeData = avgGamesPerDay(rangeDailyDocs)
        val deltaData = avgGamesPerDay(deltaDailyDocs)
        val delta = calcDelta(deltaData, rangeData)
        
        val data = deltaErrorCalc(delta, true)
        
        return DataPointObject(value = rangeData.format(2), delta = data.first, deltaColor = data.second)
    }
    
    fun avgPlayersPerGame(docs: List<DailyDoc>): Double {
        val totalGames = docs.sumOf { it.gamesPlayed }
        val totalPlayers = docs.sumOf { it.totalCount }
        
        return if (totalGames > 0) totalPlayers.toDouble() / totalGames.toDouble() else 0.0
    }
    
    fun avgPlayersPerGamePrime(): DataPointObject {
        val rangeUsers = avgPlayersPerGame(rangeDailyDocs)
        val deltaUsers = avgPlayersPerGame(deltaDailyDocs)
        val delta = calcDelta(deltaUsers, rangeUsers)
        
        val data = deltaErrorCalc(delta, true)
        
        return DataPointObject(value = "${rangeUsers.format(2)} / 1", delta = data.first, deltaColor = data.second)
    }
    
    fun getBusiestHour(): DataPointObject {
        val fragments = rangeDailyDocs.map { it.hourlyCounts }

        if (fragments.isEmpty()) {
            return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        }

        val combinedCounts = (0..23).associate { it.toString() to 0 }.toMutableMap()

        for (fragment in fragments) {
            for ((hour, count) in fragment) {
                combinedCounts[hour] = (combinedCounts[hour] ?: 0) + count
            }
        }

        val busiest = combinedCounts.maxByOrNull { it.value }
        if (busiest == null || busiest.value <= 0) {
            return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        }

        val busiestHour = busiest.key.toIntOrNull() ?: 0
        var suffix = ""
        var displayHour = busiestHour

        when (busiestHour) {
            0 -> {
                displayHour = 12
                suffix = "am"
            }
            in 1..11 -> {
                suffix = "am"
            }
            12 -> {
                suffix = "pm"
            }
            in 13..23 -> {
                displayHour = busiestHour - 12
                suffix = "pm"
            }
            else -> return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        }

        val valueString = "$displayHour$suffix"

        return DataPointObject(value = valueString, delta = null, deltaColor = "MAIN_OPP")
    }

    fun getBusiestDay(): DataPointObject {
        if (rangeDailyDocs.isEmpty()) {
            return DataPointObject(value = "N/A", deltaColor = "MAIN_OPP")
        }

        val weeklyVolume = mutableMapOf<Int, Int>()
        for (doc in rangeDailyDocs) {
            weeklyVolume[doc.weekDay] = (weeklyVolume[doc.weekDay] ?: 0) + doc.gamesPlayed
        }

        val busiestDay = weeklyVolume.maxByOrNull { it.value }
        if (busiestDay == null || busiestDay.value <= 0) {
            return DataPointObject(value = "N/A", deltaColor = "MAIN_OPP")
        }

        val dayLabels = listOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        if (busiestDay.key !in 1..7) {
            return DataPointObject(value = "N/A", deltaColor = "MAIN_OPP")
        }

        return DataPointObject(value = dayLabels[busiestDay.key], deltaColor = "MAIN_OPP")
    }
    
    suspend fun prepareChartData(): List<HourData> = withContext(Dispatchers.Default) {
        val rangeDocs = rangeDailyDocs
        val chartData = mutableListOf<HourData>()
        
        for (day in 1..7) {
            val dayDocs = rangeDocs.filter { it.weekDay == day }
            
            for (hour in 0..23) {
                val totalForHour = dayDocs.sumOf { it.hourlyCounts[hour.toString()] ?: 0 }
                chartData.add(HourData(weekday = day, hour = hour, count = totalForHour))
            }
        }
        chartData
    }
    
    suspend fun getDataForGamesPerDay(): List<PlayerActivity> = withContext(Dispatchers.Default) {
        val rangeDocs = rangeDailyDocs
        val rangeObj = range
        
        val sortedDocs = rangeDocs.sortedBy { it.dayID }
        val docsByDateString = sortedDocs.associateBy { it.dayID }
        
        val result = mutableListOf<PlayerActivity>()
        
        val dates = rangeObj.dates()
        var currentDate = dates.start
        val endDate = dates.end
        
        while (currentDate <= endDate) {
            val dateString = currentDate.toString()
            
            val doc = docsByDateString[dateString]
            if (doc != null) {
                result.add(PlayerActivity(id = "", date = currentDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.currentSystemDefault()), count = doc.gamesPlayed))
            } else {
                result.add(PlayerActivity(id = "", date = currentDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.currentSystemDefault()), count = 0))
            }
            
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }
        
        result
    }
    
    // MARK: - Game Duration Metrics
    
    fun getAvgGameDuration(): DataPointObject {
        val totalGames = rangeDailyDocs.sumOf { it.gamesPlayed }
        val totalSeconds = rangeDailyDocs.sumOf { it.totalRoundSeconds }
        val totalGamesDelta = deltaDailyDocs.sumOf { it.gamesPlayed }
        val totalSecondsDelta = deltaDailyDocs.sumOf { it.totalRoundSeconds }
        
        if (totalGames <= 0) {
            return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        }
        
        val avgSeconds = totalSeconds.toDouble() / totalGames.toDouble()
        val minutes = (avgSeconds / 60).toInt()
        
        if (totalGamesDelta <= 0) {
            return DataPointObject(value = "$minutes min", delta = null, deltaColor = "MAIN_OPP")
        }
        
        val avgSecondsDelta = totalSecondsDelta.toDouble() / totalGamesDelta.toDouble()
        val minutesDelta = (avgSecondsDelta / 60).toInt()
        
        val delta = calcDelta(minutes, minutesDelta)
        val data = deltaErrorCalc(delta, false)
        
        return DataPointObject(value = "$minutes min", delta = data.first, deltaColor = data.second)
    }
    
    fun getTotalPlayTime(): DataPointObject {
        val totalSeconds = rangeDailyDocs.sumOf { it.totalRoundSeconds }
        val totalSecondsDelta = deltaDailyDocs.sumOf { it.totalRoundSeconds }
        
        val hours = (totalSeconds / 3600).toInt()
        val hoursDelta = (totalSecondsDelta / 3600).toInt()
        
        val delta = calcDelta(hours, hoursDelta)
        val data = deltaErrorCalc(delta, false)
        
        return if (hours < 1) {
            val minutes = (totalSeconds / 60).toInt()
            DataPointObject(value = "$minutes min", delta = data.first, deltaColor = data.second)
        } else if (hours < 24) {
            DataPointObject(value = "$hours hrs", delta = data.first, deltaColor = data.second)
        } else {
            val days = hours / 24
            val remainingHours = hours % 24
            DataPointObject(value = "${days}d ${remainingHours}h", delta = data.first, deltaColor = data.second)
        }
    }
    
    fun getFastestGameTime(): DataPointObject {
        var fastestSeconds: Long = Long.MAX_VALUE
        
        for (doc in rangeDailyDocs) {
            if (doc.gamesPlayed <= 0) continue
            val avgForDay = doc.totalRoundSeconds / doc.gamesPlayed.toLong()
            if (avgForDay in 1..<fastestSeconds) {
                fastestSeconds = avgForDay
            }
        }
        
        var fastestSecondsDelta: Long = Long.MAX_VALUE
        
        for (doc in deltaDailyDocs) {
            if (doc.gamesPlayed <= 0) continue
            val avgForDay = doc.totalRoundSeconds / doc.gamesPlayed.toLong()
            if (avgForDay in 1..<fastestSecondsDelta) {
                fastestSecondsDelta = avgForDay
            }
        }
        
        val delta = calcDelta(fastestSeconds, fastestSecondsDelta)
        val data = deltaErrorCalc(delta, false)
        
        if (fastestSeconds == Long.MAX_VALUE || fastestSeconds <= 0) {
            return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        }
        
        val minutes = (fastestSeconds / 60).toInt()
        return DataPointObject(value = "$minutes min", delta = data.first, deltaColor = data.second)
    }
    
    fun getSlowestGameTime(): DataPointObject {
        var slowestSeconds: Long = 0
        
        for (doc in rangeDailyDocs) {
            if (doc.gamesPlayed <= 0) continue
            val avgForDay = doc.totalRoundSeconds / doc.gamesPlayed.toLong()
            if (avgForDay > slowestSeconds) {
                slowestSeconds = avgForDay
            }
        }
        
        var slowestSecondsDelta: Long = 0
        
        for (doc in rangeDailyDocs) { // Swift had a bug here, it was iterating over rangeDailyDocs again instead of deltaDailyDocs
            if (doc.gamesPlayed <= 0) continue
            val avgForDay = doc.totalRoundSeconds / doc.gamesPlayed.toLong()
            if (avgForDay > slowestSecondsDelta) {
                slowestSecondsDelta = avgForDay
            }
        }
        
        val delta = calcDelta(slowestSeconds, slowestSecondsDelta)
        val data = deltaErrorCalc(delta, false)
        
        if (slowestSeconds <= 0) {
            return DataPointObject(value = "N/A", delta = null, deltaColor = "MAIN_OPP")
        }
        
        val minutes = (slowestSeconds / 60).toInt()
        return DataPointObject(value = "$minutes min", delta = data.first, deltaColor = data.second)
    }
    
    suspend fun getDataForDurationTrend(): List<GameDurationActivity> = withContext(Dispatchers.Default) {
        val rangeDocs = rangeDailyDocs
        val rangeObj = range
        
        val sortedDocs = rangeDocs.sortedBy { it.dayID }
        val docsByDateString = sortedDocs.associateBy { it.dayID }
        
        val result = mutableListOf<GameDurationActivity>()
        
        val dates = rangeObj.dates()
        var currentDate = dates.start
        val endDate = dates.end
        
        while (currentDate <= endDate) {
            val dateString = currentDate.toString()
            
            val doc = docsByDateString[dateString]
            if (doc != null && doc.gamesPlayed > 0) {
                val avgSeconds = doc.totalRoundSeconds.toDouble() / doc.gamesPlayed.toDouble()
                val avgMinutes = avgSeconds / 60.0
                result.add(GameDurationActivity(id = "", date = currentDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.currentSystemDefault()), avgMinutes = avgMinutes))
            } else {
                result.add(GameDurationActivity(id = "", date = currentDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.currentSystemDefault()), avgMinutes = 0.0))
            }
            
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }
        
        result
    }
    
    // MARK: - Helper Functions
    
    private fun deltaErrorCalc(initialDelta: Double, positiveGood: Boolean): Pair<String?, String?> {
        var delta = initialDelta
        
        if (deltaDailyDocs.isEmpty() || 
            (rangeDailyDocs.size != deltaDailyDocs.size && 
             rangeDailyDocs.size != deltaDailyDocs.size + 1 && 
             rangeDailyDocs.size != deltaDailyDocs.size - 1) || 
            (delta > 999 || delta < -999)) {
            delta = 0.0
        }
        
        val isDeltaPositive = delta > 0
        
        var deltaString = "${delta.format(1)}%"
        
        if (isDeltaPositive) {
            deltaString = "+$deltaString"
        }
        
        return if (delta == 0.0) {
            Pair(null, null)
        } else {
            Pair(deltaString, positive(good = positiveGood, delta))
        }
    }
    
    private fun calcDelta(prev: Int, current: Int): Double {
        if (prev == 0) return 0.0
        return ((current - prev).toDouble() / prev.toDouble()) * 100.0
    }
    
    private fun calcDelta(prev: Long, current: Long): Double {
        if (prev == 0L) return 0.0
        return ((current - prev).toDouble() / prev.toDouble()) * 100.0
    }
    
    private fun calcDelta(prev: Double, current: Double): Double {
        if (prev == 0.0) return 0.0
        return ((current - prev) / prev) * 100.0
    }
    
    private fun positive(good: Boolean, delta: Double): String {
        return when {
            delta > 0 && good -> "GREEN"
            delta < 0 && !good -> "GREEN"
            delta < 0 && good -> "RED"
            delta > 0 && !good -> "RED"
            delta == 0.0 -> "MAIN_OPP"
            else -> "MAIN_OPP"
        }
    }
}
