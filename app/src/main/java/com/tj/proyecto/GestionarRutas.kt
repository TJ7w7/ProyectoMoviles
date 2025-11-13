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
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Adapter.RutasAdapter
import com.tj.proyecto.Entidad.entRuta
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class GestionarRutas : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var etBuscar: EditText
    private lateinit var spZona: Spinner
    private lateinit var spEstado: Spinner
    private lateinit var rvRutas: RecyclerView
    private lateinit var llSinDatos: View

    private val rutasList = mutableListOf<entRuta>()
    private val rutasFiltradas = mutableListOf<entRuta>()
    private lateinit var rutasAdapter: RutasAdapter
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gestionar_rutas, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupSpinners()
        setupRecyclerView()
        setupBusqueda()
        cargarRutas()

        return view
    }

    private fun initViews(view: View) {
        etBuscar = view.findViewById(R.id.etBuscar)
        spZona = view.findViewById(R.id.spZona)
        spEstado = view.findViewById(R.id.spEstado)
        rvRutas = view.findViewById(R.id.rvRutas)
        llSinDatos = view.findViewById(R.id.llSinDatos)
    }

    private fun setupSpinners() {
        // Zonas
        val zonas = arrayOf("Todas las zonas", "Centro", "Norte", "Sur", "Este", "Oeste")
        val adapterZonas =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, zonas)
        adapterZonas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spZona.adapter = adapterZonas

        // Estados
        val estados = arrayOf("Todas", "Activa", "Inactiva", "En Revisión")
        val adapterEstados = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapterEstados

        // Listeners para filtros
        spZona.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarRutas()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarRutas()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        rutasAdapter = RutasAdapter(
            rutasFiltradas,
            onVerPuntos = { ruta -> verPuntosRuta(ruta) },
            onEditar = { ruta -> editarRuta(ruta) }
        )
        rvRutas.layoutManager = LinearLayoutManager(requireContext())
        rvRutas.adapter = rutasAdapter
    }

    private fun setupBusqueda() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarRutas()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun cargarRutas() {
        // Usar snapshot listener para actualizaciones en tiempo real
        listenerRegistration = db.collection("rutas")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar rutas: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                rutasList.clear()
                if (snapshots != null) {
                    for (document in snapshots) {
                        val ruta = document.toObject(entRuta::class.java)
                        rutasList.add(ruta)
                    }
                }

                // Cargar cantidad de puntos para cada ruta
                cargarPuntosAsignados()
            }
    }

    private fun cargarPuntosAsignados() {
        // Cargar la cantidad de puntos asignados a cada ruta
        for (ruta in rutasList) {
            db.collection("ruta_puntos")
                .whereEqualTo("rutaId", ruta.id)
                .get()
                .addOnSuccessListener { documents ->
                    ruta.cantidadPuntos = documents.size()
                    filtrarRutas()
                }
        }
        filtrarRutas()
    }

    private fun filtrarRutas() {
        val busqueda = etBuscar.text.toString().lowercase()
        val zonaSeleccionada = spZona.selectedItem.toString()
        val estadoSeleccionado = spEstado.selectedItem.toString()

        rutasFiltradas.clear()

        for (ruta in rutasList) {
            val coincideBusqueda = busqueda.isEmpty() ||
                    ruta.nombre.lowercase().contains(busqueda) ||
                    ruta.codigo.lowercase().contains(busqueda)

            val coincideZona = zonaSeleccionada == "Todas las zonas" ||
                    ruta.zona == zonaSeleccionada

            val coincideEstado = estadoSeleccionado == "Todas" ||
                    ruta.estado == estadoSeleccionado

            if (coincideBusqueda && coincideZona && coincideEstado) {
                rutasFiltradas.add(ruta)
            }
        }

        rutasAdapter.notifyDataSetChanged()

        // Mostrar/ocultar mensaje de sin datos
        if (rutasFiltradas.isEmpty()) {
            llSinDatos.visibility = View.VISIBLE
            rvRutas.visibility = View.GONE
        } else {
            llSinDatos.visibility = View.GONE
            rvRutas.visibility = View.VISIBLE
        }
    }

    private fun verPuntosRuta(ruta: entRuta) {
        // Navegar al fragmento de ver/editar puntos de la ruta
        val fragment = VerPuntosRuta.newInstance(ruta.id, ruta.nombre)
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun editarRuta(ruta: entRuta) {
        // Navegar al fragmento de edición
        val fragment = EditarRuta.newInstance(ruta.id)
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remover el listener cuando se destruya la vista
        listenerRegistration?.remove()
    }
}