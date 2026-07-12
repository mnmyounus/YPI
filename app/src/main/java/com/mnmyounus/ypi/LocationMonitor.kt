package com.mnmyounus.ypi

import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * LocationMonitor
 *
 * Watches whether the GPS/GNSS radio is actively acquiring a satellite
 * fix, system-wide — regardless of which app requested it — via the
 * public LocationManager.registerGnssStatusCallback() API (API 24+).
 *
 * Important honesty note: this detects GPS-based positioning only.
 * Apps that use pure WiFi/cell-tower ("network") positioning without
 * ever engaging the GNSS chip will NOT trigger this badge. There is no
 * public Android API that observes network-based location requests
 * system-wide, so that gap is unavoidable on an offline, non-root app.
 *
 * Attribution: like Bluetooth, this API reports engine state only — no
 * package name — so SensorActivityLogger falls back to the foreground-
 * app heuristic for the log entry, same as Bluetooth and mobile data.
 *
 * Requires ACCESS_FINE_LOCATION, a real runtime permission with its own
 * system prompt — requested from MainActivity's Step 5. If it isn't
 * granted, start() simply no-ops and the Location badge never lights up.
 * YPI never calls requestLocationUpdates() and never reads a coordinate.
 */
class LocationMonitor(
    context: Context,
    private val onStateChanged: (active: Boolean) -> Unit
) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var registered = false

    // GNSS considered "active" once satellites are actually being reported;
    // a single lull between fixes shouldn't flap the badge on/off, so we
    // only report inactive after a short quiet period with no updates.
    private val quietTimeoutMs = 4_000L
    private val markInactive = Runnable { onStateChanged(false) }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            mainHandler.removeCallbacks(markInactive)
            onStateChanged(true)
            mainHandler.postDelayed(markInactive, quietTimeoutMs)
        }
    }

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return   // registerGnssStatusCallback needs API 24+
        val lm = locationManager ?: return

        if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return   // not granted — Location sensor simply stays inactive
        }

        try {
            lm.registerGnssStatusCallback(gnssCallback, mainHandler)
            registered = true
        } catch (_: SecurityException) {
            // Permission revoked between the check above and this call — stay inactive
        }
    }

    fun stop() {
        mainHandler.removeCallbacks(markInactive)
        if (registered) {
            try { locationManager?.unregisterGnssStatusCallback(gnssCallback) }
            catch (_: Exception) { /* already unregistered */ }
            registered = false
        }
    }
}
