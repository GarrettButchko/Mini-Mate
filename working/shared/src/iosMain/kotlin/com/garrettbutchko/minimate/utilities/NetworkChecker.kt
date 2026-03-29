package com.garrettbutchko.minimate.utilities

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_t
import platform.darwin.dispatch_queue_create

@OptIn(ExperimentalForeignApi::class)
actual class NetworkChecker private constructor() {

    actual var isConnected: Boolean = false
        private set

    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_queue_create("InternetConnectionMonitor", null)

    init {
        // Equivalent to pathUpdateHandler
        nw_path_monitor_set_update_handler(monitor) { path: nw_path_t? ->
            val status = nw_path_get_status(path)
            isConnected = (status == nw_path_status_satisfied)

            if (isConnected) {
                println("✅ Internet is available")
            } else {
                println("❌ No internet")
            }
        }

        // Start monitoring on the background queue
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
    }

    actual companion object {
        actual val shared = NetworkChecker()
    }
}