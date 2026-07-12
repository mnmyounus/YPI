package com.mnmyounus.ypi

import android.content.Context
import com.mnmyounus.ypi.data.ForegroundAppResolver
import com.mnmyounus.ypi.data.NetworkAppAttributor
import com.mnmyounus.ypi.data.RetentionPolicy
import com.mnmyounus.ypi.data.SensorLogEntry
import com.mnmyounus.ypi.data.SensorLogStore
import com.mnmyounus.ypi.data.SensorType

/**
 * SensorActivityLogger
 *
 * Watches a Set<SensorType> snapshot (camera/mic/audio/network/bluetooth)
 * and records one completed entry per usage session — from the moment a
 * sensor turns on to the moment it turns off — to the encrypted log.
 *
 * Attribution: NETWORK sessions over WiFi get genuinely measured app
 * attribution via NetworkAppAttributor. Everything else (including
 * NETWORK over mobile data) falls back to the foreground-app heuristic,
 * since Android exposes no public per-process API for those.
 *
 * Must be called from the main thread.
 */
class SensorActivityLogger(context: Context) {

    private val appContext = context.applicationContext
    private val foregroundResolver = ForegroundAppResolver(appContext)
    private val networkAttributor  = NetworkAppAttributor(appContext)
    private val store = SensorLogStore.get(appContext)

    private data class OpenSession(
        val startMillis: Long,
        val packageName: String?,
        val appLabel: String,
        val transport: String?
    )

    private val open = mutableMapOf<SensorType, OpenSession>()
    private var previousActive: Set<SensorType> = emptySet()
    private var currentTransport: NetworkMonitor.Transport = NetworkMonitor.Transport.NONE

    /** Call whenever NetworkMonitor reports a transport change. */
    fun setNetworkTransport(transport: NetworkMonitor.Transport) {
        currentTransport = transport
    }

    fun onStateChanged(active: Set<SensorType>) {
        val now = System.currentTimeMillis()

        (active - previousActive).forEach { type -> open[type] = startSession(type, now) }
        (previousActive - active).forEach { type -> open.remove(type)?.let { finishSession(type, it, now) } }

        previousActive = active
    }

    private fun startSession(type: SensorType, startMillis: Long): OpenSession {
        val isWifiNetwork = type == SensorType.NETWORK && currentTransport == NetworkMonitor.Transport.WIFI
        val pkg = if (isWifiNetwork) {
            networkAttributor.resolveTopWifiConsumer(startMillis)
                ?: foregroundResolver.resolveForegroundPackage(startMillis)
        } else {
            foregroundResolver.resolveForegroundPackage(startMillis)
        }
        val transportTag = if (type == SensorType.NETWORK) currentTransport.name else null
        return OpenSession(startMillis, pkg, foregroundResolver.labelFor(pkg), transportTag)
    }

    private fun finishSession(type: SensorType, session: OpenSession, endMillis: Long) {
        val entry = SensorLogEntry(
            id          = session.startMillis * 10 + type.ordinal,
            sensor      = type.storageKey,
            packageName = session.packageName,
            appLabel    = session.appLabel,
            startMillis = session.startMillis,
            endMillis   = endMillis,
            transport   = session.transport
        )
        // Skip sub-second blips — not useful and just noise in the log
        if (entry.durationMillis >= MIN_LOGGABLE_DURATION_MS) {
            store.append(entry, RetentionPolicy.get(appContext).millis)
        }
    }

    companion object {
        private const val MIN_LOGGABLE_DURATION_MS = 500L
    }
}
