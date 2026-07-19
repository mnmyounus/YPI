package com.mnmyounus.ypi.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * IconOption
 *
 * Android has no API for a third-party app to swap its own launcher icon
 * for an arbitrary runtime image — only a fixed, pre-declared set of
 * <activity-alias> variants can be toggled on/off via PackageManager,
 * which is what actually changes the icon shown on the home screen and
 * app drawer. These 3 aliases are declared in AndroidManifest.xml;
 * exactly one is enabled at a time.
 */
enum class IconOption(val aliasSuffix: String, val displayLabel: String) {
    DEFAULT("Default", "Default"),
    STEALTH("Stealth", "Stealth (dark)"),
    MINIMAL("Minimal", "Minimal (mono)");

    fun componentName(context: Context): ComponentName =
        ComponentName(context, "${context.packageName}.MainActivity${aliasSuffix}Alias")

    companion object {
        private const val PREFS_NAME = "ypi_settings"
        private const val KEY_ICON = "icon_option"
        private val DEFAULT_OPTION = DEFAULT

        fun get(context: Context): IconOption {
            val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ICON, DEFAULT_OPTION.name)
            return values().firstOrNull { it.name == stored } ?: DEFAULT_OPTION
        }

        /** Enables [option]'s alias, disables the other two, and persists the choice. */
        fun apply(context: Context, option: IconOption) {
            val pm = context.packageManager
            values().forEach { candidate ->
                val state = if (candidate == option)
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                pm.setComponentEnabledSetting(
                    candidate.componentName(context), state, PackageManager.DONT_KILL_APP
                )
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ICON, option.name)
                .apply()
        }
    }
}
