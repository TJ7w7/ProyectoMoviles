package com.tj.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.R
import com.tj.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso

class PuntoRecoleccionAdapter(
    private val puntos: MutableList<entPuntoRecoleccion>,
    private val onEstadoChange: (entPuntoRecoleccion, Boolean) -> Unit,
    private val onVerMapa: (entPuntoRecoleccion) -> Unit,
    private val onEditar: (entPuntoRecoleccion) -> Unit,
    private val onDescargarQR: (entPuntoRecoleccion) -> Unit
) : RecyclerView.Adapter<PuntoRecoleccionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val imgIcono: ImageView = view.findViewById(R.id.imgIcono)
        val txtNombre: TextView = view.findViewById(R.id.txtNombre)
        val txtTipo: TextView = view.findViewById(R.id.txtTipo)
        val switchEstado: SwitchCompat = view.findViewById(R.id.switchEstado)
        val txtDireccion: TextView = view.findViewById(R.id.txtDireccion)
        val txtZonaFrecuencia: TextView = view.findViewById(R.id.txtZonaFrecuencia)
        val txtMateriales: TextView = view.findViewById(R.id.txtMateriales)
        val llQRContainer: LinearLayout = view.findViewById(R.id.llQRContainer)
        val imgQR: ImageView = view.findViewById(R.id.imgQR)
        val btnVerQR: MaterialButton = view.findViewById(R.id.btnVerQR)
        val btnVerMapa: MaterialButton = view.findViewById(R.id.btnVerMapa)
        val btnEditar: MaterialButton = view.findViewById(R.id.btnEditar)
        val btnDescargarQR: MaterialButton = view.findViewById(R.id.btnDescargarQR)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val punto = puntos[position]

        // Nombre
        holder.txtNombre.text = punto.nombre

        // Tipo
        holder.txtTipo.text = punto.tipo
        when (punto.tipo) {
            "Domicilio" -> holder.txtTipo.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_blue_dark)
            )
            "Comercio" -> holder.txtTipo.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_orange_dark)
            )
            "Institución" -> holder.txtTipo.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_purple)
            )
            else -> holder.txtTipo.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )
        }

        // Estado
        holder.switchEstado.isChecked = punto.estado
        holder.switchEstado.setOnCheckedChangeListener { _, isChecked ->
            onEstadoChange(punto, isChecked)
        }

        // Dirección
        holder.txtDireccion.text = "📍 ${punto.direccion}"

        // Zona y Frecuencia
        holder.txtZonaFrecuencia.text = "🗺️ Zona ${punto.zona} • 🔄 ${punto.frecuencia}"

        // Materiales
        if (punto.tiposMaterialAceptado.isNotEmpty()) {
            holder.txtMateriales.text = "♻️ ${punto.tiposMaterialAceptado.joinToString(", ")}"
            holder.txtMateriales.visibility = View.VISIBLE
        } else {
            holder.txtMateriales.visibility = View.GONE
        }

        var qrVisible = false
        holder.llQRContainer.visibility = View.GONE

        holder.btnVerQR.setOnClickListener {
            qrVisible = !qrVisible
            if (qrVisible) {
                holder.llQRContainer.visibility = View.VISIBLE
                holder.btnVerQR.text = "Ocultar QR"

                // Cargar imagen QR si existe
                if (punto.codigoQR.isNotEmpty()) {
                    Picasso.get()
                        .load(punto.codigoQR)
                        .placeholder(android.R.drawable.ic_menu_rotate)
                        .error(android.R.drawable.ic_menu_close_clear_cancel)
                        .into(holder.imgQR)
                } else {
                    // Si no hay QR, mostrar un mensaje o icono
                    holder.imgQR.setImageResource(android.R.drawable.ic_menu_help)
                }
            } else {
                holder.llQRContainer.visibility = View.GONE
                holder.btnVerQR.text = "Ver QR"
            }
        }

        // Botón descargar QR
        holder.btnDescargarQR.setOnClickListener {
            onDescargarQR(punto)
        }

        // Botones
        holder.btnVerMapa.setOnClickListener {
            onVerMapa(punto)
        }

        holder.btnEditar.setOnClickListener {
            onEditar(punto)
        }
    }

    override fun getItemCount() = puntos.size

//    fun updateList(newList: List<entPuntoRecoleccion>) {
//        puntos.clear()
//        puntos.addAll(newList)
//        notifyDataSetChanged()
//    }
}