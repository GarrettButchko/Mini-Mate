package com.garrettbutchko.minimate.viewModels.analytics

import com.garrettbutchko.minimate.dataModels.courseModels.DailyDoc
import com.garrettbutchko.minimate.extensions.format
import com.garrettbutchko.minimate.repositories.analytics.AnalyticsRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone

open class GrowthViewModel {
    
    var rangeDailyDocs: List<DailyDoc> = emptyList()
    var deltaDailyDocs: List<DailyDoc> = emptyList()
    var range: AnalyticsRange = AnalyticsRange.Last30
    var growthChartTopic: ChartTopic = ChartTopic.TOTAL
    
    // MARK: - Growth Metrics
    
    fun getActiveUsers(docs: List<DailyDoc>): Int {
        return docs.sumOf { it.totalCount }
    }
    
    fun getFirstTimeUsers(docs: List<DailyDoc>): Int {
        return docs.sumOf { it.newPlayers }
    }
    
    fun firstTimePercOfTotal(): Double {
        val value = getFirstTimeUsers(rangeDailyDocs)
        if (value <= 0) return 0.0
        return value.toDouble() / getActiveUsers(rangeDailyDocs).toDouble()
    }
    
    fun getReturningUsers(docs: List<DailyDoc>): Int {
        return docs.sumOf { it.returningPlayers }
    }
    
    fun returningPercOfTotal(): Double {
        val value = getReturningUsers(rangeDailyDocs)
        if (value <= 0) return 0.0
        return value.toDouble() / getActiveUsers(rangeDailyDocs).toDouble()
    }
    
    fun avgPlayersPerGame(docs: List<DailyDoc>): Double {
        val totalGames = docs.sumOf { it.gamesPlayed }
        val players = getActiveUsers(docs)
        return if (players > 0) players.toDouble() / totalGames.toDouble() else 0.0
    }
    
    // MARK: - Prime (Data Points with Deltas)
    
    fun activeUsersPrime(): DataPointObject {
        val rangeUsers = getActiveUsers(rangeDailyDocs)
        val deltaUsers = getActiveUsers(deltaDailyDocs)
        val delta = calcDelta(deltaUsers, rangeUsers)
        
        val data = deltaErrorCalc(delta, true)
        
        return DataPointObject(value = rangeUsers.toString(), delta = data.first, deltaColor = data.second)
    }
    
    fun firstTimePrime(): DataPointObject {
        val rangeUsers = getFirstTimeUsers(rangeDailyDocs)
        val deltaUsers = getFirstTimeUsers(deltaDailyDocs)
        val delta = calcDelta(deltaUsers, rangeUsers)
        
        val data = deltaErrorCalc(delta, true)
        
        return DataPointObject(value = rangeUsers.toString(), delta = data.first, deltaColor = data.second)
    }
    
    fun returningPrime(): DataPointObject {
        val rangeUsers = getReturningUsers(rangeDailyDocs)
        val deltaUsers = getReturningUsers(deltaDailyDocs)
        val delta = calcDelta(deltaUsers, rangeUsers)
        
        val data = deltaErrorCalc(delta, true)
        
        return DataPointObject(value = rangeUsers.toString(), delta = data.first, deltaColor = data.second)
    }
    
    fun avgPlayersPerGamePrime(): DataPointObject {
        val rangeUsers = avgPlayersPerGame(rangeDailyDocs)
        val deltaUsers = avgPlayersPerGame(deltaDailyDocs)
        val delta = calcDelta(deltaUsers, rangeUsers)
        
        val data = deltaErrorCalc(delta, true)
        
        return DataPointObject(value = "${rangeUsers.format(2)} / 1", delta = data.first, deltaColor = data.second)
    }
    
    // MARK: - Chart Data
    
    suspend fun getDataForGrowthTrend(): List<PlayerActivity> = withContext(Dispatchers.Default) {
        val rangeDocs = rangeDailyDocs
        val rangeObj = range
        val chartTopic = growthChartTopic
        
        val sortedDocs = rangeDocs.sortedBy { it.dayID }
        val docsByDateString = sortedDocs.associateBy { it.dayID }
        
        val result = mutableListOf<PlayerActivity>()
        
        val dates = rangeObj.dates()
        var currentDate = dates.start
        val endDate = dates.end
        
        while (currentDate <= endDate) {
            val dateString = currentDate.toString()
            
            val count = if (docsByDateString.containsKey(dateString)) {
                val doc = docsByDateString[dateString]!!
                when (chartTopic) {
                    ChartTopic.TOTAL -> doc.totalCount
                    ChartTopic.FIRST -> doc.newPlayers
                    ChartTopic.RETURNING -> doc.returningPlayers
                }
            } else {
                0
            }
            
            result.add(PlayerActivity(id = "", date = currentDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.currentSystemDefault()), count = count))
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
