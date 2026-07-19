package com.mnmyounus.ypi

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mnmyounus.ypi.data.InstalledAppsPermissionReader
import com.mnmyounus.ypi.databinding.ActivityPermissionsInspectorBinding
import java.util.concurrent.Executors

/**
 * PermissionsInspectorActivity
 *
 * A sub-screen (reached from Insights), not a bottom-nav tab — this is an
 * occasional deep-dive, not something checked daily like the other 4
 * screens. Querying every installed app's permissions can be slow on a
 * device with 100+ apps, so it runs on a background executor and posts
 * back to the main thread, the same pattern SensorLogStore already uses.
 */
class PermissionsInspectorActivity : AppCompatActivity() {

    private lateinit var b: ActivityPermissionsInspectorBinding
    private val adapter = AppPermissionAdapter()
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPermissionsInspectorBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener { finish() }
        b.recyclerApps.layoutManager = LinearLayoutManager(this)
        b.recyclerApps.adapter = adapter

        loadApps()
    }

    private fun loadApps() {
        val reader = InstalledAppsPermissionReader(this)
        ioExecutor.execute {
            val apps = reader.allApps()
            mainHandler.post {
                if (isFinishing || isDestroyed) return@post
                adapter.submit(apps)
                b.loadingSpinner.visibility = View.GONE
                b.recyclerApps.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdownNow()
    }
}
