package com.mnmyounus.ypi.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * ThemePreference
 *
 * Light / Dark / System, persisted in SharedPreferences and applied via
 * AppCompatDelegate.setDefaultNightMode(). Applied in YpiApplication's
 * onCreate() — before any Activity exists — so the correct theme is
 * already active on the very first frame, no flash of the wrong theme.
 */
enum class ThemePreference(val storageValue: Int, val displayLabel: String, val nightMode: Int) {
    LIGHT(0, "Light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK(1, "Dark", AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM(2, "System default", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    fun apply() {
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    companion object {
        private const val PREFS_NAME = "ypi_settings"
        private const val KEY_THEME = "theme_mode"
        private val DEFAULT = SYSTEM

        fun get(context: Context): ThemePreference {
            val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_THEME, DEFAULT.storageValue)
            return values().firstOrNull { it.storageValue == stored } ?: DEFAULT
        }

        fun set(context: Context, pref: ThemePreference) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_THEME, pref.storageValue)
                .apply()
        }
    }
}
