package com.tj.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.R
import com.tj.proyecto.Entidad.entPuntoRecoleccion

class PuntosAsignadosAdapter(
    private val puntos: List<entPuntoRecoleccion>,
    private val onSubir: (Int) -> Unit,
    private val onBajar: (Int) -> Unit,
    private val onQuitar: (Int) -> Unit)
    : RecyclerView.Adapter<PuntosAsignadosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtOrden: TextView = view.findViewById(R.id.txtOrden)
        val txtNombrePunto: TextView = view.findViewById(R.id.txtNombrePunto)
        val txtDireccionPunto: TextView = view.findViewById(R.id.txtDireccionPunto)
        val btnSubir: ImageButton = view.findViewById(R.id.btnSubir)
        val btnBajar: ImageButton = view.findViewById(R.id.btnBajar)
        val btnQuitar: ImageButton = view.findViewById(R.id.btnQuitar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto_asignado, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val punto = puntos[position]

        holder.txtOrden.text = (position + 1).toString()
        holder.txtNombrePunto.text = punto.nombre
        holder.txtDireccionPunto.text = punto.direccion

        // Deshabilitar botón subir si es el primero
        holder.btnSubir.isEnabled = position > 0
        holder.btnSubir.alpha = if (position > 0) 1.0f else 0.3f

        // Deshabilitar botón bajar si es el último
        holder.btnBajar.isEnabled = position < puntos.size - 1
        holder.btnBajar.alpha = if (position < puntos.size - 1) 1.0f else 0.3f

        holder.btnSubir.setOnClickListener {
            onSubir(position)
        }

        holder.btnBajar.setOnClickListener {
            onBajar(position)
        }

        holder.btnQuitar.setOnClickListener {
            onQuitar(position)
        }
    }

    override fun getItemCount() = puntos.size
}