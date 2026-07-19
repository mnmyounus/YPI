package com.mnmyounus.ypi.data

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Process
import java.util.Calendar

/**
 * DataUsageReader
 *
 * Real per-app data usage for today, via NetworkStatsManager — the same
 * system API Android's own Settings → Data usage screen uses. Requires
 * Usage Access, the same permission YPI already asks for in Settings to
 * label log entries — nothing new to grant.
 *
 * querySummary() takes the older ConnectivityManager.TYPE_WIFI/TYPE_MOBILE
 * constants specifically (not NetworkCapabilities.TRANSPORT_*, which is a
 * different, newer constant system used elsewhere in this app for
 * NetworkCallback). That's not a typo — NetworkStatsManager's API surface
 * was never migrated off the older constants, so deprecation warnings on
 * these two are expected and safe to suppress.
 *
 * WiFi totals are reliable on every device. Mobile-data totals are queried
 * with a null subscriber ID, which works when the caller holds Usage
 * Access (no READ_PHONE_STATE needed) — but on some OEM/OS combinations
 * this can fail; if it does, this falls back to WiFi-only figures rather
 * than showing a wrong number or crashing. Every figure that IS shown is
 * genuine measured data, never an estimate.
 */
@Suppress("DEPRECATION")
class DataUsageReader(context: Context) {

    private val appContext = context.applicationContext
    private val statsManager =
        appContext.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    private val packageManager = appContext.packageManager

    data class AppUsage(
        val packageName: String,
        val appLabel: String,
        val wifiBytes: Long,
        val mobileBytes: Long
    ) {
        val totalBytes: Long get() = wifiBytes + mobileBytes
    }

    /** Per-app usage since local midnight today, sorted by total descending. */
    fun todayUsageByApp(): List<AppUsage> {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()

        val wifiByUid = queryByUid(ConnectivityManager.TYPE_WIFI, start, end)
        val mobileByUid = queryByUid(ConnectivityManager.TYPE_MOBILE, start, end)

        val uids = wifiByUid.keys + mobileByUid.keys
        return uids.mapNotNull { uid ->
            val pkg = packageManager.getPackagesForUid(uid)?.firstOrNull() ?: return@mapNotNull null
            val label = try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(info).toString()
            } catch (_: PackageManager.NameNotFoundException) { pkg }
            AppUsage(
                packageName = pkg,
                appLabel = label,
                wifiBytes = wifiByUid[uid] ?: 0L,
                mobileBytes = mobileByUid[uid] ?: 0L
            )
        }.sortedByDescending { it.totalBytes }
    }

    private fun queryByUid(networkType: Int, start: Long, end: Long): Map<Int, Long> {
        val result = mutableMapOf<Int, Long>()
        try {
            val bucket = NetworkStats.Bucket()
            val stats = statsManager.querySummary(networkType, null, start, end)
            stats.use {
                while (it.hasNextBucket()) {
                    it.getNextBucket(bucket)
                    if (bucket.uid == Process.SYSTEM_UID) continue   // skip the OS itself
                    val prev = result[bucket.uid] ?: 0L
                    result[bucket.uid] = prev + bucket.rxBytes + bucket.txBytes
                }
            }
        } catch (_: Exception) {
            // Usage Access not granted, or this transport unsupported on this
            // device/OS combination — return whatever succeeded elsewhere.
        }
        return result
    }
}
