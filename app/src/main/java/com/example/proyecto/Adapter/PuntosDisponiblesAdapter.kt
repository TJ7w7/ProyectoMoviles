package com.example.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.material.button.MaterialButton

class PuntosDisponiblesAdapter (private val puntos: List<entPuntoRecoleccion>,
                                private val onAgregarClick: (entPuntoRecoleccion) -> Unit):
    RecyclerView.Adapter<PuntosDisponiblesAdapter.ViewHolder>(){

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombrePunto: TextView = view.findViewById(R.id.txtNombrePunto)
        val txtDireccionPunto: TextView = view.findViewById(R.id.txtDireccionPunto)
        val txtZonaPunto: TextView = view.findViewById(R.id.txtZonaPunto)
        val btnAgregar: MaterialButton = view.findViewById(R.id.btnAgregar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto_disponible, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val punto = puntos[position]

        holder.txtNombrePunto.text = punto.nombre
        holder.txtDireccionPunto.text = punto.direccion
        holder.txtZonaPunto.text = "Zona: ${punto.zona}"

        holder.btnAgregar.setOnClickListener {
            onAgregarClick(punto)
        }
    }

    override fun getItemCount() = puntos.size
}