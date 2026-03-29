package com.garrettbutchko.minimate.utilities

expect class NetworkChecker {
    var isConnected: Boolean
        private set

    companion object {
        val shared: NetworkChecker
    }
}