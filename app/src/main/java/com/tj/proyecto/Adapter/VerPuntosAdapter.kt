package com.tj.proyecto.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.R
import com.tj.proyecto.Recolector.RecolectorRuta
import com.tj.proyecto.VerPuntosRuta

class VerPuntosAdapter(
    private val puntos: List<VerPuntosRuta.PuntoConOrden>,
    private val puntosConIncidencia: Set<String> = emptySet(),
    private val onPuntoClick: (VerPuntosRuta.PuntoConOrden) -> Unit = {}
) : RecyclerView.Adapter<VerPuntosAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardPuntoVer)
        val txtOrden: TextView = itemView.findViewById(R.id.txtOrden)
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val txtDireccion: TextView = itemView.findViewById(R.id.txtDireccion)
        val txtZona: TextView = itemView.findViewById(R.id.txtZona)
        val imgAlerta: ImageView = itemView.findViewById(R.id.imgAlerta)
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

        // --- LÓGICA DE SEGURIDAD VISUAL ---
        // Verificamos si este punto tiene una incidencia reportada
        if (puntosConIncidencia.contains(punto.id)) {
            // ¡PROBLEMA! Pintamos de rojo suave
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Rojo muy claro
            holder.txtOrden.setBackgroundColor(Color.RED)
            holder.imgAlerta.visibility = View.VISIBLE

            // Le damos click
            holder.itemView.setOnClickListener { onPuntoClick(puntoConOrden) }
        } else {
            // NORMAL
            holder.cardView.setCardBackgroundColor(Color.WHITE)
            holder.txtOrden.setBackgroundColor(Color.parseColor("#0D47A1")) // Azul normal
            holder.imgAlerta.visibility = View.GONE

            // Opcional: Bloquear click si NO tiene incidencia (Para evitar errores del admin)
            // holder.itemView.setOnClickListener { /* No hacer nada o mostrar mensaje "Todo ok aquí" */ }
            holder.itemView.setOnClickListener { onPuntoClick(puntoConOrden) }
        }

    }

    override fun getItemCount() = puntos.size
}