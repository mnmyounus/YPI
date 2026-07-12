package com.mnmyounus.ypi.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.mnmyounus.ypi.PrivacyAccessibilityService

/**
 * PermissionStatus
 *
 * Centralizes every permission/setting check YPI's onboarding depends on,
 * so Home's compact status banner and Settings' full step cards can never
 * silently drift out of agreement about what's actually granted.
 *
 * Usage Access isn't duplicated here — ForegroundAppResolver.hasPermission()
 * already owns that check cleanly and is used directly wherever needed.
 */
object PermissionStatus {

    fun overlayGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * Reads the colon-separated list from Settings.Secure to confirm our
     * specific service component is enabled — the only reliable approach
     * across all API levels and OEM skins.
     */
    fun accessibilityServiceEnabled(context: Context): Boolean {
        val component = "${context.packageName}/${PrivacyAccessibilityService::class.java.canonicalName}"
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').also { it.setString(raw) }
        while (splitter.hasNext()) {
            if (splitter.next().equals(component, ignoreCase = true)) return true
        }
        return false
    }

    fun bluetoothGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED

    fun locationGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Only overlay + accessibility service are required for badges to render at all. */
    fun coreReady(context: Context): Boolean =
        overlayGranted(context) && accessibilityServiceEnabled(context)
}
