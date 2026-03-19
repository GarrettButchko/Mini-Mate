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
