package com.garrettbutchko.minimate.repositories.analytics

import kotlinx.datetime.DayOfWeek

val DayOfWeek.isoDayNumber: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }

fun ClosedRange<Double>.random(): Double = (endInclusive - start) * kotlin.random.Random.nextDouble() + start
fun IntRange.random(): Int = kotlin.random.Random.nextInt(start, endInclusive + 1)
