package com.tj.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.DetalleAsignacion
import com.tj.proyecto.R
import java.text.SimpleDateFormat
import java.util.Locale

class EvidenciasPorPuntoAdapter(
    private val evidencias: List<DetalleAsignacion.EvidenciaPunto>,
    private val onFotoClick: (String) -> Unit
) : RecyclerView.Adapter<EvidenciasPorPuntoAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtOrden: TextView = itemView.findViewById(R.id.txtOrden)
        val txtNombrePunto: TextView = itemView.findViewById(R.id.txtNombrePunto)
        val txtDireccionPunto: TextView = itemView.findViewById(R.id.txtDireccionPunto)
        val txtFechaRecoleccion: TextView = itemView.findViewById(R.id.txtFechaRecoleccion)
        val txtCantidadFotos: TextView = itemView.findViewById(R.id.txtCantidadFotos)
        val rvFotosGrid: RecyclerView = itemView.findViewById(R.id.rvFotosGrid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evidencia_punto, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val evidencia = evidencias[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        holder.txtOrden.text = "${evidencia.orden}"
        holder.txtNombrePunto.text = evidencia.nombrePunto
        holder.txtDireccionPunto.text = evidencia.direccionPunto
        holder.txtFechaRecoleccion.text = "Recolectado: ${sdf.format(evidencia.fechaRecoleccion)}"
        holder.txtCantidadFotos.text = "${evidencia.fotos.size} foto(s)"

        // Configurar grid de fotos
        val fotosAdapter = FotosEvidenciaAdapter(evidencia.fotos, onFotoClick)
        holder.rvFotosGrid.layoutManager = GridLayoutManager(holder.itemView.context, 3)
        holder.rvFotosGrid.adapter = fotosAdapter
    }

    override fun getItemCount() = evidencias.size
}