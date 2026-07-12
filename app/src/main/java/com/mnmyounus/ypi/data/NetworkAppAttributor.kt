package com.mnmyounus.ypi.data

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Process

/**
 * NetworkAppAttributor
 *
 * Unlike camera/mic/audio/Bluetooth, WiFi usage has a real, public,
 * per-app accounting API: NetworkStatsManager. This gives a genuinely
 * measured answer — not a foreground-app guess — at the cost of requiring
 * Usage Access (the same optional permission already used for the other
 * sensors) and a short scan over the recent usage buckets.
 *
 * Mobile data is intentionally NOT covered here: precise per-app cellular
 * querying needs a device subscriber ID, which means adding READ_PHONE_STATE
 * — a much heavier permission ask for one extra bit of accuracy. Mobile
 * data sessions fall back to the same foreground-app heuristic as the
 * other sensors (see SensorActivityLogger).
 */
class NetworkAppAttributor(context: Context) {

    private val appContext = context.applicationContext
    private val resolver = ForegroundAppResolver(appContext)
    private val packageManager = appContext.packageManager
    private val statsManager =
        appContext.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    /** Best-effort package using the most WiFi data in the last [windowMillis], or null. */
    fun resolveTopWifiConsumer(atTimeMillis: Long, windowMillis: Long = WINDOW_MS): String? {
        if (!resolver.hasPermission()) return null

        var bestUid = -1
        var bestBytes = 0L

        try {
            val stats = statsManager.querySummary(
                ConnectivityManager.TYPE_WIFI, null, atTimeMillis - windowMillis, atTimeMillis
            )
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val total = bucket.rxBytes + bucket.txBytes
                if (total > bestBytes && bucket.uid >= Process.FIRST_APPLICATION_UID) {
                    bestBytes = total
                    bestUid = bucket.uid
                }
            }
            stats.close()
        } catch (_: Exception) {
            return null
        }

        if (bestUid < 0 || bestBytes < MIN_ATTRIBUTABLE_BYTES) return null
        return packageManager.getPackagesForUid(bestUid)?.firstOrNull()
    }

    companion object {
        private const val WINDOW_MS = 15_000L
        private const val MIN_ATTRIBUTABLE_BYTES = 4_000L
    }
}
