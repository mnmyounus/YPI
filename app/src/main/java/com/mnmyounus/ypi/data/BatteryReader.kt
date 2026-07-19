package com.mnmyounus.ypi.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * BatteryReader
 *
 * Overall device battery level and charging state only. Android has no
 * public API for third-party apps to read PER-APP battery consumption —
 * BatteryStatsManager.getBatteryUsageStats() exists, but it requires the
 * signature-level BATTERY_STATS permission, which the OS only grants to
 * pre-installed/platform-signed apps, never a sideloaded APK like this
 * one. This reads what's actually accessible: the device-wide numbers.
 */
class BatteryReader(context: Context) {

    private val appContext = context.applicationContext

    data class BatteryState(val percent: Int, val isCharging: Boolean)

    fun currentState(): BatteryState {
        // Peeking a sticky broadcast this way (null receiver) synchronously reads
        // the last-known battery Intent — no live registration needed.
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else -1

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryState(percent, charging)
    }
}
