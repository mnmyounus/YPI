package com.mnmyounus.ypi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Handler
import android.os.Looper

/**
 * NetworkMonitor
 *
 * Detects two things independently:
 *  1. Current transport (WiFi vs Mobile Data) — event-driven via
 *     ConnectivityManager.NetworkCallback, zero polling.
 *  2. Whether data is actively flowing right now — Android has no system
 *     callback for "traffic is happening," so this samples the device's
 *     total RX+TX byte counters on a light interval and flags activity
 *     when the delta crosses a small threshold. Two integer reads every
 *     2 seconds; no per-app work happens here (see NetworkAppAttributor
 *     for that, which only runs on an activation transition, not on a loop).
 */
class NetworkMonitor(
    context: Context,
    private val onStateChanged: (active: Boolean, transport: Transport) -> Unit
) {
    enum class Transport { WIFI, MOBILE, NONE }

    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var transport: Transport = Transport.NONE
    private var isActive = false
    private var lastTotalBytes = -1L
    private var polling = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            transport = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> Transport.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.MOBILE
                else -> transport
            }
            dispatch()
        }
        override fun onLost(network: Network) {
            transport = Transport.NONE
            dispatch()
        }
    }

    private val pollTick = object : Runnable {
        override fun run() {
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            if (rx != TrafficStats.UNSUPPORTED.toLong() && tx != TrafficStats.UNSUPPORTED.toLong()) {
                val total = rx + tx
                if (lastTotalBytes >= 0L) {
                    val delta = total - lastTotalBytes
                    val nowActive = delta > ACTIVITY_THRESHOLD_BYTES
                    if (nowActive != isActive) { isActive = nowActive; dispatch() }
                }
                lastTotalBytes = total
            }
            if (polling) mainHandler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun start() {
        cm.registerDefaultNetworkCallback(networkCallback)
        polling = true
        lastTotalBytes = -1L
        mainHandler.postDelayed(pollTick, POLL_INTERVAL_MS)
    }

    fun stop() {
        polling = false
        mainHandler.removeCallbacks(pollTick)
        try { cm.unregisterNetworkCallback(networkCallback) } catch (_: Exception) { /* not registered */ }
    }

    private fun dispatch() = onStateChanged(isActive, transport)

    companion object {
        private const val POLL_INTERVAL_MS = 2_000L
        // ~6KB every 2s ≈ 24 kbps sustained — comfortably above idle background
        // chatter (DNS, keepalives), well below anything a user would notice as "using data"
        private const val ACTIVITY_THRESHOLD_BYTES = 6_000L
    }
}
