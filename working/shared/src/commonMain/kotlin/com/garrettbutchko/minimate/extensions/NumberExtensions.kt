package com.garrettbutchko.minimate.extensions

import kotlin.math.pow
import kotlin.math.round

fun Double.format(digits: Int): String {
    val multiplier = 10.0.pow(digits)
    val rounded = round(this * multiplier) / multiplier
    return if (digits == 0) rounded.toInt().toString() else rounded.toString()
}
