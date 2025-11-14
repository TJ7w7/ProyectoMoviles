package com.tj.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.R
import com.tj.proyecto.Entidad.entUsuario
import com.google.android.material.button.MaterialButton

class UsuarioAdapter (
    private val usuarios: MutableList<entUsuario>,
    private val onEstadoChange: (entUsuario, Boolean) -> Unit,
    private val onVerDetalles: (entUsuario) -> Unit,
    private val onEditar: (entUsuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.ViewHolder>(){
    // El ViewHolder contiene las vistas de cada item
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcono: ImageView = view.findViewById(R.id.imgIcono)
        val txtNombreCompleto: TextView = view.findViewById(R.id.txtNombreCompleto)
        val txtTipoUsuario: TextView = view.findViewById(R.id.txtTipoUsuario)
        val txtTelefono: TextView = view.findViewById(R.id.txtTelefono)
        val txtCorreo: TextView = view.findViewById(R.id.txtCorreo)
        val switchEstado: SwitchCompat = view.findViewById(R.id.switchEstado)
        val btnVerDetalles: MaterialButton = view.findViewById(R.id.btnVerDetalles)
        val btnEditar: MaterialButton = view.findViewById(R.id.btnEditar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario = usuarios[position]

        // Nombre completo
        holder.txtNombreCompleto.text = "${usuario.nombres} ${usuario.apellidos}"

        // Tipo de usuario
        holder.txtTipoUsuario.text = usuario.tipoUsuario
        if (usuario.tipoUsuario == "Administrador") {
            holder.txtTipoUsuario.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_purple)
            )
        } else {
            holder.txtTipoUsuario.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )
        }

        // Icono según tipo
        if (usuario.tipoUsuario == "Administrador") {
            holder.imgIcono.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_purple)
            )
        } else {
            holder.imgIcono.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_blue_light)
            )
        }

        // Teléfono
        holder.txtTelefono.text = "📱 ${usuario.telefono}"

        // Correo
        holder.txtCorreo.text = "📧 ${usuario.correo}"

        // Estado - CORRECCIÓN AQUÍ
        // Primero, remover cualquier listener previo
        holder.switchEstado.setOnCheckedChangeListener(null)

        // Luego, actualizar el estado
        holder.switchEstado.isChecked = usuario.estado

        // Finalmente, agregar el nuevo listener
        holder.switchEstado.setOnCheckedChangeListener { _, isChecked ->
            // Solo llamar si el estado realmente cambió
            if (isChecked != usuario.estado) {
                onEstadoChange(usuario, isChecked)
            }
        }

        // Estado
//        holder.switchEstado.isChecked = usuario.estado
//        holder.switchEstado.setOnCheckedChangeListener { _, isChecked ->
//            onEstadoChange(usuario, isChecked)
//        }

        // Botones
        holder.btnVerDetalles.setOnClickListener {
            onVerDetalles(usuario)
        }

        holder.btnEditar.setOnClickListener {
            onEditar(usuario)
        }
    }

    override fun getItemCount() = usuarios.size

//    fun updateList(newList: List<entUsuario>) {
//        usuarios.clear()
//        usuarios.addAll(newList)
//        notifyDataSetChanged()
//    }
}