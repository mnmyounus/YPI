package com.mnmyounus.ypi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.mnmyounus.ypi.data.UsageCounters

/**
 * ScreenLockMonitor
 *
 * Counts screen-off (treated as "locked") and genuine unlock events, via
 * two protected system broadcasts that can only be observed by registering
 * dynamically at runtime — they can't be declared in the manifest on API
 * 26+. Mirrors BluetoothMonitor's registration pattern exactly.
 *
 * ACTION_SCREEN_OFF fires whenever the screen turns off — the standard,
 * honest proxy for "locked," even on devices with no PIN/pattern set.
 * ACTION_USER_PRESENT fires only when the keyguard is actually dismissed,
 * which is the correct "unlocked" signal, not merely "screen turned on."
 */
class ScreenLockMonitor(context: Context) {

    private val appContext = context.applicationContext
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF   -> UsageCounters.incrementLock(appContext)
                Intent.ACTION_USER_PRESENT -> UsageCounters.incrementUnlock(appContext)
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
        registered = true
    }

    fun stop() {
        if (!registered) return
        try { appContext.unregisterReceiver(receiver) } catch (_: Exception) { /* already unregistered */ }
        registered = false
    }
}
