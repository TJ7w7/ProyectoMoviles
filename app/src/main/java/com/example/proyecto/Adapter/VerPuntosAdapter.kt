package com.example.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.VerPuntosRuta

class VerPuntosAdapter(
    private val puntos: List<VerPuntosRuta.PuntoConOrden>
) : RecyclerView.Adapter<VerPuntosAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtOrden: TextView = itemView.findViewById(R.id.txtOrden)
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val txtDireccion: TextView = itemView.findViewById(R.id.txtDireccion)
        val txtZona: TextView = itemView.findViewById(R.id.txtZona)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto_ver, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val puntoConOrden = puntos[position]
        val punto = puntoConOrden.punto

        holder.txtOrden.text = "${puntoConOrden.orden}"
        holder.txtNombre.text = punto.nombre
        holder.txtDireccion.text = punto.direccion
        holder.txtZona.text = punto.zona
    }

    override fun getItemCount() = puntos.size
}