package com.tj.proyecto.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tj.proyecto.Entidad.entIncidencia
import com.tj.proyecto.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IncidenciasAdapter (
    private var lista: List<entIncidencia>,
    private val onVerDetallesClick: (entIncidencia) -> Unit
) : RecyclerView.Adapter<IncidenciasAdapter.ViewHolder>(){

    fun actualizarLista(nuevaLista: List<entIncidencia>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTipo: TextView = itemView.findViewById(R.id.txtTipo)
        val txtPuntoRuta: TextView = itemView.findViewById(R.id.txtPuntoRuta)
        val txtEstado: TextView = itemView.findViewById(R.id.txtEstado)
        val txtDescripcion: TextView = itemView.findViewById(R.id.txtDescripcion)
        val txtFechaReportado: TextView = itemView.findViewById(R.id.txtFechaReportado)
        val btnVerDetalles: MaterialButton = itemView.findViewById(R.id.btnVerDetalles)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_incidencia, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        val context = holder.itemView.context

        // 1. Motivo Principal
        holder.txtTipo.text = item.motivo

        // 2. Ubicación
        holder.txtPuntoRuta.text = "Punto ${item.orden}: ${item.puntoNombre}"

        // 3. Descripción (Si hay foto, lo mencionamos)
        val tieneFoto = if (item.fotoUrl.isNotEmpty()) "📸 Con evidencia fotográfica." else "Sin foto."
        holder.txtDescripcion.text = "El recolector reportó: ${item.motivo}. $tieneFoto"

        // 4. Estado y Color
        holder.txtEstado.text = item.estado
        when (item.estado) {
            "Pendiente" -> {
                holder.txtEstado.setBackgroundColor(Color.parseColor("#FF9800")) // Naranja
                holder.btnVerDetalles.text = "Resolver"
            }
            "Resuelta" -> {
                holder.txtEstado.setBackgroundColor(Color.parseColor("#4CAF50")) // Verde
                holder.btnVerDetalles.text = "Ver Detalles"
            }
            else -> holder.txtEstado.setBackgroundColor(Color.GRAY)
        }

        // 5. Fecha
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fecha = sdf.format(Date(item.fechaReporte))
        holder.txtFechaReportado.text = "📅 $fecha"

        // 6. Click para ir a solucionar
        holder.btnVerDetalles.setOnClickListener {
            onVerDetallesClick(item)
        }
    }

    override fun getItemCount() = lista.size

}