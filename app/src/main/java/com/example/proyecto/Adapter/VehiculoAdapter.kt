package com.example.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.Entidad.entVehiculo
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class VehiculoAdapter(
    private val vehiculos: MutableList<entVehiculo>,
    private val onMantenimiento: (entVehiculo) -> Unit,
    private val onEditar: (entVehiculo) -> Unit,
    private val onHistorial: (entVehiculo) -> Unit
) : RecyclerView.Adapter<VehiculoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcono: ImageView = view.findViewById(R.id.imgIcono)
        val txtPlaca: TextView = view.findViewById(R.id.txtPlaca)
        val txtTipoVehiculo: TextView = view.findViewById(R.id.txtTipoVehiculo)
        val txtEstado: TextView = view.findViewById(R.id.txtEstado)
        val txtCapacidad: TextView = view.findViewById(R.id.txtCapacidad)
        val txtUltimoMantenimiento: TextView = view.findViewById(R.id.txtUltimoMantenimiento)
        val btnMantenimiento: MaterialButton = view.findViewById(R.id.btnMantenimiento)
        val btnEditar: MaterialButton = view.findViewById(R.id.btnEditar)
        val btnHistorial: MaterialButton = view.findViewById(R.id.btnHistorial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehiculo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val vehiculo = vehiculos[position]

        // Placa
        holder.txtPlaca.text = vehiculo.placa

        // Tipo de vehículo
        holder.txtTipoVehiculo.text = vehiculo.tipoVehiculo

        // Estado con colores
        holder.txtEstado.text = vehiculo.estado
        when (vehiculo.estado) {
            "Disponible" -> holder.txtEstado.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )
            "En Ruta" -> holder.txtEstado.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_blue_dark)
            )
            "Mantenimiento" -> holder.txtEstado.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_orange_dark)
            )
            "Inactivo" -> holder.txtEstado.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.darker_gray)
            )
        }

        // Icono según tipo de vehículo
        when (vehiculo.tipoVehiculo) {
            "Triciclo" -> {
                holder.imgIcono.setImageResource(android.R.drawable.ic_menu_directions)
                holder.imgIcono.setBackgroundColor(
                    holder.itemView.context.getColor(android.R.color.holo_green_light)
                )
            }
            "Motocicleta" -> {
                holder.imgIcono.setImageResource(android.R.drawable.ic_menu_directions)
                holder.imgIcono.setBackgroundColor(
                    holder.itemView.context.getColor(android.R.color.holo_blue_light)
                )
            }
            "Camión Pequeño" -> {
                holder.imgIcono.setImageResource(android.R.drawable.ic_menu_directions)
                holder.imgIcono.setBackgroundColor(
                    holder.itemView.context.getColor(android.R.color.holo_orange_light)
                )
            }
            "Camión Grande" -> {
                holder.imgIcono.setImageResource(android.R.drawable.ic_menu_directions)
                holder.imgIcono.setBackgroundColor(
                    holder.itemView.context.getColor(android.R.color.holo_red_light)
                )
            }
        }

        // Capacidad
        holder.txtCapacidad.text = "⚖️ Capacidad: ${vehiculo.capacidadKg} Kg"

        // Último mantenimiento
        val mantenimientoText = if (vehiculo.ultimoMantenimiento != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            "🔧 Último mant.: ${sdf.format(vehiculo.ultimoMantenimiento)}"
        } else {
            "🔧 Sin registro de mantenimiento"
        }
        holder.txtUltimoMantenimiento.text = mantenimientoText

        // Botones
        holder.btnMantenimiento.setOnClickListener {
            onMantenimiento(vehiculo)
        }

        holder.btnHistorial.setOnClickListener {
            onHistorial(vehiculo)
        }

        holder.btnEditar.setOnClickListener {
            onEditar(vehiculo)
        }
    }

    override fun getItemCount() = vehiculos.size

    fun updateList(newList: List<entVehiculo>) {
        vehiculos.clear()
        vehiculos.addAll(newList)
        notifyDataSetChanged()
    }
}