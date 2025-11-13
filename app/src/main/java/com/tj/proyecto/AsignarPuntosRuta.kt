package com.tj.proyecto

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Adapter.PuntosAsignadosAdapter
import com.tj.proyecto.Adapter.PuntosDisponiblesAdapter
import com.tj.proyecto.Entidad.entPuntoRecoleccion
import com.tj.proyecto.Entidad.entRutaPunto
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
class AsignarPuntosRuta : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var rutaId: String
    private lateinit var nombreRuta: String

    private lateinit var txtNombreRuta: TextView
    private lateinit var etBuscarPunto: EditText
    private lateinit var spFiltroZona: Spinner
    private lateinit var rvPuntosDisponibles: RecyclerView
    private lateinit var rvPuntosAsignados: RecyclerView
    private lateinit var lblPuntosAsignados: TextView
    private lateinit var txtContadorDisponibles: TextView
    private lateinit var btnGuardarAsignacion: MaterialButton
    private lateinit var txtSinPuntos: TextView
    private lateinit var layoutSinPuntos: View
    private lateinit var layoutSinAsignados: View

    private val puntosDisponiblesList = mutableListOf<entPuntoRecoleccion>()
    private val puntosDisponiblesFiltrados = mutableListOf<entPuntoRecoleccion>()
    private val puntosAsignados = mutableListOf<entPuntoRecoleccion>()

    private lateinit var adapterDisponibles: PuntosDisponiblesAdapter
    private lateinit var adapterAsignados: PuntosAsignadosAdapter

    companion object {
        fun newInstance(rutaId: String, nombreRuta: String): AsignarPuntosRuta {
            val fragment = AsignarPuntosRuta()
            val args = Bundle()
            args.putString("rutaId", rutaId)
            args.putString("nombreRuta", nombreRuta)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rutaId = arguments?.getString("rutaId") ?: ""
        nombreRuta = arguments?.getString("nombreRuta") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_asignar_puntos_ruta, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecyclerViews()
        setupSpinner()
        setupBusqueda()
        cargarPuntosDisponibles()
        cargarPuntosYaAsignados()

        btnGuardarAsignacion.setOnClickListener {
            guardarAsignacion()
        }

        return view
    }

    private fun initViews(view: View) {
        txtNombreRuta = view.findViewById(R.id.txtNombreRuta)
        etBuscarPunto = view.findViewById(R.id.etBuscarPunto)
        spFiltroZona = view.findViewById(R.id.spFiltroZona)
        rvPuntosDisponibles = view.findViewById(R.id.rvPuntosDisponibles)
        rvPuntosAsignados = view.findViewById(R.id.rvPuntosAsignados)
        lblPuntosAsignados = view.findViewById(R.id.lblPuntosAsignados)
        btnGuardarAsignacion = view.findViewById(R.id.btnGuardarAsignacion)
        txtContadorDisponibles = view.findViewById(R.id.txtContadorDisponibles)
        txtSinPuntos = view.findViewById(R.id.txtSinPuntos)
        layoutSinPuntos = view.findViewById(R.id.layoutSinPuntos)
        layoutSinAsignados = view.findViewById(R.id.layoutSinAsignados)

        txtNombreRuta.text = nombreRuta
        actualizarEstadoBoton()
    }

    private fun setupRecyclerViews() {
        // RecyclerView de puntos disponibles
        adapterDisponibles = PuntosDisponiblesAdapter(puntosDisponiblesFiltrados) { punto ->
            agregarPunto(punto)
        }
        rvPuntosDisponibles.layoutManager = LinearLayoutManager(requireContext())
        rvPuntosDisponibles.adapter = adapterDisponibles

        // RecyclerView de puntos asignados
        adapterAsignados = PuntosAsignadosAdapter(
            puntosAsignados,
            onSubir = { position -> moverPunto(position, -1) },
            onBajar = { position -> moverPunto(position, 1) },
            onQuitar = { position -> quitarPunto(position) }
        )
        rvPuntosAsignados.layoutManager = LinearLayoutManager(requireContext())
        rvPuntosAsignados.adapter = adapterAsignados
    }

    private fun setupSpinner() {
        val zonas = arrayOf("Todas las zonas", "Centro", "Norte", "Sur", "Este", "Oeste")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, zonas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFiltroZona.adapter = adapter

        spFiltroZona.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarPuntos()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupBusqueda() {
        etBuscarPunto.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarPuntos()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun cargarPuntosDisponibles() {
        db.collection("puntos_recoleccion")
            .whereEqualTo("estado", true)
            .get()
            .addOnSuccessListener { documents ->
                puntosDisponiblesList.clear()
                for (document in documents) {
                    val punto = document.toObject(entPuntoRecoleccion::class.java)
                    puntosDisponiblesList.add(punto)
                }

                if (puntosDisponiblesList.isEmpty()) {
                    txtSinPuntos.visibility = View.VISIBLE
                    rvPuntosDisponibles.visibility = View.GONE
                } else {
                    txtSinPuntos.visibility = View.GONE
                    rvPuntosDisponibles.visibility = View.VISIBLE
                    filtrarPuntos()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al cargar puntos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // Agregar después de cargarPuntosDisponibles()
    private fun cargarPuntosYaAsignados() {
        db.collection("ruta_puntos")
            .whereEqualTo("rutaId", rutaId)
            .orderBy("orden")
            .get()
            .addOnSuccessListener { rutaPuntos ->
                if (rutaPuntos.isEmpty) {
                    return@addOnSuccessListener
                }

                var puntosRestantes = rutaPuntos.size()

                for (rutaPunto in rutaPuntos) {
                    val puntoId = rutaPunto.getString("puntoId") ?: ""

                    db.collection("puntos_recoleccion")
                        .document(puntoId)
                        .get()
                        .addOnSuccessListener { puntoDoc ->
                            if (puntoDoc.exists()) {
                                val punto = puntoDoc.toObject(entPuntoRecoleccion::class.java)
                                if (punto != null && punto.estado) {
                                    puntosAsignados.add(punto)
                                }
                            }

                            puntosRestantes--
                            if (puntosRestantes == 0) {
                                actualizarListaAsignados()
                                filtrarPuntos()
                            }
                        }
                }
            }
    }

    private fun filtrarPuntos() {
        val busqueda = etBuscarPunto.text.toString().lowercase()
        val zonaSeleccionada = spFiltroZona.selectedItem.toString()

        puntosDisponiblesFiltrados.clear()

        for (punto in puntosDisponiblesList) {
            // Si ya está asignado, no mostrarlo
            if (puntosAsignados.any { it.id == punto.id }) {
                continue
            }

            val coincideBusqueda = busqueda.isEmpty() ||
                    punto.nombre.lowercase().contains(busqueda) ||
                    punto.direccion.lowercase().contains(busqueda)

            val coincideZona = zonaSeleccionada == "Todas las zonas" || punto.zona == zonaSeleccionada

            if (coincideBusqueda && coincideZona) {
                puntosDisponiblesFiltrados.add(punto)
            }
        }

        adapterDisponibles.notifyDataSetChanged()
        actualizarContadorDisponibles()

        // Mostrar/ocultar mensaje de "sin puntos"
        if (puntosDisponiblesFiltrados.isEmpty()) {
            layoutSinPuntos.visibility = View.VISIBLE
            rvPuntosDisponibles.visibility = View.GONE
        } else {
            layoutSinPuntos.visibility = View.GONE
            rvPuntosDisponibles.visibility = View.VISIBLE
        }
    }

    private fun agregarPunto(punto: entPuntoRecoleccion) {
        puntosAsignados.add(punto)
        actualizarListaAsignados()
        filtrarPuntos()  // Actualizar lista de disponibles

        rvPuntosAsignados.smoothScrollToPosition(puntosAsignados.size - 1)

        Toast.makeText(requireContext(), "✓ ${punto.nombre} agregado", Toast.LENGTH_SHORT).show()
    }

    private fun quitarPunto(position: Int) {
        val punto = puntosAsignados[position]
        puntosAsignados.removeAt(position)
        actualizarListaAsignados()
        filtrarPuntos()

        Toast.makeText(requireContext(), "${punto.nombre} removido", Toast.LENGTH_SHORT).show()
    }

    private fun moverPunto(position: Int, direccion: Int) {
        val nuevaPosicion = position + direccion
        if (nuevaPosicion >= 0 && nuevaPosicion < puntosAsignados.size) {
            val punto = puntosAsignados.removeAt(position)
            puntosAsignados.add(nuevaPosicion, punto)
            actualizarListaAsignados()
        }
    }

    private fun actualizarListaAsignados() {
        lblPuntosAsignados.text = "Puntos Asignados (${puntosAsignados.size})"
        adapterAsignados.notifyDataSetChanged()

        // Mostrar/ocultar mensaje de "sin asignados"
        if (puntosAsignados.isEmpty()) {
            layoutSinAsignados.visibility = View.VISIBLE
        } else {
            layoutSinAsignados.visibility = View.GONE
        }

        actualizarEstadoBoton()
    }

    private fun actualizarContadorDisponibles() {
        val total = puntosDisponiblesList.size - puntosAsignados.size
        txtContadorDisponibles.text = "$total puntos"
    }

    private fun actualizarEstadoBoton() {
        if (puntosAsignados.isEmpty()) {
            btnGuardarAsignacion.isEnabled = false
            btnGuardarAsignacion.alpha = 0.5f
        } else {
            btnGuardarAsignacion.isEnabled = true
            btnGuardarAsignacion.alpha = 1.0f
        }
    }

    private fun guardarAsignacion() {
        if (puntosAsignados.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Debe asignar al menos un punto a la ruta",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnGuardarAsignacion.isEnabled = false
        btnGuardarAsignacion.text = "Guardando..."

        // Eliminar asignaciones previas de esta ruta
        db.collection("ruta_puntos")
            .whereEqualTo("rutaId", rutaId)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()

                // Eliminar documentos anteriores
                for (document in documents) {
                    batch.delete(document.reference)
                }

                // Crear nuevas asignaciones
                puntosAsignados.forEachIndexed { index, punto ->
                    val rutaPuntoId = db.collection("ruta_puntos").document().id
                    val rutaPunto = entRutaPunto(
                        id = rutaPuntoId,
                        rutaId = rutaId,
                        puntoId = punto.id,
                        orden = index + 1,
                        fechaAsignacion = System.currentTimeMillis()
                    )

                    val docRef = db.collection("ruta_puntos").document(rutaPuntoId)
                    batch.set(docRef, rutaPunto)
                }

                // Ejecutar batch
                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Puntos asignados exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Volver atrás
//                        requireActivity().supportFragmentManager.popBackStack()
                        val nuevoRegistroFragment = RegistrarRuta()
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, nuevoRegistroFragment)
                            .commit()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error al guardar: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        btnGuardarAsignacion.isEnabled = true
                        btnGuardarAsignacion.text = "Guardar Asignación"
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnGuardarAsignacion.isEnabled = true
                btnGuardarAsignacion.text = "Guardar Asignación"
            }
    }
}