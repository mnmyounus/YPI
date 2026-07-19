package com.mnmyounus.ypi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mnmyounus.ypi.data.InstalledAppsPermissionReader
import com.mnmyounus.ypi.databinding.ItemAppPermissionBinding

/**
 * AppPermissionAdapter
 *
 * One row per installed app, with a badge per granted permission — reusing
 * the same emoji glyphs SensorType already uses elsewhere in YPI (📷 🎤 📍
 * 🎧), so "what can access my camera" reads consistently with "what's
 * using my camera right now." 🌐 for internet access isn't a SensorType
 * (YPI doesn't track "the internet" as a live sensor), so it's the one
 * badge defined locally here instead.
 */
class AppPermissionAdapter : RecyclerView.Adapter<AppPermissionAdapter.ViewHolder>() {

    private var items: List<InstalledAppsPermissionReader.AppPermissions> = emptyList()

    fun submit(newItems: List<InstalledAppsPermissionReader.AppPermissions>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppPermissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    class ViewHolder(private val b: ItemAppPermissionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(app: InstalledAppsPermissionReader.AppPermissions) {
            b.tvAppLabel.text = app.appLabel
            b.tvPermissionBadges.text = buildString {
                if (app.hasCamera) append("📷 ")
                if (app.hasMicrophone) append("🎤 ")
                if (app.hasLocation) append("📍 ")
                if (app.hasBluetooth) append("🎧 ")
                if (app.hasInternet) append("🌐 ")
            }.trim()
        }
    }
}
