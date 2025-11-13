package com.tj.proyecto.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.R
import com.tj.proyecto.Entidad.entAsignacionRuta
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class MisAsignacionesAdapter(
    private val asignaciones: List<entAsignacionRuta>,
    private val onClick: (entAsignacionRuta) -> Unit
) : RecyclerView.Adapter<MisAsignacionesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardAsignacion: MaterialCardView = itemView.findViewById(R.id.cardAsignacion)
        val txtRutaNombre: TextView = itemView.findViewById(R.id.txtRutaNombre)
        val txtRutaCodigo: TextView = itemView.findViewById(R.id.txtRutaCodigo)
        val txtEstado: TextView = itemView.findViewById(R.id.txtEstado)
        val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        val txtVehiculo: TextView = itemView.findViewById(R.id.txtVehiculo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mi_asignacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val asignacion = asignaciones[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        holder.txtRutaNombre.text = asignacion.rutaNombre
        holder.txtRutaCodigo.text = asignacion.rutaCodigo
        holder.txtFecha.text = "📅 ${sdf.format(asignacion.fechaAsignacion)}"
        holder.txtVehiculo.text = "🚛 ${asignacion.vehiculoPlaca}"

        // Estado con colores
        holder.txtEstado.text = asignacion.estado
        when (asignacion.estado) {
            "Programada" -> {
                holder.txtEstado.setBackgroundColor(
                    Color.parseColor("#2196F3")
                )
            }
            "En Progreso" -> {
                holder.txtEstado.setBackgroundColor(
                    Color.parseColor("#FF9800")
                )
            }
            "Completada" -> {
                holder.txtEstado.setBackgroundColor(
                    Color.parseColor("#4CAF50")
                )
            }
            "Cancelada" -> {
                holder.txtEstado.setBackgroundColor(
                    Color.parseColor("#F44336")
                )
            }
        }

        holder.cardAsignacion.setOnClickListener {
            onClick(asignacion)
        }
    }

    override fun getItemCount() = asignaciones.size
}