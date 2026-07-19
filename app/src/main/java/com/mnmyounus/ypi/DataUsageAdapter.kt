package com.mnmyounus.ypi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mnmyounus.ypi.data.DataUsageReader
import com.mnmyounus.ypi.databinding.ItemDataUsageAppBinding
import java.util.Locale

/**
 * DataUsageAdapter
 *
 * Renders today's per-app data usage: app name, total, and a WiFi/Mobile
 * split caption. WiFi figures are always genuine measured data; a mobile
 * figure of exactly 0 can mean "really zero" or "couldn't be read on this
 * device" — DataUsageReader already logs which case applies internally,
 * this just displays whatever numbers it successfully got.
 */
class DataUsageAdapter : RecyclerView.Adapter<DataUsageAdapter.ViewHolder>() {

    private var items: List<DataUsageReader.AppUsage> = emptyList()

    fun submit(newItems: List<DataUsageReader.AppUsage>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDataUsageAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    class ViewHolder(private val b: ItemDataUsageAppBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(usage: DataUsageReader.AppUsage) {
            b.tvAppLabel.text = usage.appLabel
            b.tvTotal.text = formatBytes(usage.totalBytes)
            b.tvBreakdown.text = "${formatBytes(usage.wifiBytes)} WiFi · ${formatBytes(usage.mobileBytes)} Mobile"
        }

        private fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
