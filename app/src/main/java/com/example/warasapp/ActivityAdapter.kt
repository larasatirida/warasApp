package com.example.warasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ActivityItem(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val source: String,
    val startTime: String,
    val endTime: String
)

class ActivityAdapter(
    private var items: List<ActivityItem>,
    private val onEditClick: (ActivityItem) -> Unit,
    private val onDeleteClick: (ActivityItem) -> Unit
) : RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvItemName)
        val tvDuration: TextView = view.findViewById(R.id.tvItemDuration)
        val tvSource: TextView = view.findViewById(R.id.tvItemSource)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditActivity)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteActivity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        
        val timeRange = if (item.startTime != "--:--" && item.endTime != "--:--") {
            "${item.startTime} - ${item.endTime} "
        } else ""
        
        holder.tvDuration.text = "$timeRange(${item.durationMinutes} mnt)"
        holder.tvSource.text = item.source

        holder.btnEdit.setOnClickListener { onEditClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<ActivityItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
