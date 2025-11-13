package com.tj.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.R
import com.tj.proyecto.Entidad.entMantenimiento
import java.text.SimpleDateFormat
import java.util.Locale

class MantenimientoAdapter(private val mantenimientos: List<entMantenimiento>) :
    RecyclerView.Adapter<MantenimientoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtFecha: TextView = view.findViewById(R.id.txtFecha)
        val txtDescripcion: TextView = view.findViewById(R.id.txtDescripcion)
        val txtCosto: TextView = view.findViewById(R.id.txtCosto)
        val txtProximoMantenimiento: TextView = view.findViewById(R.id.txtProximoMantenimiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mantenimiento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mantenimiento = mantenimientos[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Fecha de mantenimiento
        holder.txtFecha.text = sdf.format(mantenimiento.fechaMantenimiento)

        // Descripción
        holder.txtDescripcion.text = mantenimiento.descripcion

        // Costo
        holder.txtCosto.text = "S/ ${"%.2f".format(mantenimiento.costo)}"

        // Próximo mantenimiento
        if (mantenimiento.proximoMantenimiento != null) {
            holder.txtProximoMantenimiento.text = "Próximo: ${sdf.format(mantenimiento.proximoMantenimiento)}"
            holder.txtProximoMantenimiento.visibility = View.VISIBLE
        } else {
            holder.txtProximoMantenimiento.visibility = View.GONE
        }
    }

    override fun getItemCount() = mantenimientos.size
}