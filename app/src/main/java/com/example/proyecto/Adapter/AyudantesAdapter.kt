package com.example.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.Entidad.entUsuario

class AyudantesAdapter(
    private val ayudantes: List<entUsuario>,
    private val onQuitar: (Int) -> Unit
) : RecyclerView.Adapter<AyudantesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombre: TextView = view.findViewById(R.id.txtNombreAyudante)
        val btnQuitar: ImageButton = view.findViewById(R.id.btnQuitarAyudante)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ayudante, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ayudante = ayudantes[position]

        holder.txtNombre.text = "${ayudante.nombres} ${ayudante.apellidos}"

        holder.btnQuitar.setOnClickListener {
            onQuitar(position)
        }
    }

    override fun getItemCount() = ayudantes.size
}