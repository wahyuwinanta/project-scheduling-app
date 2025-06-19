package com.cvmmk

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageAdapter(
    private val imageUris: MutableList<Uri>,
    private val isEditable: Boolean = false,
    private val onRemoveClick: (Uri) -> Unit = {}
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.iv_image)
        val btnRemove: ImageView = itemView.findViewById(R.id.btn_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = imageUris[position]
        Glide.with(holder.itemView.context)
            .load(uri)
            .centerCrop()
            .placeholder(R.drawable.ic_project_placeholder)
            .error(R.drawable.ic_project_placeholder)
            .into(holder.imageView)

        holder.btnRemove.visibility = if (isEditable) View.VISIBLE else View.GONE
        holder.btnRemove.setOnClickListener {
            if (isEditable) onRemoveClick(uri)
        }
    }

    override fun getItemCount(): Int = imageUris.size
}