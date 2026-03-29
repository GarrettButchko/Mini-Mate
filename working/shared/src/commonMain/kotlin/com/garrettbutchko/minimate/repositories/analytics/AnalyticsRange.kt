package com.garrettbutchko.minimate.repositories.analytics

import com.garrettbutchko.minimate.extensions.toLocalDate
import com.garrettbutchko.minimate.extensions.toTimestamp
import com.garrettbutchko.minimate.utilities.DateUtils
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

sealed class AnalyticsRange {
    object Last7 : AnalyticsRange()
    object Last30 : AnalyticsRange()
    object Last90 : AnalyticsRange()
    data class Custom(val start: LocalDate, val end: LocalDate) : AnalyticsRange()

    fun dates(now: LocalDate = Timestamp.now().toLocalDate()): RangeResult {
        return when (this) {
            Last7 -> {
                val start = now.minus(7, DateTimeUnit.DAY)
                RangeResult(start, now, now.minus(14, DateTimeUnit.DAY), start.minus(1, DateTimeUnit.DAY))
            }
            Last30 -> {
                val start = now.minus(30, DateTimeUnit.DAY)
                RangeResult(start, now, now.minus(60, DateTimeUnit.DAY), start.minus(1, DateTimeUnit.DAY))
            }
            Last90 -> {
                val start = now.minus(90, DateTimeUnit.DAY)
                RangeResult(start, now, now.minus(180, DateTimeUnit.DAY), start.minus(1, DateTimeUnit.DAY))
            }
            is Custom -> {
                val dayCount = (end.toEpochDays() - start.toEpochDays())
                val deltaEnd = start.minus(1, DateTimeUnit.DAY)
                val deltaStart = deltaEnd.minus(dayCount, DateTimeUnit.DAY)
                RangeResult(start, end, deltaStart, deltaEnd)
            }
        }
    }

    data class RangeResult(val start: LocalDate, val end: LocalDate, val dStart: LocalDate, val dEnd: LocalDate)

    val startDate: LocalDate get() = dates().start
    val endDate: LocalDate get() = dates().end

    val startDelta: LocalDate get() = dates().dStart
    val endDelta: LocalDate get() = dates().dEnd

    val title: String get() = when (this) {
        Last7 -> "Last 7 days"
        Last30 -> "Last 30 days"
        Last90 -> "Last 90 days"
        is Custom -> "Custom"
    }

    val isCustom: Boolean get() = this is Custom

    val daysBetween: Int get() = (endDate.toEpochDays() - startDate.toEpochDays()).toInt()

    val daysInMainRange: List<String>
        get() {
            val dates = this.dates()
            val days = mutableListOf<String>()
            var current = dates.start
            while (current <= dates.end) {
                days.add(DateUtils.makeDayID(current.toTimestamp()))
                current = current.plus(1, DateTimeUnit.DAY)
            }
            return days
        }

    val daysInDeltaRange: List<String>
        get() {
            val dates = this.dates()
            val days = mutableListOf<String>()
            var current = dates.dStart
            while (current <= dates.dEnd) {
                days.add(DateUtils.makeDayID(current.toTimestamp()))
                current = current.plus(1, DateTimeUnit.DAY)
            }
            return days
        }

}
