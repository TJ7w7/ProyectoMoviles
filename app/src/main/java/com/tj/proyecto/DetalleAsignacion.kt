package com.tj.proyecto

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Adapter.VerPuntosAdapter
import com.tj.proyecto.Entidad.entAsignacionRuta
import com.tj.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.tj.proyecto.Adapter.EvidenciasPorPuntoAdapter
import com.tj.proyecto.Adapter.FotosEvidenciaAdapter
import java.text.SimpleDateFormat
import java.util.Locale

class DetalleAsignacion : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var asignacionId: String

    private lateinit var txtRutaNombre: TextView
    private lateinit var txtRutaCodigo: TextView
    private lateinit var txtEstado: TextView
    private lateinit var txtFechaAsignacion: TextView
    private lateinit var txtFechaEjecucion: TextView
    private lateinit var txtVehiculo: TextView
    private lateinit var txtConductor: TextView
    private lateinit var txtAyudantes: TextView
    private lateinit var txtObservaciones: TextView
    private lateinit var rvPuntosRuta: RecyclerView
    private lateinit var btnVolver: MaterialButton

    // Nuevos elementos para evidencias
    private lateinit var cardEvidencias: MaterialCardView
    private lateinit var txtSinEvidencias: TextView
    private lateinit var rvEvidenciasPuntos: RecyclerView

    private val puntosRuta = mutableListOf<VerPuntosRuta.PuntoConOrden>()
    private lateinit var puntosAdapter: VerPuntosAdapter

    // Adaptador para evidencias agrupadas por punto
    private val evidenciasPuntos = mutableListOf<EvidenciaPunto>()
    private lateinit var evidenciasAdapter: EvidenciasPorPuntoAdapter

    companion object {
        private const val TAG = "DetalleAsignacion"

        fun newInstance(asignacionId: String): DetalleAsignacion {
            val fragment = DetalleAsignacion()
            val args = Bundle()
            args.putString("asignacionId", asignacionId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asignacionId = arguments?.getString("asignacionId") ?: ""
        Log.d(TAG, "onCreate - AsignacionId: $asignacionId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detalle_asignacion, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecyclerViews()
        cargarDatosAsignacion()

        btnVolver.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        txtRutaNombre = view.findViewById(R.id.txtRutaNombre)
        txtRutaCodigo = view.findViewById(R.id.txtRutaCodigo)
        txtEstado = view.findViewById(R.id.txtEstado)
        txtFechaAsignacion = view.findViewById(R.id.txtFechaAsignacion)
        txtFechaEjecucion = view.findViewById(R.id.txtFechaEjecucion)
        txtVehiculo = view.findViewById(R.id.txtVehiculo)
        txtConductor = view.findViewById(R.id.txtConductor)
        txtAyudantes = view.findViewById(R.id.txtAyudantes)
        txtObservaciones = view.findViewById(R.id.txtObservaciones)
        rvPuntosRuta = view.findViewById(R.id.rvPuntosRuta)
        btnVolver = view.findViewById(R.id.btnVolver)

        // Nuevos elementos
        cardEvidencias = view.findViewById(R.id.cardEvidencias)
        txtSinEvidencias = view.findViewById(R.id.txtSinEvidencias)
        rvEvidenciasPuntos = view.findViewById(R.id.rvEvidenciasPuntos)
    }

    private fun setupRecyclerViews() {
        // RecyclerView de puntos
        puntosAdapter = VerPuntosAdapter(puntosRuta)
        rvPuntosRuta.layoutManager = LinearLayoutManager(requireContext())
        rvPuntosRuta.adapter = puntosAdapter

        // RecyclerView de evidencias por punto
        evidenciasAdapter = EvidenciasPorPuntoAdapter(evidenciasPuntos) { fotoUrl ->
            mostrarImagenCompleta(fotoUrl)
        }
        rvEvidenciasPuntos.layoutManager = LinearLayoutManager(requireContext())
        rvEvidenciasPuntos.adapter = evidenciasAdapter
    }

    private fun cargarDatosAsignacion() {
        Log.d(TAG, "Cargando datos de asignación: $asignacionId")

        db.collection("asignaciones_rutas")
            .document(asignacionId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "Documento encontrado: ${document.data}")
                    val asignacion = document.toObject(entAsignacionRuta::class.java)
                    asignacion?.let {
                        mostrarDatos(it)
                        Log.d(TAG, "Estado de asignación: ${it.estado}")

                        // Cargar evidencias solo si el estado lo permite
                        if (it.estado in listOf("En Progreso", "Completada")) {
                            Log.d(TAG, "Cargando evidencias para estado: ${it.estado}")
                            cargarEvidencias()
                        } else {
                            Log.d(TAG, "No se cargan evidencias. Estado: ${it.estado}")
                            cardEvidencias.visibility = View.GONE
                        }
                    }
                } else {
                    Log.e(TAG, "Documento no existe")
                    Toast.makeText(requireContext(), "Asignación no encontrada", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar datos: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarDatos(asignacion: entAsignacionRuta) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        txtRutaNombre.text = asignacion.rutaNombre
        txtRutaCodigo.text = asignacion.rutaCodigo
        txtEstado.text = asignacion.estado

        // Color del estado
        when (asignacion.estado) {
            "Programada" -> txtEstado.setBackgroundColor(
                android.graphics.Color.parseColor("#2196F3")
            )
            "En Progreso" -> txtEstado.setBackgroundColor(
                android.graphics.Color.parseColor("#FF9800")
            )
            "Completada" -> txtEstado.setBackgroundColor(
                android.graphics.Color.parseColor("#4CAF50")
            )
            "Cancelada" -> txtEstado.setBackgroundColor(
                android.graphics.Color.parseColor("#F44336")
            )
        }

        txtFechaAsignacion.text = sdf.format(asignacion.fechaAsignacion)

        if (asignacion.fechaEjecucion != null) {
            txtFechaEjecucion.text = sdf.format(asignacion.fechaEjecucion)
            txtFechaEjecucion.visibility = View.VISIBLE
        } else {
            txtFechaEjecucion.visibility = View.GONE
        }

        txtVehiculo.text = asignacion.vehiculoPlaca
        txtConductor.text = asignacion.conductorNombre

        if (asignacion.ayudantesNombres.isEmpty()) {
            txtAyudantes.text = "Sin ayudantes"
        } else {
            txtAyudantes.text = asignacion.ayudantesNombres.joinToString("\n• ", prefix = "• ")
        }

        if (asignacion.observaciones.isNotEmpty()) {
            txtObservaciones.text = asignacion.observaciones
            txtObservaciones.visibility = View.VISIBLE
        } else {
            txtObservaciones.visibility = View.GONE
        }

        // Cargar puntos de la ruta
        cargarPuntosRuta(asignacion.rutaId)
    }

    private fun cargarPuntosRuta(rutaId: String) {
        db.collection("ruta_puntos")
            .whereEqualTo("rutaId", rutaId)
            .orderBy("orden")
            .get()
            .addOnSuccessListener { rutaPuntos ->
                if (rutaPuntos.isEmpty) {
                    return@addOnSuccessListener
                }

                puntosRuta.clear()
                var puntosRestantes = rutaPuntos.size()

                for (rutaPunto in rutaPuntos) {
                    val puntoId = rutaPunto.getString("puntoId") ?: ""
                    val orden = rutaPunto.getLong("orden")?.toInt() ?: 0
                    val fechaAsignacion = rutaPunto.getLong("fechaAsignacion") ?: 0L

                    db.collection("puntos_recoleccion")
                        .document(puntoId)
                        .get()
                        .addOnSuccessListener { puntoDoc ->
                            if (puntoDoc.exists()) {
                                val punto = puntoDoc.toObject(entPuntoRecoleccion::class.java)
                                if (punto != null) {
                                    puntosRuta.add(VerPuntosRuta.PuntoConOrden(punto, orden, fechaAsignacion))
                                }
                            }

                            puntosRestantes--
                            if (puntosRestantes == 0) {
                                puntosRuta.sortBy { it.orden }
                                puntosAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }
    }

    /**
     * Carga las evidencias fotográficas de todos los puntos de esta asignación
     * VERSIÓN MEJORADA CON DEBUGGING Y SIN ORDERBY
     */
    private fun cargarEvidencias() {
        Log.d(TAG, "=== INICIANDO CARGA DE EVIDENCIAS ===")
        Log.d(TAG, "AsignacionId: $asignacionId")

        // Primero mostrar la card
        cardEvidencias.visibility = View.VISIBLE
        txtSinEvidencias.visibility = View.VISIBLE
        txtSinEvidencias.text = "Cargando evidencias..."
        rvEvidenciasPuntos.visibility = View.GONE

        // Consulta SIN orderBy para evitar errores de índice
        db.collection("evidencias_recoleccion")
            .whereEqualTo("asignacionId", asignacionId)
            .get()
            .addOnSuccessListener { evidenciasSnapshot ->
                Log.d(TAG, "Query exitosa. Documentos encontrados: ${evidenciasSnapshot.size()}")

                if (evidenciasSnapshot.isEmpty) {
                    Log.w(TAG, "No se encontraron evidencias para esta asignación")
                    txtSinEvidencias.text = "Aún no se han tomado fotos de evidencia para esta asignación"
                    txtSinEvidencias.visibility = View.VISIBLE
                    rvEvidenciasPuntos.visibility = View.GONE
                    return@addOnSuccessListener
                }

                // Log de cada documento encontrado
                evidenciasSnapshot.documents.forEachIndexed { index, doc ->
                    Log.d(TAG, "Evidencia $index: ${doc.id} - ${doc.data}")
                }

                evidenciasPuntos.clear()
                var evidenciasRestantes = evidenciasSnapshot.size()

                for (evidenciaDoc in evidenciasSnapshot.documents) {
                    val puntoId = evidenciaDoc.getString("puntoId") ?: ""
                    val orden = evidenciaDoc.getLong("orden")?.toInt() ?: 0
                    val fotos = evidenciaDoc.get("fotos") as? List<String> ?: emptyList()
                    val fechaRecoleccion = evidenciaDoc.getLong("fechaRecoleccion") ?: 0L

                    Log.d(TAG, "Procesando evidencia: puntoId=$puntoId, orden=$orden, fotos=${fotos.size}")

                    if (fotos.isEmpty()) {
                        Log.w(TAG, "Advertencia: Evidencia sin fotos para punto $puntoId")
                    }

                    // Obtener datos del punto
                    db.collection("puntos_recoleccion")
                        .document(puntoId)
                        .get()
                        .addOnSuccessListener { puntoDoc ->
                            if (puntoDoc.exists()) {
                                val nombrePunto = puntoDoc.getString("nombre") ?: "Sin nombre"
                                val direccionPunto = puntoDoc.getString("direccion") ?: ""

                                Log.d(TAG, "Punto encontrado: $nombrePunto - ${fotos.size} fotos")

                                evidenciasPuntos.add(
                                    EvidenciaPunto(
                                        puntoId = puntoId,
                                        orden = orden,
                                        nombrePunto = nombrePunto,
                                        direccionPunto = direccionPunto,
                                        fotos = fotos,
                                        fechaRecoleccion = fechaRecoleccion
                                    )
                                )
                            } else {
                                Log.w(TAG, "Punto no encontrado: $puntoId")
                            }

                            evidenciasRestantes--
                            Log.d(TAG, "Evidencias restantes por procesar: $evidenciasRestantes")

                            if (evidenciasRestantes == 0) {
                                // Ordenar manualmente por orden
                                evidenciasPuntos.sortBy { it.orden }

                                Log.d(TAG, "=== FINALIZANDO CARGA ===")
                                Log.d(TAG, "Total evidencias cargadas: ${evidenciasPuntos.size}")

                                if (evidenciasPuntos.isEmpty()) {
                                    Log.w(TAG, "No se agregaron evidencias a la lista")
                                    txtSinEvidencias.text = "⏳ Aún no se han tomado fotos de evidencia para esta asignación"
                                    txtSinEvidencias.visibility = View.VISIBLE
                                    rvEvidenciasPuntos.visibility = View.GONE
                                } else {
                                    Log.d(TAG, "Mostrando ${evidenciasPuntos.size} evidencias en RecyclerView")
                                    txtSinEvidencias.visibility = View.GONE
                                    rvEvidenciasPuntos.visibility = View.VISIBLE
                                    evidenciasAdapter.notifyDataSetChanged()

                                    // Log detallado de lo que se va a mostrar
                                    evidenciasPuntos.forEachIndexed { index, ev ->
                                        Log.d(TAG, "Evidencia $index: ${ev.nombrePunto} - ${ev.fotos.size} fotos")
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al cargar punto $puntoId: ${e.message}", e)
                            evidenciasRestantes--
                            if (evidenciasRestantes == 0) {
                                finalizarCargaEvidencias()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ERROR en query de evidencias: ${e.message}", e)
                cardEvidencias.visibility = View.VISIBLE
                txtSinEvidencias.text = "Error al cargar evidencias: ${e.message}"
                txtSinEvidencias.visibility = View.VISIBLE
                rvEvidenciasPuntos.visibility = View.GONE

                Toast.makeText(
                    requireContext(),
                    "Error al cargar evidencias: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun finalizarCargaEvidencias() {
        evidenciasPuntos.sortBy { it.orden }

        if (evidenciasPuntos.isEmpty()) {
            txtSinEvidencias.text = "Aún no se han tomado fotos de evidencia para esta asignación"
            txtSinEvidencias.visibility = View.VISIBLE
            rvEvidenciasPuntos.visibility = View.GONE
        } else {
            txtSinEvidencias.visibility = View.GONE
            rvEvidenciasPuntos.visibility = View.VISIBLE
            evidenciasAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Muestra una imagen en pantalla completa
     */
    private fun mostrarImagenCompleta(fotoUrl: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_imagen_completa, null)

        val imgCompleta = dialogView.findViewById<ImageView>(R.id.imgCompleta)

        Picasso.get()
            .load(fotoUrl)
            .into(imgCompleta)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    /**
     * Data class para agrupar evidencias por punto
     */
    data class EvidenciaPunto(
        val puntoId: String,
        val orden: Int,
        val nombrePunto: String,
        val direccionPunto: String,
        val fotos: List<String>,
        val fechaRecoleccion: Long
    )
}

//    private lateinit var db: FirebaseFirestore
//    private lateinit var asignacionId: String
//
//    private lateinit var txtRutaNombre: TextView
//    private lateinit var txtRutaCodigo: TextView
//    private lateinit var txtEstado: TextView
//    private lateinit var txtFechaAsignacion: TextView
//    private lateinit var txtFechaEjecucion: TextView
//    private lateinit var txtVehiculo: TextView
//    private lateinit var txtConductor: TextView
//    private lateinit var txtAyudantes: TextView
//    private lateinit var txtObservaciones: TextView
//    private lateinit var rvPuntosRuta: RecyclerView
//    private lateinit var btnVolver: MaterialButton
//
//    // Nuevos elementos para evidencias
//    private lateinit var cardEvidencias: MaterialCardView
//    private lateinit var txtSinEvidencias: TextView
//    private lateinit var rvEvidenciasPuntos: RecyclerView
//
//    private val puntosRuta = mutableListOf<VerPuntosRuta.PuntoConOrden>()
//    private lateinit var puntosAdapter: VerPuntosAdapter
//
//    // Adaptador para evidencias agrupadas por punto
//    private val evidenciasPuntos = mutableListOf<EvidenciaPunto>()
//    private lateinit var evidenciasAdapter: EvidenciasPorPuntoAdapter
//
//    companion object {
//        fun newInstance(asignacionId: String): DetalleAsignacion {
//            val fragment = DetalleAsignacion()
//            val args = Bundle()
//            args.putString("asignacionId", asignacionId)
//            fragment.arguments = args
//            return fragment
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        asignacionId = arguments?.getString("asignacionId") ?: ""
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_detalle_asignacion, container, false)
//
//        db = FirebaseFirestore.getInstance()
//
//        initViews(view)
//        setupRecyclerViews()
//        cargarDatosAsignacion()
//
//        btnVolver.setOnClickListener {
//            requireActivity().supportFragmentManager.popBackStack()
//        }
//
//        return view
//    }
//
//    private fun initViews(view: View) {
//        txtRutaNombre = view.findViewById(R.id.txtRutaNombre)
//        txtRutaCodigo = view.findViewById(R.id.txtRutaCodigo)
//        txtEstado = view.findViewById(R.id.txtEstado)
//        txtFechaAsignacion = view.findViewById(R.id.txtFechaAsignacion)
//        txtFechaEjecucion = view.findViewById(R.id.txtFechaEjecucion)
//        txtVehiculo = view.findViewById(R.id.txtVehiculo)
//        txtConductor = view.findViewById(R.id.txtConductor)
//        txtAyudantes = view.findViewById(R.id.txtAyudantes)
//        txtObservaciones = view.findViewById(R.id.txtObservaciones)
//        rvPuntosRuta = view.findViewById(R.id.rvPuntosRuta)
//        btnVolver = view.findViewById(R.id.btnVolver)
//
//        // Nuevos elementos
//        cardEvidencias = view.findViewById(R.id.cardEvidencias)
//        txtSinEvidencias = view.findViewById(R.id.txtSinEvidencias)
//        rvEvidenciasPuntos = view.findViewById(R.id.rvEvidenciasPuntos)
//    }
//
//    private fun setupRecyclerViews() {
//        // RecyclerView de puntos
//        puntosAdapter = VerPuntosAdapter(puntosRuta)
//        rvPuntosRuta.layoutManager = LinearLayoutManager(requireContext())
//        rvPuntosRuta.adapter = puntosAdapter
//
//        // RecyclerView de evidencias por punto
//        evidenciasAdapter = EvidenciasPorPuntoAdapter(evidenciasPuntos) { fotoUrl ->
//            // Abrir imagen en pantalla completa
//            mostrarImagenCompleta(fotoUrl)
//        }
//        rvEvidenciasPuntos.layoutManager = LinearLayoutManager(requireContext())
//        rvEvidenciasPuntos.adapter = evidenciasAdapter
//    }
//
//    private fun cargarDatosAsignacion() {
//        db.collection("asignaciones_rutas")
//            .document(asignacionId)
//            .get()
//            .addOnSuccessListener { document ->
//                if (document.exists()) {
//                    val asignacion = document.toObject(entAsignacionRuta::class.java)
//                    asignacion?.let {
//                        mostrarDatos(it)
//                        // Cargar evidencias solo si el estado lo permite
//                        if (it.estado in listOf("En Progreso", "Completada")) {
//                            cargarEvidencias()
//                        } else {
//                            cardEvidencias.visibility = View.GONE
//                        }
//                    }
//                } else {
//                    Toast.makeText(requireContext(), "Asignación no encontrada", Toast.LENGTH_SHORT).show()
//                    requireActivity().supportFragmentManager.popBackStack()
//                }
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(
//                    requireContext(),
//                    "Error al cargar datos: ${e.message}",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//    }
//
//    private fun mostrarDatos(asignacion: entAsignacionRuta) {
//        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
//
//        txtRutaNombre.text = asignacion.rutaNombre
//        txtRutaCodigo.text = asignacion.rutaCodigo
//        txtEstado.text = asignacion.estado
//
//        // Color del estado
//        when (asignacion.estado) {
//            "Programada" -> txtEstado.setBackgroundColor(
//                android.graphics.Color.parseColor("#2196F3")
//            )
//            "En Progreso" -> txtEstado.setBackgroundColor(
//                android.graphics.Color.parseColor("#FF9800")
//            )
//            "Completada" -> txtEstado.setBackgroundColor(
//                android.graphics.Color.parseColor("#4CAF50")
//            )
//            "Cancelada" -> txtEstado.setBackgroundColor(
//                android.graphics.Color.parseColor("#F44336")
//            )
//        }
//
//        txtFechaAsignacion.text = sdf.format(asignacion.fechaAsignacion)
//
//        if (asignacion.fechaEjecucion != null) {
//            txtFechaEjecucion.text = sdf.format(asignacion.fechaEjecucion)
//            txtFechaEjecucion.visibility = View.VISIBLE
//        } else {
//            txtFechaEjecucion.visibility = View.GONE
//        }
//
//        txtVehiculo.text = asignacion.vehiculoPlaca
//        txtConductor.text = asignacion.conductorNombre
//
//        if (asignacion.ayudantesNombres.isEmpty()) {
//            txtAyudantes.text = "Sin ayudantes"
//        } else {
//            txtAyudantes.text = asignacion.ayudantesNombres.joinToString("\n• ", prefix = "• ")
//        }
//
//        if (asignacion.observaciones.isNotEmpty()) {
//            txtObservaciones.text = asignacion.observaciones
//            txtObservaciones.visibility = View.VISIBLE
//        } else {
//            txtObservaciones.visibility = View.GONE
//        }
//
//        // Cargar puntos de la ruta
//        cargarPuntosRuta(asignacion.rutaId)
//    }
//
//    private fun cargarPuntosRuta(rutaId: String) {
//        db.collection("ruta_puntos")
//            .whereEqualTo("rutaId", rutaId)
//            .orderBy("orden")
//            .get()
//            .addOnSuccessListener { rutaPuntos ->
//                if (rutaPuntos.isEmpty) {
//                    return@addOnSuccessListener
//                }
//
//                puntosRuta.clear()
//                var puntosRestantes = rutaPuntos.size()
//
//                for (rutaPunto in rutaPuntos) {
//                    val puntoId = rutaPunto.getString("puntoId") ?: ""
//                    val orden = rutaPunto.getLong("orden")?.toInt() ?: 0
//                    val fechaAsignacion = rutaPunto.getLong("fechaAsignacion") ?: 0L
//
//                    db.collection("puntos_recoleccion")
//                        .document(puntoId)
//                        .get()
//                        .addOnSuccessListener { puntoDoc ->
//                            if (puntoDoc.exists()) {
//                                val punto = puntoDoc.toObject(entPuntoRecoleccion::class.java)
//                                if (punto != null) {
//                                    puntosRuta.add(VerPuntosRuta.PuntoConOrden(punto, orden, fechaAsignacion))
//                                }
//                            }
//
//                            puntosRestantes--
//                            if (puntosRestantes == 0) {
//                                puntosRuta.sortBy { it.orden }
//                                puntosAdapter.notifyDataSetChanged()
//                            }
//                        }
//                }
//            }
//    }
//
//    /**
//     * Carga las evidencias fotográficas de todos los puntos de esta asignación
//     */
//    private fun cargarEvidencias() {
//        db.collection("evidencias_recoleccion")
//            .whereEqualTo("asignacionId", asignacionId)
//            .orderBy("orden")
//            .get()
//            .addOnSuccessListener { evidencias ->
//                if (evidencias.isEmpty) {
//                    // No hay evidencias aún
//                    cardEvidencias.visibility = View.VISIBLE
//                    txtSinEvidencias.visibility = View.VISIBLE
//                    rvEvidenciasPuntos.visibility = View.GONE
//                    return@addOnSuccessListener
//                }
//
//                evidenciasPuntos.clear()
//                var evidenciasRestantes = evidencias.size()
//
//                for (evidenciaDoc in evidencias) {
//                    val puntoId = evidenciaDoc.getString("puntoId") ?: ""
//                    val orden = evidenciaDoc.getLong("orden")?.toInt() ?: 0
//                    val fotos = evidenciaDoc.get("fotos") as? List<String> ?: emptyList()
//                    val fechaRecoleccion = evidenciaDoc.getLong("fechaRecoleccion") ?: 0L
//
//                    // Obtener datos del punto
//                    db.collection("puntos_recoleccion")
//                        .document(puntoId)
//                        .get()
//                        .addOnSuccessListener { puntoDoc ->
//                            if (puntoDoc.exists()) {
//                                val nombrePunto = puntoDoc.getString("nombre") ?: "Sin nombre"
//                                val direccionPunto = puntoDoc.getString("direccion") ?: ""
//
//                                evidenciasPuntos.add(
//                                    EvidenciaPunto(
//                                        puntoId = puntoId,
//                                        orden = orden,
//                                        nombrePunto = nombrePunto,
//                                        direccionPunto = direccionPunto,
//                                        fotos = fotos,
//                                        fechaRecoleccion = fechaRecoleccion
//                                    )
//                                )
//                            }
//
//                            evidenciasRestantes--
//                            if (evidenciasRestantes == 0) {
//                                evidenciasPuntos.sortBy { it.orden }
//
//                                if (evidenciasPuntos.isEmpty()) {
//                                    txtSinEvidencias.visibility = View.VISIBLE
//                                    rvEvidenciasPuntos.visibility = View.GONE
//                                } else {
//                                    txtSinEvidencias.visibility = View.GONE
//                                    rvEvidenciasPuntos.visibility = View.VISIBLE
//                                    evidenciasAdapter.notifyDataSetChanged()
//                                }
//
//                                cardEvidencias.visibility = View.VISIBLE
//                            }
//                        }
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.e("DetalleAsignacion", "Error al cargar evidencias: ${e.message}")
//                cardEvidencias.visibility = View.GONE
//            }
//    }
//
//    /**
//     * Muestra una imagen en pantalla completa
//     */
//    private fun mostrarImagenCompleta(fotoUrl: String) {
//        val dialogView = LayoutInflater.from(requireContext())
//            .inflate(R.layout.dialog_imagen_completa, null)
//
//        val imgCompleta = dialogView.findViewById<ImageView>(R.id.imgCompleta)
//
//        Picasso.get()
//            .load(fotoUrl)
//            .into(imgCompleta)
//
//        AlertDialog.Builder(requireContext())
//            .setView(dialogView)
//            .setPositiveButton("Cerrar", null)
//            .show()
//    }
//
//    /**
//     * Data class para agrupar evidencias por punto
//     */
//    data class EvidenciaPunto(
//        val puntoId: String,
//        val orden: Int,
//        val nombrePunto: String,
//        val direccionPunto: String,
//        val fotos: List<String>,
//        val fechaRecoleccion: Long
//    )
//}

//    private lateinit var db: FirebaseFirestore
//    private lateinit var asignacionId: String
//
//    private lateinit var txtRutaNombre: TextView
//    private lateinit var txtRutaCodigo: TextView
//    private lateinit var txtEstado: TextView
//    private lateinit var txtFechaAsignacion: TextView
//    private lateinit var txtFechaEjecucion: TextView
//    private lateinit var txtVehiculo: TextView
//    private lateinit var txtConductor: TextView
//    private lateinit var txtAyudantes: TextView
//    private lateinit var txtObservaciones: TextView
//    private lateinit var rvPuntosRuta: RecyclerView
//    private lateinit var btnVolver: MaterialButton
//
//    private val puntosRuta = mutableListOf<VerPuntosRuta.PuntoConOrden>()
//    private lateinit var puntosAdapter: VerPuntosAdapter
//
//    companion object {
//        fun newInstance(asignacionId: String): DetalleAsignacion {
//            val fragment = DetalleAsignacion()
//            val args = Bundle()
//            args.putString("asignacionId", asignacionId)
//            fragment.arguments = args
//            return fragment
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        asignacionId = arguments?.getString("asignacionId") ?: ""
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        val view = inflater.inflate(R.layout.fragment_detalle_asignacion, container, false)
//
//        db = FirebaseFirestore.getInstance()
//
//        initViews(view)
//        setupRecyclerView()
//        cargarDatosAsignacion()
//
//        btnVolver.setOnClickListener {
//            requireActivity().supportFragmentManager.popBackStack()
//        }
//
//        return view
//    }
//
//    private fun initViews(view: View) {
//        txtRutaNombre = view.findViewById(R.id.txtRutaNombre)
//        txtRutaCodigo = view.findViewById(R.id.txtRutaCodigo)
//        txtEstado = view.findViewById(R.id.txtEstado)
//        txtFechaAsignacion = view.findViewById(R.id.txtFechaAsignacion)
//        txtFechaEjecucion = view.findViewById(R.id.txtFechaEjecucion)
//        txtVehiculo = view.findViewById(R.id.txtVehiculo)
//        txtConductor = view.findViewById(R.id.txtConductor)
//        txtAyudantes = view.findViewById(R.id.txtAyudantes)
//        txtObservaciones = view.findViewById(R.id.txtObservaciones)
//        rvPuntosRuta = view.findViewById(R.id.rvPuntosRuta)
//        btnVolver = view.findViewById(R.id.btnVolver)
//    }
//
//    private fun setupRecyclerView() {
//        puntosAdapter = VerPuntosAdapter(puntosRuta)
//        rvPuntosRuta.layoutManager = LinearLayoutManager(requireContext())
//        rvPuntosRuta.adapter = puntosAdapter
//    }
//
//    private fun cargarDatosAsignacion() {
//        db.collection("asignaciones_rutas")
//            .document(asignacionId)
//            .get()
//            .addOnSuccessListener { document ->
//                if (document.exists()) {
//                    val asignacion = document.toObject(entAsignacionRuta::class.java)
//                    asignacion?.let { mostrarDatos(it) }
//                } else {
//                    Toast.makeText(requireContext(), "Asignación no encontrada", Toast.LENGTH_SHORT).show()
//                    requireActivity().supportFragmentManager.popBackStack()
//                }
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(
//                    requireContext(),
//                    "Error al cargar datos: ${e.message}",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//    }
//
//    private fun mostrarDatos(asignacion: entAsignacionRuta) {
//        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
//
//        txtRutaNombre.text = asignacion.rutaNombre
//        txtRutaCodigo.text = asignacion.rutaCodigo
//        txtEstado.text = asignacion.estado
//
//        // Color del estado
//        when (asignacion.estado) {
//            "Programada" -> txtEstado.setBackgroundColor(
//                android.graphics.Color.parseColor("#2196F3")
//            )
//            "En Progreso" -> txtEstado.setBackgroundColor(
//                android.graphics.Color.parseColor("#FF9800")
//            )
//            "Completada" -> txtEstado.setBackgroundColor(
//                android.graphics.Color.parseColor("#4CAF50")
//            )
//            "Cancelada" -> txtEstado.setBackgroundColor(
//                android.graphics.Color.parseColor("#F44336")
//            )
//        }
//
//        txtFechaAsignacion.text = sdf.format(asignacion.fechaAsignacion)
//
//        if (asignacion.fechaEjecucion != null) {
//            txtFechaEjecucion.text = sdf.format(asignacion.fechaEjecucion)
//            txtFechaEjecucion.visibility = View.VISIBLE
//        } else {
//            txtFechaEjecucion.visibility = View.GONE
//        }
//
//        txtVehiculo.text = asignacion.vehiculoPlaca
//        txtConductor.text = asignacion.conductorNombre
//
//        if (asignacion.ayudantesNombres.isEmpty()) {
//            txtAyudantes.text = "Sin ayudantes"
//        } else {
//            txtAyudantes.text = asignacion.ayudantesNombres.joinToString("\n• ", prefix = "• ")
//        }
//
//        if (asignacion.observaciones.isNotEmpty()) {
//            txtObservaciones.text = asignacion.observaciones
//            txtObservaciones.visibility = View.VISIBLE
//        } else {
//            txtObservaciones.visibility = View.GONE
//        }
//
//        // Cargar puntos de la ruta
//        cargarPuntosRuta(asignacion.rutaId)
//    }
//
//    private fun cargarPuntosRuta(rutaId: String) {
//        db.collection("ruta_puntos")
//            .whereEqualTo("rutaId", rutaId)
//            .orderBy("orden")
//            .get()
//            .addOnSuccessListener { rutaPuntos ->
//                if (rutaPuntos.isEmpty) {
//                    return@addOnSuccessListener
//                }
//
//                puntosRuta.clear()
//                var puntosRestantes = rutaPuntos.size()
//
//                for (rutaPunto in rutaPuntos) {
//                    val puntoId = rutaPunto.getString("puntoId") ?: ""
//                    val orden = rutaPunto.getLong("orden")?.toInt() ?: 0
//                    val fechaAsignacion = rutaPunto.getLong("fechaAsignacion") ?: 0L
//
//                    db.collection("puntos_recoleccion")
//                        .document(puntoId)
//                        .get()
//                        .addOnSuccessListener { puntoDoc ->
//                            if (puntoDoc.exists()) {
//                                val punto = puntoDoc.toObject(entPuntoRecoleccion::class.java)
//                                if (punto != null) {
//                                    puntosRuta.add(VerPuntosRuta.PuntoConOrden(punto, orden, fechaAsignacion))
//                                }
//                            }
//
//                            puntosRestantes--
//                            if (puntosRestantes == 0) {
//                                puntosRuta.sortBy { it.orden }
//                                puntosAdapter.notifyDataSetChanged()
//                            }
//                        }
//                }
//            }
//    }
