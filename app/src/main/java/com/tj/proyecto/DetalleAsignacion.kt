package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Adapter.VerPuntosAdapter
import com.tj.proyecto.Entidad.entAsignacionRuta
import com.tj.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
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

    private val puntosRuta = mutableListOf<VerPuntosRuta.PuntoConOrden>()
    private lateinit var puntosAdapter: VerPuntosAdapter

    companion object {
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_detalle_asignacion, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecyclerView()
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
    }

    private fun setupRecyclerView() {
        puntosAdapter = VerPuntosAdapter(puntosRuta)
        rvPuntosRuta.layoutManager = LinearLayoutManager(requireContext())
        rvPuntosRuta.adapter = puntosAdapter
    }

    private fun cargarDatosAsignacion() {
        db.collection("asignaciones_rutas")
            .document(asignacionId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val asignacion = document.toObject(entAsignacionRuta::class.java)
                    asignacion?.let { mostrarDatos(it) }
                } else {
                    Toast.makeText(requireContext(), "Asignación no encontrada", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { e ->
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
}