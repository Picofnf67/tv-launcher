package ru.example.tvlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    private var items: List<AppEntry> = emptyList()

    fun submit(list: List<AppEntry>) {
        items = list
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val label: TextView = view.findViewById(R.id.app_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = items[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.itemView.setOnClickListener { onClick(app) }
        holder.itemView.setOnLongClickListener {
            onLongClick(app)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}
