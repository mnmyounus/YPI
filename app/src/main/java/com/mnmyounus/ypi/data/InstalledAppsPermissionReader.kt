package com.mnmyounus.ypi.data

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * InstalledAppsPermissionReader
 *
 * A static audit — which installed apps CAN access camera/mic/location/
 * Bluetooth/internet — complementing the real-time badges elsewhere in
 * YPI, which show who's using a sensor right now. Uses PackageManager's
 * ordinary, unprivileged permission-inspection APIs; no special access
 * needed beyond seeing that the app exists at all (see the <queries>
 * block in AndroidManifest.xml, scoped to launcher apps only).
 *
 * Only apps with a launcher icon are shown — the device has dozens of
 * invisible system service packages that would just be noise here.
 */
class InstalledAppsPermissionReader(context: Context) {

    private val appContext = context.applicationContext
    private val pm = appContext.packageManager

    data class AppPermissions(
        val packageName: String,
        val appLabel: String,
        val hasCamera: Boolean,
        val hasMicrophone: Boolean,
        val hasLocation: Boolean,
        val hasBluetooth: Boolean,
        val hasInternet: Boolean
    ) {
        val grantedCount: Int
            get() = listOf(hasCamera, hasMicrophone, hasLocation, hasBluetooth, hasInternet).count { it }
    }

    /** Runs real PackageManager queries — call this off the main thread. */
    fun allApps(): List<AppPermissions> {
        val packages = try {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (_: Exception) {
            emptyList()
        }

        return packages.mapNotNull { pkgInfo ->
            if (pkgInfo.packageName == appContext.packageName) return@mapNotNull null   // skip YPI itself

            val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val hasLauncherIcon = pm.getLaunchIntentForPackage(pkgInfo.packageName) != null
            if (isSystemApp && !hasLauncherIcon) return@mapNotNull null   // hide invisible system components

            val granted = grantedPermissionSet(pkgInfo)
            AppPermissions(
                packageName = pkgInfo.packageName,
                appLabel = pm.getApplicationLabel(appInfo).toString(),
                hasCamera = Manifest.permission.CAMERA in granted,
                hasMicrophone = Manifest.permission.RECORD_AUDIO in granted,
                hasLocation = Manifest.permission.ACCESS_FINE_LOCATION in granted ||
                    Manifest.permission.ACCESS_COARSE_LOCATION in granted,
                hasBluetooth = bluetoothGranted(granted),
                hasInternet = Manifest.permission.INTERNET in granted
            )
        }.sortedByDescending { it.grantedCount }
    }

    private fun grantedPermissionSet(pkgInfo: PackageInfo): Set<String> {
        val names = pkgInfo.requestedPermissions ?: return emptySet()
        val flags = pkgInfo.requestedPermissionsFlags ?: return emptySet()
        val result = mutableSetOf<String>()
        names.forEachIndexed { i, name ->
            if (i < flags.size && (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                result.add(name)
            }
        }
        return result
    }

    private fun bluetoothGranted(granted: Set<String>): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT in granted
        } else {
            true   // pre-31 devices don't gate basic Bluetooth behind a runtime permission
        }
}
