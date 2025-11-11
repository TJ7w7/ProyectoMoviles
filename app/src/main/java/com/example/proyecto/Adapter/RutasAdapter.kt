package com.example.proyecto.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.Entidad.entRuta
import com.google.android.material.button.MaterialButton

class RutasAdapter(
    private val rutas: List<entRuta>,
    private val onVerPuntos: (entRuta) -> Unit,
    private val onEditar: (entRuta) -> Unit
) : RecyclerView.Adapter<RutasAdapter.RutaViewHolder>() {
    inner class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val imgIcono: ImageView = itemView.findViewById(R.id.imgIcono)
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val txtCodigo: TextView = itemView.findViewById(R.id.txtCodigo)
        val txtEstado: TextView = itemView.findViewById(R.id.txtEstado)
        val txtZona: TextView = itemView.findViewById(R.id.txtZona)
        val txtHorario: TextView = itemView.findViewById(R.id.txtHorario)
        val txtDias: TextView = itemView.findViewById(R.id.txtDias)
        val txtPuntos: TextView = itemView.findViewById(R.id.txtPuntos)
        val btnVerPuntos: MaterialButton = itemView.findViewById(R.id.btnVerPuntos)
        val btnEditar: MaterialButton = itemView.findViewById(R.id.btnEditar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ruta, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = rutas[position]

        holder.txtNombre.text = ruta.nombre
        holder.txtCodigo.text = ruta.codigo
        holder.txtZona.text = "🗺️ Zona: ${ruta.zona}"

        // Estado con colores
        holder.txtEstado.text = ruta.estado
        when (ruta.estado) {
            "Activa" -> holder.txtEstado.setBackgroundColor(
                Color.parseColor("#4CAF50")
            )
            "Inactiva" -> holder.txtEstado.setBackgroundColor(
                Color.parseColor("#F44336")
            )
            "En Revisión" -> holder.txtEstado.setBackgroundColor(
                Color.parseColor("#FF9800")
            )
        }

        // Horario (si existe en tu entidad)
        holder.txtHorario.visibility = View.GONE // Ocultar si no tienes este campo

        // Días de la semana
        val diasTexto = if (ruta.diasSemana.isEmpty()) {
            "Sin días asignados"
        } else {
            ruta.diasSemana.joinToString(", ")
        }
        holder.txtDias.text = "📅 $diasTexto"

        // Puntos asignados
        val cantidadPuntos = ruta.cantidadPuntos ?: 0
        holder.txtPuntos.text = "📍 $cantidadPuntos puntos asignados"

        // Botones
        holder.btnVerPuntos.setOnClickListener {
            onVerPuntos(ruta)
        }

        holder.btnEditar.setOnClickListener {
            onEditar(ruta)
        }
    }

    override fun getItemCount() = rutas.size
}