package com.example.llama

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

data class ModelItem(
    val file: File,
    val cleanName: String,
    val sizeStr: String,
    val quantBadge: String,
    val description: String,
    val isActive: Boolean
)

class ModelPickerAdapter(
    private val models: List<ModelItem>,
    private val onModelSelected: (ModelItem) -> Unit
) : RecyclerView.Adapter<ModelPickerAdapter.ModelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_model_picker_row, parent, false)
        return ModelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        val item = models[position]
        holder.titleTv.text = item.cleanName
        holder.badgeTv.text = item.quantBadge
        holder.subtitleTv.text = "${item.description} • ${item.sizeStr}"
        holder.checkmarkIv.visibility = if (item.isActive) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onModelSelected(item) }
    }

    override fun getItemCount(): Int = models.size

    class ModelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTv: TextView = view.findViewById(R.id.model_title)
        val badgeTv: TextView = view.findViewById(R.id.model_badge)
        val subtitleTv: TextView = view.findViewById(R.id.model_subtitle)
        val checkmarkIv: ImageView = view.findViewById(R.id.model_checkmark)
    }
}
