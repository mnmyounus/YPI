package com.mnmyounus.ypi.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

/**
 * ForegroundAppResolver
 *
 * Android does not expose a public, unprivileged API that names the exact
 * process holding the camera, microphone, or a Bluetooth/mobile-data
 * connection. This resolver approximates it: whichever app was last
 * brought to the foreground right before a sensor activated is recorded
 * as the likely source. This is accurate the large majority of the time,
 * but a background service (e.g. a voice assistant) running behind a
 * different visible app can be misattributed to that visible app.
 *
 * (NETWORK over WiFi is the one exception with genuinely measured
 * attribution — see NetworkAppAttributor.)
 *
 * Requires PACKAGE_USAGE_STATS, a "special access" permission the user
 * grants manually via Settings → Apps → Special access → Usage access.
 * Without it, callers get null/"Unknown app" — every badge still works.
 */
class ForegroundAppResolver(context: Context) {

    private val appContext = context.applicationContext
    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = appContext.packageManager

    /** True once the user has granted Usage Access via Settings. */
    fun hasPermission(): Boolean {
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            appContext.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Best-effort package name in the foreground at [atTimeMillis], or null if unknown. */
    fun resolveForegroundPackage(atTimeMillis: Long): String? {
        if (!hasPermission()) return null
        return try {
            val windowStart = atTimeMillis - LOOKBACK_MS
            val events = usageStatsManager.queryEvents(windowStart, atTimeMillis + 1)
            val event  = UsageEvents.Event()
            var lastForeground: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastForeground = event.packageName
                }
            }
            lastForeground
        } catch (_: Exception) {
            null
        }
    }

    /** Human-readable app name for display, falling back gracefully. */
    fun labelFor(packageName: String?): String {
        if (packageName == null) return "Unknown app"
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    companion object {
        private const val LOOKBACK_MS = 10_000L   // look back up to 10s for a foreground event
    }
}
