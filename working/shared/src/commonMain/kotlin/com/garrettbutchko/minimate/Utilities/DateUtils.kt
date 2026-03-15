package com.garrettbutchko.minimate.utilities

import kotlinx.datetime.*

object DateUtils {

    // ""2021-01-01" → "2020-W53" (Jan 1, 2021 is still in ISO week 53 of 2020)
    fun getIsoWeekID(dayID: String): String {
        val date = LocalDate.parse(dayID)
        
        // ISO 8601 week-based year and week number calculation
        // The first week of an ISO year is the week with the first Thursday.
        
        // Find the Thursday of the current week
        val dayOfWeek = date.dayOfWeek.isoDayNumber // 1 (Mon) to 7 (Sun)
        val thursday = date.plus(3 - (dayOfWeek - 1), DateTimeUnit.DAY)
        
        val year = thursday.year
        val firstDayOfYear = LocalDate(year, 1, 1)
        val firstThursday = if (firstDayOfYear.dayOfWeek.isoDayNumber <= 4) {
            firstDayOfYear.plus(4 - firstDayOfYear.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
        } else {
            firstDayOfYear.plus(11 - firstDayOfYear.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
        }
        
        val daysBetween = (thursday.toEpochDays() - firstThursday.toEpochDays())
        val week = (daysBetween / 7) + 1
        
        return "${year.toString().padStart(4, '0')}-W${week.toString().padStart(2, '0')}"
    }

    // Sunday = 1, Monday = 2, ... Saturday = 7
    fun getWeekday(dayID: String): Int {
        val date = LocalDate.parse(dayID)
        // kotlinx.datetime Monday is 1, Sunday is 7.
        // If we want Sunday = 1, Monday = 2, ..., Saturday = 7:
        val day = date.dayOfWeek.isoDayNumber // 1 (Mon) - 7 (Sun)
        return if (day == 7) 1 else day + 1
    }
}

// Extension to get ISO day number (1=Mon, 7=Sun)
private val DayOfWeek.isoDayNumber: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }
