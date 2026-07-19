package com.mnmyounus.ypi

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mnmyounus.ypi.data.ThemePreference
import com.mnmyounus.ypi.data.UsageCounters

/**
 * YpiApplication
 *
 * Two things need to happen before any Activity exists:
 *
 *  1. Apply the saved theme (light/dark/system) here rather than per-
 *     Activity — otherwise the first frame briefly flashes the wrong
 *     theme before an Activity's own onCreate() gets a chance to set it.
 *
 *  2. Register a PROCESS-level foreground observer to count genuine app
 *     opens. This deliberately does NOT use any single Activity's own
 *     onStart() — YPI's screens are separate Activities that finish()
 *     each other when you switch bottom-nav tabs, so an Activity-level
 *     onStart() would wrongly count every tab switch as a new "open."
 *     ProcessLifecycleOwner only fires once per genuine "app was fully
 *     backgrounded, now it's back."
 */
class YpiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemePreference.get(this).apply()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                UsageCounters.incrementAppOpen(applicationContext)
            }
        })
    }
}
