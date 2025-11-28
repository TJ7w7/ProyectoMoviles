package com.tj.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.R
import com.tj.proyecto.Recolector.RecolectorRuta
import com.google.android.material.button.MaterialButton

class PuntosRecoleccionAdapterTr(
    private val puntos: List<RecolectorRuta.PuntoConEstado>,
//    private val asignacionId: String,
    private val onPuntoClick: (RecolectorRuta.PuntoConEstado) -> Unit,
    private val onMapGoogle: (RecolectorRuta.PuntoConEstado) -> Unit,
    private val onEscanearQR: (RecolectorRuta.PuntoConEstado) -> Unit,
    private val onReportarClick: (RecolectorRuta.PuntoConEstado) -> Unit
) : RecyclerView.Adapter<PuntosRecoleccionAdapterTr.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardPunto: CardView = itemView.findViewById(R.id.cardPunto)
        val txtOrden: TextView = itemView.findViewById(R.id.txtOrden)
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val txtDireccion: TextView = itemView.findViewById(R.id.txtDireccion)
        val txtTipo: TextView = itemView.findViewById(R.id.txtTipo)
        val txtMateriales: TextView = itemView.findViewById(R.id.txtMateriales)
        val txtEstado: TextView = itemView.findViewById(R.id.txtEstado)
        val btnEscanearQR: MaterialButton = itemView.findViewById(R.id.btnMarcarCompletado)
        val btnVerEnMapa: MaterialButton = itemView.findViewById(R.id.btnVerEnMapa)
        val btnReportar: ImageButton = itemView.findViewById(R.id.btnReportarIncidencia) // NUEVO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto_recoleccion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val puntoEstado = puntos[position]
        val punto = puntoEstado.punto

        holder.txtOrden.text = "${puntoEstado.orden}"
        holder.txtNombre.text = punto.nombre
        holder.txtDireccion.text = "📍 ${punto.direccion}"
        holder.txtTipo.text = "Tipo: ${punto.tipo}"

        val materiales = punto.tiposMaterialAceptado.joinToString(", ")
        holder.txtMateriales.text = "♻️ $materiales"

        // Cambiar el texto del botón a "Escanear QR"
        holder.btnEscanearQR.text = "Escanear QR"
        holder.btnEscanearQR.icon = ContextCompat.getDrawable(
            holder.itemView.context,
            android.R.drawable.ic_menu_camera
        )

        // Configurar según estado
        when (puntoEstado.estado) {
            RecolectorRuta.EstadoRecoleccion.COMPLETADO -> {
                holder.cardPunto.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light)
                )
                holder.txtEstado.text = "✓ Completado"
                holder.txtEstado.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
                )
                holder.btnEscanearQR.visibility = View.GONE
                holder.btnReportar.visibility = View.GONE
                holder.txtOrden.setBackgroundResource(R.drawable.circle_completed)
            }
            RecolectorRuta.EstadoRecoleccion.EN_CURSO -> {
                holder.cardPunto.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_light)
                )
                holder.txtEstado.text = "→ En Curso"
                holder.txtEstado.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_dark)
                )
                holder.btnEscanearQR.visibility = View.VISIBLE
                holder.btnEscanearQR.isEnabled = true
                holder.txtOrden.setBackgroundResource(R.drawable.circle_active)

                holder.btnReportar.visibility = View.VISIBLE
            }
            RecolectorRuta.EstadoRecoleccion.PENDIENTE -> {
                holder.cardPunto.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                )
                holder.txtEstado.text = "⏳ Pendiente"
                holder.txtEstado.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
                )
                holder.btnEscanearQR.visibility = View.VISIBLE
                holder.btnEscanearQR.isEnabled = false
                holder.btnReportar.visibility = View.GONE
                holder.txtOrden.setBackgroundResource(R.drawable.circle_inactive)
            }
        }

        holder.btnReportar.setOnClickListener {
            onReportarClick(puntoEstado)
        }

        holder.btnEscanearQR.setOnClickListener {
            onEscanearQR(puntoEstado)
        }

        holder.btnVerEnMapa.setOnClickListener {
            onMapGoogle(puntoEstado)
        }

        holder.cardPunto.setOnClickListener {
            onPuntoClick(puntoEstado)
        }
    }

    override fun getItemCount() = puntos.size
}