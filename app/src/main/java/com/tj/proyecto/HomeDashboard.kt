package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.tj.proyecto.Adapter.RutasActivasAdminAdapter
import com.tj.proyecto.Entidad.entAsignacionRuta
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeDashboard : Fragment(){
    private lateinit var db: FirebaseFirestore

    private lateinit var txtFecha: TextView

    // Cards de estadísticas generales
    private lateinit var cardTotalMaterialHoy: MaterialCardView
    private lateinit var txtPesoHoy: TextView
    private lateinit var txtMaterialesHoy: TextView

    private lateinit var cardTotalMaterialGeneral: MaterialCardView
    private lateinit var txtPesoTotal: TextView
    private lateinit var txtCantidadRegistros: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_dashboard, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupFecha()
        cargarEstadisticas()

        return view
    }

    private fun initViews(view: View) {
        txtFecha = view.findViewById(R.id.txtFecha)

        cardTotalMaterialHoy = view.findViewById(R.id.cardTotalMaterialHoy)
        txtPesoHoy = view.findViewById(R.id.txtPesoHoy)
        txtMaterialesHoy = view.findViewById(R.id.txtMaterialesHoy)

        cardTotalMaterialGeneral = view.findViewById(R.id.cardTotalMaterialGeneral)
        txtPesoTotal = view.findViewById(R.id.txtPesoTotal)
        txtCantidadRegistros = view.findViewById(R.id.txtCantidadRegistros)
    }

    private fun setupFecha() {
        val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM yyyy", Locale("es", "ES"))
        txtFecha.text = sdf.format(Date())
    }

    private fun cargarEstadisticas() {
        // Calcular inicio y fin del día actual
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val inicioDia = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val finDia = calendar.timeInMillis

        // 1. Material recolectado HOY
        db.collection("registros_materiales")
            .whereGreaterThanOrEqualTo("fechaHoraRegistro", inicioDia)
            .whereLessThanOrEqualTo("fechaHoraRegistro", finDia)
            .get()
            .addOnSuccessListener { documents ->
                var pesoTotalHoy = 0.0
                val materialesHoy = mutableMapOf(
                    "Plástico" to 0.0,
                    "Cartón" to 0.0,
                    "Vidrio" to 0.0,
                    "Papel" to 0.0
                )

                for (doc in documents) {
                    val pesoTotal = doc.getDouble("pesoTotal") ?: 0.0
                    pesoTotalHoy += pesoTotal

                    val materiales = doc.get("materialesRecolectados") as? Map<String, Any>
                    materiales?.forEach { (material, peso) ->
                        val pesoDouble = when (peso) {
                            is Double -> peso
                            is Long -> peso.toDouble()
                            else -> 0.0
                        }
                        val materialKey = material.replaceFirstChar { it.uppercase() }
                        materialesHoy[materialKey] = (materialesHoy[materialKey] ?: 0.0) + pesoDouble
                    }
                }

                txtPesoHoy.text = String.format("%.2f kg", pesoTotalHoy)

                val materialesTexto = materialesHoy.entries
                    .filter { it.value > 0.0 }
                    .joinToString(" • ") { "${it.key}: ${String.format("%.1f", it.value)}kg" }

                txtMaterialesHoy.text = if (materialesTexto.isNotEmpty()) {
                    materialesTexto
                } else {
                    "Sin recolecciones hoy"
                }
            }

        // 2. Material total acumulado
        db.collection("registros_materiales")
            .get()
            .addOnSuccessListener { documents ->
                var pesoTotalGeneral = 0.0
                for (doc in documents) {
                    pesoTotalGeneral += doc.getDouble("pesoTotal") ?: 0.0
                }

                txtPesoTotal.text = String.format("%.2f kg", pesoTotalGeneral)
                txtCantidadRegistros.text = "${documents.size()} registros"
            }
    }
}