package com.garrettbutchko.minimate.utilities

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

actual class NetworkChecker private constructor(context: Context) {

    actual var isConnected: Boolean = false
        private set

    init {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Listen for internet-capable networks
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isConnected = true
                Log.d("NetworkChecker", "✅ Internet is available")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isConnected = false
                Log.d("NetworkChecker", "❌ No internet")
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                // Optionally double-check if the connection is actually validated (has real internet access)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                  networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                isConnected = hasInternet
            }
        })

        // Check the initial state synchronously
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    actual companion object {
        @Volatile
        private var instance: NetworkChecker? = null

        fun getInstance(context: Context): NetworkChecker {
            return instance ?: synchronized(this) {
                instance ?: NetworkChecker(context.applicationContext).also { instance = it }
            }
        }
        
        // Mock fallback for common platform requirement 
        // Note: For Android 'shared' isn't as trivial due to Context requirement, 
        // you should have previously called getInstance(context)
        actual val shared: NetworkChecker
            get() = instance ?: error("NetworkChecker must be initialized via getInstance(context) first on Android.")
    }
}
