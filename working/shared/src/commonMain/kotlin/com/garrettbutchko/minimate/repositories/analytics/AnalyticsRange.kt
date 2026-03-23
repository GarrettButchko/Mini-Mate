package com.garrettbutchko.minimate.repositories.analytics

import com.garrettbutchko.minimate.extensions.toLocalDate
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

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
}
