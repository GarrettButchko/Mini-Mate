package com.garrettbutchko.minimate.extensions

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

/**
 * Extension to convert kotlinx-datetime LocalDateTime to iOS NSDate.
 * This can be used in Swift as item.date.toNSDate().
 */
fun LocalDateTime.toNSDate(): NSDate {
    val instant = this.toInstant(TimeZone.currentSystemDefault())
    val seconds = instant.epochSeconds.toDouble()
    val nanoseconds = instant.nanosecondsOfSecond.toDouble()
    return NSDate.dateWithTimeIntervalSince1970(seconds + (nanoseconds / 1_000_000_000.0))
}

/**
 * Extension to convert iOS NSDate to kotlinx-datetime LocalDateTime.
 * This can be used in Swift as date.toLocalDateTime().
 */
fun NSDate.toLocalDateTime(): LocalDateTime {
    val timeInterval = this.timeIntervalSince1970
    val seconds = timeInterval.toLong()
    val nanoseconds = ((timeInterval - seconds) * 1_000_000_000).toInt()
    return Instant.fromEpochSeconds(seconds, nanoseconds).toLocalDateTime(TimeZone.currentSystemDefault())
}

/**
 * Extension to convert kotlinx-datetime LocalDate to iOS NSDate.
 * This can be used in Swift as item.date.toNSDate().
 */
fun LocalDate.toNSDate(): NSDate {
    val instant = this.atStartOfDayIn(TimeZone.currentSystemDefault())
    val seconds = instant.epochSeconds.toDouble()
    val nanoseconds = instant.nanosecondsOfSecond.toDouble()
    return NSDate.dateWithTimeIntervalSince1970(seconds + (nanoseconds / 1_000_000_000.0))
}

/**
 * Extension to convert iOS NSDate to kotlinx-datetime LocalDate.
 * This can be used in Swift as date.toLocalDate().
 */
fun NSDate.toLocalDate(): LocalDate {
    return this.toLocalDateTime().date
}
