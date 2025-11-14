package com.tj.proyecto.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.tj.proyecto.Entidad.entAsignacionRuta
import com.tj.proyecto.R

class RutasActivasAdminAdapter(

    private val rutas: List<entAsignacionRuta>,
    private val onRutaClick: (entAsignacionRuta) -> Unit
) : RecyclerView.Adapter<RutasActivasAdminAdapter.ViewHolder>() {
    private val db = FirebaseFirestore.getInstance()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtEstado: TextView = itemView.findViewById(R.id.txtEstado)
        val txtHorario: TextView = itemView.findViewById(R.id.txtHorario)
        val txtNombreRuta: TextView = itemView.findViewById(R.id.txtNombreRuta)
        val txtCodigo: TextView = itemView.findViewById(R.id.txtCodigo)
        val txtVehiculo: TextView = itemView.findViewById(R.id.txtVehiculo)
        val txtConductor: TextView = itemView.findViewById(R.id.txtConductor)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val txtProgreso: TextView = itemView.findViewById(R.id.txtProgreso)
        val txtProgresoTexto: TextView = itemView.findViewById(R.id.txtProgresoTexto)
        val btnVerMapa: MaterialButton = itemView.findViewById(R.id.btnVerMapa)
        val btnVerDetalles: MaterialButton = itemView.findViewById(R.id.btnVerDetalles)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ruta_hoy, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val asignacion = rutas[position]

        // IMPORTANTE: Resetear valores mientras carga
        holder.progressBar.progress = 0
        holder.txtProgreso.text = "0/0"
        holder.txtProgresoTexto.text = "Cargando progreso..."

        // Estado
        holder.txtEstado.text = asignacion.estado
        when (asignacion.estado) {
            "Programada" -> {
                holder.txtEstado.setBackgroundColor(
                    android.graphics.Color.parseColor("#2196F3")
                )
            }
            "En Progreso" -> {
                holder.txtEstado.setBackgroundColor(
                    android.graphics.Color.parseColor("#FF9800")
                )
            }
            "Completada" -> {
                holder.txtEstado.setBackgroundColor(
                    android.graphics.Color.parseColor("#4CAF50")
                )
            }
        }

        // Horario
        holder.txtHorario.text = "🕐 Asignada hoy"

        // Nombre y código
        holder.txtNombreRuta.text = asignacion.rutaNombre
        holder.txtCodigo.text = asignacion.rutaCodigo

        // Vehículo
        holder.txtVehiculo.text = "🚛 ${asignacion.vehiculoPlaca}"

        // Conductor
        holder.txtConductor.text = "👤 Conductor: ${asignacion.conductorNombre}"

        // Cargar progreso de puntos completados
        cargarProgresoPuntos(asignacion.id, asignacion.rutaId, holder)

        // Botones
        holder.btnVerMapa.setOnClickListener {
            // TODO: Implementar vista de mapa para admin
            Toast.makeText(
                holder.itemView.context,
                "Próximamente: Mapa de ruta",
                Toast.LENGTH_SHORT
            ).show()
        }

        holder.btnVerDetalles.setOnClickListener {
            onRutaClick(asignacion)
        }
    }

    private fun cargarProgresoPuntos(
        asignacionId: String,
        rutaId: String,
        holder: ViewHolder
    ) {
        // Contar puntos totales de la ruta
        db.collection("ruta_puntos")
            .whereEqualTo("rutaId", rutaId)
            .get()
            .addOnSuccessListener { rutaPuntos ->
                val totalPuntos = rutaPuntos.size()

                // Contar puntos completados (con evidencia)
                db.collection("evidencias_recoleccion")
                    .whereEqualTo("asignacionId", asignacionId)
                    .get()
                    .addOnSuccessListener { evidencias ->
                        val completados = evidencias.size()

                        // Actualizar UI
                        holder.txtProgreso.text = "$completados/$totalPuntos"
                        holder.txtProgresoTexto.text =
                            "$completados de $totalPuntos puntos completados"

                        if (totalPuntos > 0) {
                            val progreso = (completados * 100) / totalPuntos
                            holder.progressBar.progress = progreso
                        } else {
                            holder.progressBar.progress = 0
                            holder.txtProgresoTexto.text = "Sin puntos asignados"
                        }
                    }
                    .addOnFailureListener {
                        holder.txtProgreso.text = "0/$totalPuntos"
                        holder.txtProgresoTexto.text = "Error al cargar progreso"
                        holder.progressBar.progress = 0
                    }
            }
            .addOnFailureListener {
                holder.txtProgreso.text = "0/0"
                holder.txtProgresoTexto.text = "Error al cargar datos"
                holder.progressBar.progress = 0
            }
    }

    override fun getItemCount() = rutas.size
}