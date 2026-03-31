package com.garrettbutchko.minimate.viewModels.analytics

import com.garrettbutchko.minimate.dataModels.courseModels.CourseEmail
import com.garrettbutchko.minimate.extensions.format
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.collections.iterator
import kotlin.time.Clock

open class RetentionViewModel {
    
    var allEmails: Map<String, CourseEmail> = emptyMap()
    
    // Cached tier data - computed once and reused
    var cachedNewPlayers: Map<String, CourseEmail> = emptyMap()
    var cachedMidTierPlayers: Map<String, CourseEmail> = emptyMap()
    var cachedFrequentPlayers: Map<String, CourseEmail> = emptyMap()
    var cachedAtRiskPlayers: Map<String, CourseEmail> = emptyMap()
    
    // Cached metrics
    var cachedAvgTimeToReturn: Int = 0
    var cached30DayRetention: Double = 0.0
    
    // MARK: - Retention Tier Filtering
    
    fun getNewPlayers(): Map<String, CourseEmail> {
        return allEmails.filterValues { it.playCount == 1 }
    }
    
    fun getMidTierPlayers(): Map<String, CourseEmail> {
        val days = avgTimeToReturn().let { if (it > 0) it else 30 }
        return allEmails.filterValues { data ->
            val isInPlayRange = data.playCount in 2..5
            val isActive = isRecentlyActive(data.lastPlayed, days)
            isInPlayRange && isActive
        }
    }
    
    fun getFrequentPlayers(): Map<String, CourseEmail> {
        val days = avgTimeToReturn().let { if (it > 0) it else 30 }
        return allEmails.filterValues { data ->
            val isHighPlayCount = data.playCount > 5
            val isActive = isRecentlyActive(data.lastPlayed, days)
            isHighPlayCount && isActive
        }
    }
    
    fun getAtRiskPlayers(): Map<String, CourseEmail> {
        val days = avgTimeToReturn().let { if (it > 0) it else 30 }
        return allEmails.filterValues { data ->
            val hasPlayedBefore = data.playCount > 1
            val isInactive = !isRecentlyActive(data.lastPlayed, days)
            hasPlayedBefore && isInactive
        }
    }
    
    fun isRecentlyActive(lastPlayedString: String?, days: Int): Boolean {
        if (lastPlayedString == null) return false
        
        val lastPlayedDate = try {
            LocalDate.parse(lastPlayedString)
        } catch (e: Exception) {
            return false
        }
        
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val cutoffDate = today.minus(days, DateTimeUnit.DAY)
        
        return lastPlayedDate >= cutoffDate
    }
    
    // MARK: - Retention Metrics
    
    fun avgTimeToReturn(): Int {
        val returningPlayers = allEmails.filterValues { it.secondSeen != null }
        
        if (returningPlayers.isEmpty()) return 0
        
        var totalDays = 0L
        var count = 0
        
        for ((_, data) in returningPlayers) {
            val firstSeenStr = data.firstSeen ?: continue
            val secondSeenStr = data.secondSeen ?: continue
            
            val firstSeenDate = try { LocalDate.parse(firstSeenStr) } catch (e: Exception) { continue }
            val secondSeenDate = try { LocalDate.parse(secondSeenStr) } catch (e: Exception) { continue }
            
            val daysBetween = (secondSeenDate.toEpochDays() - firstSeenDate.toEpochDays())
            totalDays += daysBetween
            count += 1
        }
        
        return (if (count > 0) totalDays / count else 0).toInt()
    }
    
    fun getAvgTimeToReturn(): DataPointObject {
        val avgDays = avgTimeToReturn()
        return DataPointObject(
            value = avgDays.toString(),
            delta = null,
            deltaColor = null
        )
    }
    
    fun calculate30DayRetention(): Double {
        val totalPlayers = allEmails.size
        if (totalPlayers <= 0) return 0.0
        
        var retainedWithin30 = 0
        
        for ((_, data) in allEmails) {
            val firstStr = data.firstSeen ?: continue
            val secondStr = data.secondSeen ?: continue
            
            val firstDate = try { LocalDate.parse(firstStr) } catch (e: Exception) { continue }
            val secondDate = try { LocalDate.parse(secondStr) } catch (e: Exception) { continue }
            
            val daysBetween = (secondDate.toEpochDays() - firstDate.toEpochDays())
            
            if (daysBetween <= 30) {
                retainedWithin30 += 1
            }
        }
        
        return retainedWithin30.toDouble() / totalPlayers.toDouble()
    }
    
    fun get30DayRetention(): DataPointObject {
        val retentionPercentage = calculate30DayRetention() * 100.0
        return DataPointObject(
            value = "${retentionPercentage.format(0)}%",
            delta = null,
            deltaColor = null
        )
    }
    
    fun recomputePlayerTiers() {
        val avgDays = avgTimeToReturn()
        cachedAvgTimeToReturn = avgDays
        cached30DayRetention = calculate30DayRetention()
        
        val activeThreshold = if (avgDays > 0) avgDays else 30
        
        cachedNewPlayers = allEmails.filterValues { it.playCount == 1 }
        
        cachedMidTierPlayers = allEmails.filterValues { data ->
            val isInPlayRange = data.playCount in 2..5
            val isActive = isRecentlyActive(data.lastPlayed, activeThreshold)
            isInPlayRange && isActive
        }
        
        cachedFrequentPlayers = allEmails.filterValues { data ->
            val isHighPlayCount = data.playCount > 5
            val isActive = isRecentlyActive(data.lastPlayed, activeThreshold)
            isHighPlayCount && isActive
        }
        
        cachedAtRiskPlayers = allEmails.filterValues { data ->
            val hasPlayedBefore = data.playCount > 1
            val isInactive = !isRecentlyActive(data.lastPlayed, activeThreshold)
            hasPlayedBefore && isInactive
        }
    }
    
    // MARK: - CSV Generation
    
    fun generateCSVContent(emails: List<String>): String {
        val sb = StringBuilder()
        sb.append("Email\n")
        
        for (email in emails) {
            sb.append(email).append("\n")
        }
        
        return sb.toString()
    }
}
