package com.tj.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.R
import com.squareup.picasso.Picasso

class FotosEvidenciaAdapter (
    private val fotos: List<String>,
    private val onFotoClick: (String) -> Unit
) : RecyclerView.Adapter<FotosEvidenciaAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFoto: ImageView = itemView.findViewById(R.id.imgFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_foto_evidencia, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fotoUrl = fotos[position]

        Picasso.get()
            .load(fotoUrl)
            .centerCrop()
            .fit()
            .into(holder.imgFoto)

        holder.imgFoto.setOnClickListener {
            onFotoClick(fotoUrl)
        }
    }

    override fun getItemCount() = fotos.size
}