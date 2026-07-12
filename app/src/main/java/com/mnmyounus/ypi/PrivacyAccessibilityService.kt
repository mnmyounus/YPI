package com.mnmyounus.ypi

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.mnmyounus.ypi.data.RetentionPolicy
import com.mnmyounus.ypi.data.SensorLogStore
import com.mnmyounus.ypi.data.SensorType

/**
 * PrivacyAccessibilityService
 *
 * Core service responsibilities:
 *  1. Attach a TYPE_ACCESSIBILITY_OVERLAY window — a fixed-width row wide
 *     enough for all 6 sensor badges — that renders above every other
 *     window, including full-screen games and system dialogs.
 *  2. Start SensorMonitor (camera/mic/audio), NetworkMonitor,
 *     BluetoothMonitor, and LocationMonitor, and combine their readings
 *     into one Set<SensorType>.
 *  3. Route every change to both SensorIndicatorView (what's drawn) and
 *     SensorActivityLogger (what gets written to the encrypted log).
 *
 * This service receives accessibility events on app/window switches only
 * (see accessibility_service_config.xml) and discards them immediately —
 * it is purely an overlay host and sensor bridge, not a screen reader.
 *
 * The static `isRunning` flag lets MainActivity reflect live status
 * without any IPC overhead.
 */
class PrivacyAccessibilityService : AccessibilityService() {

    private var windowManager:     WindowManager?         = null
    private var indicatorView:     SensorIndicatorView?   = null
    private var sensorMonitor:     SensorMonitor?          = null
    private var networkMonitor:    NetworkMonitor?         = null
    private var bluetoothMonitor:  BluetoothMonitor?       = null
    private var locationMonitor:   LocationMonitor?        = null
    private var activityLogger:    SensorActivityLogger?  = null

    // Latest reading from each independent source — combined in publish()
    private var camera = false
    private var mic = false
    private var audio = false
    private var networkActive = false
    private var bluetoothActive = false
    private var locationActive = false

    companion object {
        @Volatile
        var isRunning = false
            private set

        // Overlay geometry — adjust to reposition the badge row
        private const val MARGIN_X_DP = 12   // from right screen edge
        private const val MARGIN_Y_DP =  5   // from top  screen edge
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    override fun onServiceConnected() {
        isRunning = true
        createOverlay()

        activityLogger = SensorActivityLogger(this)
        SensorLogStore.get(this).pruneNow(RetentionPolicy.get(this).millis)

        sensorMonitor = SensorMonitor(this) { c, m, a ->
            camera = c; mic = m; audio = a
            publish()
        }.also { it.start() }

        networkMonitor = NetworkMonitor(this) { active, transport ->
            networkActive = active
            indicatorView?.setNetworkTransport(transport)
            activityLogger?.setNetworkTransport(transport)
            publish()
        }.also { it.start() }

        bluetoothMonitor = BluetoothMonitor(this) { connected ->
            bluetoothActive = connected
            publish()
        }.also { it.start() }

        locationMonitor = LocationMonitor(this) { active ->
            locationActive = active
            publish()
        }.also { it.start() }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isRunning = false
        tearDown()
        return false   // do not request re-bind
    }

    override fun onDestroy() {
        isRunning = false
        tearDown()
        super.onDestroy()
    }

    // ── Overlay ───────────────────────────────────────────────────

    private fun createOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val widthPx  = dpToPx(SensorIndicatorView.SLOT_WIDTH_DP * SensorType.values().size)
        val heightPx = dpToPx(SensorIndicatorView.BADGE_DIAMETER_DP + 4)

        val params = WindowManager.LayoutParams(
            widthPx, heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // NOT_FOCUSABLE  → keyboard / back-button events pass through
            // NOT_TOUCHABLE  → no accidental touch interception, even across empty slots
            // LAYOUT_IN_SCREEN → coordinates include the status bar area
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).also { lp ->
            lp.gravity = Gravity.TOP or Gravity.END
            lp.x       = dpToPx(MARGIN_X_DP)
            lp.y       = dpToPx(MARGIN_Y_DP)
        }

        indicatorView = SensorIndicatorView(this).also { wm.addView(it, params) }
    }

    // ── Combine all 6 sources ───────────────────────────────────────

    private fun publish() {
        val active = buildSet {
            if (camera) add(SensorType.CAMERA)
            if (mic) add(SensorType.MIC)
            if (audio) add(SensorType.AUDIO)
            if (networkActive) add(SensorType.NETWORK)
            if (bluetoothActive) add(SensorType.BLUETOOTH)
            if (locationActive) add(SensorType.LOCATION)
        }
        indicatorView?.updateState(active)
        activityLogger?.onStateChanged(active)
    }

    // ── Cleanup ───────────────────────────────────────────────────

    private fun tearDown() {
        sensorMonitor?.stop();    sensorMonitor = null
        networkMonitor?.stop();   networkMonitor = null
        bluetoothMonitor?.stop(); bluetoothMonitor = null
        locationMonitor?.stop();  locationMonitor = null
        activityLogger = null

        indicatorView?.let { v ->
            try { windowManager?.removeView(v) }
            catch (_: IllegalArgumentException) { /* view already detached */ }
        }
        indicatorView = null
        windowManager  = null
    }

    // ── Required no-op overrides ──────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt()                                     = Unit

    // ── Utility ───────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()
}
