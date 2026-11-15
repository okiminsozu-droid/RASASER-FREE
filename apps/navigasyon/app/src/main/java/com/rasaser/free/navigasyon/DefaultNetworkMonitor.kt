package com.rasaser.free.navigasyon

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper

// VALIDATED is an Android network signal, not a guarantee that our server is reachable.
class DefaultNetworkMonitor(context: Context, private val listener: (Connection) -> Unit) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val handler = Handler(Looper.getMainLooper())
    private var registered = false

    private val cb = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = current()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            publish(classify(capabilities))
        }
        override fun onLost(network: Network) = current()
    }
    fun start() {
        if (registered) return
        try {
            cm.registerDefaultNetworkCallback(cb)
            registered = true
            current()
        } catch (_: SecurityException) {
            listener(Connection.OFFLINE)
        } catch (_: RuntimeException) {
            listener(Connection.OFFLINE)
        }
    }
    fun stop() {
        if (!registered) return
        registered = false
        try { cm.unregisterNetworkCallback(cb) } catch (_: IllegalArgumentException) {}
    }
    private fun current() = publish(classify(cm.getNetworkCapabilities(cm.activeNetwork)))
    private fun publish(c: Connection) { handler.post { if (registered) listener(c) } }
    private fun classify(c: NetworkCapabilities?): Connection {
        if (c == null ||
            !c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            !c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return Connection.OFFLINE
        return if (c.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
            Connection.UNMETERED else Connection.METERED
    }
}
