package com.example.warasapp

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActivityAdapter(
    private var activityList: MutableList<ActivityItem>,
    private val onEditClick: (ActivityItem) -> Unit,
    private val onDeleteClick: (ActivityItem) -> Unit
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCategoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val tvName: TextView = view.findViewById(R.id.tvActivityName)
        val tvTimeDuration: TextView = view.findViewById(R.id.tvTimeDuration)
        val tvWeightBadge: TextView = view.findViewById(R.id.tvWeightBadge)
        val btnOptions: View = view.findViewById(R.id.btnOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity_card, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val item = activityList[position]

        holder.tvName.text = item.name
        holder.tvTimeDuration.text = "${item.startTime} - ${item.endTime} (${formatDuration(item.durationMinutes)})"

        // Set Ikon Vektor dengan aman menggunakan pemetaan string Firestore
        val resId = getDrawableResId(holder.itemView.context, item.iconName)
        holder.ivCategoryIcon.setImageResource(resId)

        // Set Teks dan Warna Badge Tingkat Beban (Weight)
        holder.tvWeightBadge.text = item.weight
        when (item.weight.lowercase()) {
            "ringan" -> {
                holder.tvWeightBadge.setTextColor(Color.parseColor("#2CD97B"))
                holder.tvWeightBadge.setBackgroundColor(Color.parseColor("#E8F9EE"))
            }
            "sedang" -> {
                holder.tvWeightBadge.setTextColor(Color.parseColor("#D99B00"))
                holder.tvWeightBadge.setBackgroundColor(Color.parseColor("#FFF9E6"))
            }
            "berat" -> {
                holder.tvWeightBadge.setTextColor(Color.parseColor("#F87171"))
                holder.tvWeightBadge.setBackgroundColor(Color.parseColor("#FEF2F2"))
            }
        }

        // Tombol opsi (titik tiga) untuk Edit / Hapus
        holder.btnOptions.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            popup.menu.add("Edit")
            popup.menu.add("Hapus")
            popup.setOnMenuItemClickListener { menu ->
                if (menu.title == "Edit") onEditClick(item)
                else if (menu.title == "Hapus") onDeleteClick(item)
                true
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = activityList.size

    fun updateData(newList: List<ActivityItem>) {
        activityList.clear()
        activityList.addAll(newList)
        notifyDataSetChanged()
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0 && mins > 0) "${hours}j ${mins}m"
        else if (hours > 0) "${hours}j"
        else "${mins}m"
    }

    private fun getDrawableResId(context: Context, iconName: String?): Int {
        return when (iconName?.lowercase()) {
            "ic_mission" -> R.drawable.ic_mission
            "ic_profile" -> R.drawable.ic_profile
            "ic_check" -> R.drawable.ic_check
            "ic_warning" -> R.drawable.ic_warning
            "ic_sleepy" -> R.drawable.ic_sleepy
            "projek", "pekerjaan", "ic_jadwal" -> R.drawable.ic_jadwal
            else -> R.drawable.ic_jadwal // Fallback aman agar tidak pernah kosong/kotak hitam
        }
    }
}