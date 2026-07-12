package com.mnmyounus.ypi

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * BluetoothMonitor
 *
 * Watches for any classic Bluetooth link connecting or disconnecting
 * (earbuds, watch, car kit, etc.) via the system ACL broadcast — no
 * polling. Requires BLUETOOTH_CONNECT on Android 12+, a real runtime
 * permission with its own system prompt — unlike the other sensors'
 * Settings-based "special access" permissions. If it isn't granted,
 * start() simply no-ops and the Bluetooth badge never lights up.
 */
class BluetoothMonitor(
    context: Context,
    private val onStateChanged: (connected: Boolean) -> Unit
) {
    private val appContext = context.applicationContext
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED    -> onStateChanged(true)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> onStateChanged(false)
            }
        }
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return   // not granted — Bluetooth sensor simply stays inactive
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(receiver, filter)
        }
        registered = true
    }

    fun stop() {
        if (registered) {
            try { appContext.unregisterReceiver(receiver) } catch (_: Exception) { /* already unregistered */ }
            registered = false
        }
    }
}
