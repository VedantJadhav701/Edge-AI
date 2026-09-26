package com.example.llama

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

data class CatalogModel(
    val id: String,
    val filename: String,
    val cleanName: String,
    val downloadUrl: String,
    val sizeStr: String,
    val quantBadge: String,
    val description: String
)

data class ModelItem(
    val file: File?,
    val catalogModel: CatalogModel?,
    val cleanName: String,
    val sizeStr: String,
    val quantBadge: String,
    val description: String,
    val isDownloaded: Boolean,
    val isActive: Boolean,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0
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

        if (item.isDownloading) {
            holder.subtitleTv.text = "Downloading: ${item.downloadProgress}%"
            holder.actionBtn.visibility = View.VISIBLE
            holder.actionBtn.text = "${item.downloadProgress}%"
            holder.checkmarkIv.visibility = View.GONE
        } else if (item.isDownloaded) {
            holder.subtitleTv.text = "${item.description} • ${item.sizeStr}"
            holder.actionBtn.visibility = View.GONE
            holder.checkmarkIv.visibility = if (item.isActive) View.VISIBLE else View.GONE
        } else {
            holder.subtitleTv.text = "${item.description} • ${item.sizeStr}"
            holder.actionBtn.visibility = View.VISIBLE
            holder.actionBtn.text = "Download"
            holder.checkmarkIv.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onModelSelected(item) }
    }

    override fun getItemCount(): Int = models.size

    class ModelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTv: TextView = view.findViewById(R.id.model_title)
        val badgeTv: TextView = view.findViewById(R.id.model_badge)
        val subtitleTv: TextView = view.findViewById(R.id.model_subtitle)
        val actionBtn: TextView = view.findViewById(R.id.model_action_btn)
        val checkmarkIv: ImageView = view.findViewById(R.id.model_checkmark)
    }
}
