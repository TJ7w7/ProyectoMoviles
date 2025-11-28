package com.tj.proyecto

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeDashboard : Fragment(){

    private lateinit var db: FirebaseFirestore

    // Vistas
    private lateinit var txtFecha: TextView
    private lateinit var txtPesoHoy: TextView
    private lateinit var txtPesoTotal: TextView
    private lateinit var imgGraficoGrande: ImageView
    private lateinit var txtCargandoGrafico: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_dashboard, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupFecha()
        cargarDatosYGrafico()

        return view
    }

    private fun initViews(view: View) {

        txtFecha = view.findViewById(R.id.txtFecha)
        txtPesoHoy = view.findViewById(R.id.txtPesoHoy)
        txtPesoTotal = view.findViewById(R.id.txtPesoTotal)
        imgGraficoGrande = view.findViewById(R.id.imgGraficoGrande)
        txtCargandoGrafico = view.findViewById(R.id.txtCargandoGrafico)

    }

    private fun setupFecha() {
        val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM yyyy", Locale("es", "ES"))
        txtFecha.text = sdf.format(Date())
    }

    private fun cargarDatosYGrafico() {
        // Obtenemos TODOS los registros
        db.collection("registros_materiales").get()
            .addOnSuccessListener { documents ->

                // Variables acumuladoras para el GRÁFICO (Histórico)
                var totalPlastico = 0.0
                var totalCarton = 0.0
                var totalVidrio = 0.0
                var totalPapel = 0.0

                var pesoTotalHistorico = 0.0
                var pesoHoy = 0.0

                // Configurar fecha de inicio de HOY
                val hoyInicio = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                for (doc in documents) {
                    val peso = doc.getDouble("pesoTotal") ?: 0.0
                    val fecha = doc.getLong("fechaHoraRegistro") ?: 0L

                    // 1. Sumar totales generales
                    pesoTotalHistorico += peso

                    // 2. Sumar totales de HOY
                    if (fecha >= hoyInicio) {
                        pesoHoy += peso
                    }

                    // 3. Desglosar materiales (Aquí estaba el error de lectura)
                    val materiales = doc.get("materialesRecolectados") as? Map<String, Any>

                    materiales?.forEach { (key, value) ->
                        // Aseguramos que el valor sea Double
                        val p = when (value) {
                            is Double -> value
                            is Long -> value.toDouble()
                            is String -> value.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        // USAMOS LAS CLAVES EXACTAS DE TU 'RegistroPesoMaterial'
                        // (plastico, carton, vidrio, papel)
                        when (key.lowercase().trim()) {
                            "plastico", "plástico" -> totalPlastico += p
                            "carton", "cartón" -> totalCarton += p
                            "vidrio" -> totalVidrio += p
                            "papel" -> totalPapel += p
                        }
                    }
                }

                // Actualizar Textos de las tarjetas
                txtPesoTotal.text = String.format("%.1f kg", pesoTotalHistorico)
                txtPesoHoy.text = String.format("%.1f kg", pesoHoy)

                // Generar el Gráfico solo si hay datos
                if (pesoTotalHistorico > 0) {
                    generarGraficoQuickChart(totalPlastico, totalCarton, totalVidrio, totalPapel)
                } else {
                    txtCargandoGrafico.text = "Sin registros aún"
                }
            }
            .addOnFailureListener {
                txtCargandoGrafico.text = "Error de conexión"
                Log.e("Dashboard", "Error cargando datos: ${it.message}")
            }
    }

    private fun generarGraficoQuickChart(plastico: Double, carton: Double, vidrio: Double, papel: Double) {

        val chartJson = """
        {
          "type": "pie",
          "data": {
            "labels": ["Plastico", "Carton", "Vidrio", "Papel"],
            "datasets": [{
              "data": [$plastico, $carton, $vidrio, $papel],
              "backgroundColor": ["#2196F3", "#FF9800", "#4CAF50", "#9E9E9E"]
            }]
          },
          "options": {
            "plugins": {
              "datalabels": {
                "color": "white",
                "font": { "size": 14, "weight": "bold" },
                "formatter": (value) => { return value + " kg" }
              },
              "legend": { "position": "bottom" }
            }
          }
        }
        """.trimIndent()

        try {
            // Codificamos la URL para evitar errores
            val jsonEncoded = URLEncoder.encode(chartJson, "UTF-8")
            val urlFinal = "https://quickchart.io/chart?c=$jsonEncoded"

            Log.d("GRAFICO", "Cargando URL: $urlFinal")

            if (isAdded && context != null) {
                // Carga simple y directa
                Glide.with(this)
                    .load(urlFinal)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imgGraficoGrande)

                // Mostramos la imagen y ocultamos el texto de carga
                imgGraficoGrande.visibility = View.VISIBLE
                txtCargandoGrafico.visibility = View.GONE
            }

        } catch (e: Exception) {
            Log.e("GRAFICO", "Error en URL: ${e.message}")
        }
    }
}