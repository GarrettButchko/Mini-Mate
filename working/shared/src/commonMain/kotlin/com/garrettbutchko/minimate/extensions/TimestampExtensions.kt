package com.garrettbutchko.minimate.extensions

import dev.gitlive.firebase.firestore.Timestamp
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.atStartOfDayIn


fun Timestamp.toInstant(): Instant {
    return Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
}

fun Instant.toTimestamp(): Timestamp {
    return Timestamp(epochSeconds, nanosecondsOfSecond)
}

fun Timestamp.toLocalDateTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
    toInstant().toLocalDateTime(timeZone)

fun Timestamp.toLocalDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    toLocalDateTime(timeZone).date

fun LocalDateTime.toTimestamp(timeZone: TimeZone = TimeZone.currentSystemDefault()): Timestamp =
    toInstant(timeZone).toTimestamp()

fun LocalDate.toTimestamp(timeZone: TimeZone = TimeZone.currentSystemDefault()): Timestamp =
    atStartOfDayIn(timeZone).toTimestamp()

/**
 * Formats the timestamp to a string equivalent to Swift's 
 * .formatted(date: .abbreviated, time: .shortened)
 * Example: "Oct 24, 2023, 2:30 PM"
 */

fun Timestamp.formatted(): String {
    val dateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())
    val monthStr = dateTime.month.name.lowercase()
        .replaceFirstChar { it.uppercase() }
        .take(3)
    val day = dateTime.day
    val year = dateTime.year
    val hour = dateTime.hour
    val minute = dateTime.minute.toString().padStart(2, '0')
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    
    return "$monthStr $day, $year, $hour12:$minute $amPm"
}
