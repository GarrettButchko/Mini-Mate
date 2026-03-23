package com.garrettbutchko.minimate.extensions

private val emailRegEx = Regex("[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

val String.isValidEmail: Boolean
    get() = emailRegEx.matches(this)
