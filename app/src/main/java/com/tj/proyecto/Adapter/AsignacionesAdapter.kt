package com.tj.proyecto.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Entidad.entAsignacionRuta
import com.google.android.material.button.MaterialButton
import com.tj.proyecto.R
import java.text.SimpleDateFormat
import java.util.Locale

class AsignacionesAdapter(
    private val asignaciones: List<entAsignacionRuta>,
    private val onVerDetalle: (entAsignacionRuta) -> Unit,
    private val onEditar: (entAsignacionRuta) -> Unit,
    private val onCambiarEstado: (entAsignacionRuta) -> Unit,
    private val onEliminar: (entAsignacionRuta) -> Unit
) : RecyclerView.Adapter<AsignacionesAdapter.AsignacionViewHolder>() {
    inner class AsignacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcono: ImageView = itemView.findViewById(R.id.imgIcono)
        val txtRuta: TextView = itemView.findViewById(R.id.txtRuta)
        val txtCodigo: TextView = itemView.findViewById(R.id.txtCodigo)
        val txtEstado: TextView = itemView.findViewById(R.id.txtEstado)
        val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        val txtVehiculo: TextView = itemView.findViewById(R.id.txtVehiculo)
        val txtConductor: TextView = itemView.findViewById(R.id.txtConductor)
        val txtAyudantes: TextView = itemView.findViewById(R.id.txtAyudantes)
        val btnVerDetalle: MaterialButton = itemView.findViewById(R.id.btnVerDetalle)
        val btnEditar: MaterialButton = itemView.findViewById(R.id.btnEditar)
        val btnCambiarEstado: MaterialButton = itemView.findViewById(R.id.btnCambiarEstado)
        val btnEliminar: MaterialButton = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsignacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asignacion, parent, false)
        return AsignacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AsignacionViewHolder, position: Int) {
        val asignacion = asignaciones[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        holder.txtRuta.text = asignacion.rutaNombre
        holder.txtCodigo.text = asignacion.rutaCodigo
        holder.txtFecha.text = "📅 ${sdf.format(asignacion.fechaAsignacion)}"
        holder.txtVehiculo.text = "🚛 ${asignacion.vehiculoPlaca}"
        holder.txtConductor.text = "👤 ${asignacion.conductorNombre}"

        // Ayudantes
        val ayudantesTexto = if (asignacion.ayudantesNombres.isEmpty()) {
            "Sin ayudantes"
        } else {
            "${asignacion.ayudantesNombres.size} ayudante(s): ${asignacion.ayudantesNombres.joinToString(", ")}"
        }
        holder.txtAyudantes.text = "👥 $ayudantesTexto"

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

        // Botones
        holder.btnVerDetalle.setOnClickListener {
            onVerDetalle(asignacion)
        }

        holder.btnEditar.setOnClickListener {
            onEditar(asignacion)
        }

        holder.btnCambiarEstado.setOnClickListener {
            onCambiarEstado(asignacion)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminar(asignacion)
        }

        // Ocultar botón editar si no es "Programada"
        if (asignacion.estado != "Programada") {
            holder.btnEditar.visibility = View.GONE
        } else {
            holder.btnEditar.visibility = View.VISIBLE
        }

        // Ocultar botón cambiar estado si está completada o cancelada
        if (asignacion.estado == "Completada" || asignacion.estado == "Cancelada") {
            holder.btnCambiarEstado.visibility = View.GONE
        } else {
            holder.btnCambiarEstado.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = asignaciones.size
}