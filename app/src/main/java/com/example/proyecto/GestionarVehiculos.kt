package com.example.proyecto

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.Adapter.VehiculoAdapter
import com.example.proyecto.Entidad.entVehiculo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"

///**
// * A simple [Fragment] subclass.
// * Use the [GestionarVehiculos.newInstance] factory method to
// * create an instance of this fragment.
// */
class GestionarVehiculos : Fragment() {

//    private lateinit var etBuscar: EditText
//    private lateinit var spTipoVehiculo: Spinner
//    private lateinit var spEstado: Spinner
//    private lateinit var rvVehiculos: RecyclerView
//    private lateinit var llSinDatos: View

    private lateinit var db: FirebaseFirestore
    private lateinit var etBuscar: EditText
    private lateinit var spTipoVehiculo: Spinner
    private lateinit var spEstado: Spinner
    private lateinit var rvVehiculos: RecyclerView
    private lateinit var llSinDatos: View

    private val vehiculosList = mutableListOf<entVehiculo>()
    private val vehiculosFiltrados = mutableListOf<entVehiculo>()
    private lateinit var adapter: VehiculoAdapter
    private var listenerRegistration: ListenerRegistration? = null

//    private var param1: String? = null
//    private var param2: String? = null

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_gestionar_vehiculos, container, false)

        db = FirebaseFirestore.getInstance()
        initViews(view)
        setupSpinners()
        setupRecyclerView()
        setupBusqueda()
        cargarVehiculos()

        return view
    }

    private fun initViews(view: View) {
        etBuscar = view.findViewById(R.id.etBuscar)
        spTipoVehiculo = view.findViewById(R.id.spTipoVehiculo)
        spEstado = view.findViewById(R.id.spEstado)
        rvVehiculos = view.findViewById(R.id.rvVehiculos)
        llSinDatos = view.findViewById(R.id.llSinDatos)
    }

    private fun setupSpinners() {
        // Tipo de vehículo
        val tipos = arrayOf("Todos", "Triciclo", "Motocicleta", "Camión Pequeño", "Camión Grande")
        val adapterTipos =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipoVehiculo.adapter = adapterTipos

        // AÑADIDO: Listener para detectar cambios en el spinner de tipo de vehículo
        spTipoVehiculo.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarVehiculos() // Llama al filtro cada vez que se selecciona algo
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Estado
        val estados = arrayOf("Todos", "Disponible", "En Ruta", "Mantenimiento", "Inactivo")
        val adapterEstados = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapterEstados

        // AÑADIDO: Listener para detectar cambios en el spinner de estado
        spEstado.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarVehiculos() // Llama al filtro cada vez que se selecciona algo
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
//        rvVehiculos.layoutManager = LinearLayoutManager(requireContext())
//        // TODO: Configurar adapter cuando se conecte con Firebase
//
//        llSinDatos.visibility = View.VISIBLE
        adapter = VehiculoAdapter(
            vehiculosFiltrados,
            onMantenimiento = { vehiculo ->
                abrirMantenimiento(vehiculo)
            },
            onEditar = { vehiculo ->
                abrirEdicion(vehiculo)
            },
            onHistorial = { vehiculo -> // <- Agregar este parámetro
                abrirHistorial(vehiculo)
            }
        )

        rvVehiculos.layoutManager = LinearLayoutManager(requireContext())
        rvVehiculos.adapter = adapter
    }

    private fun setupBusqueda() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarVehiculos()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun abrirMantenimiento(vehiculo: entVehiculo) {
        val fragment = MantenimientoVehiculo.newInstance(vehiculo)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirHistorial(vehiculo: entVehiculo) {
        val fragment = HistorialMantenimiento.newInstance(vehiculo)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun abrirEdicion(vehiculo: entVehiculo) {
        val fragment = EditarVehiculo.newInstance(vehiculo)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun cargarVehiculos() {
        listenerRegistration = db.collection("vehiculos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar vehículos: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    vehiculosList.clear()
                    for (document in snapshot.documents) {
                        val vehiculo = document.toObject(entVehiculo::class.java)
                        if (vehiculo != null) {
                            vehiculosList.add(vehiculo)
                        }
                    }
                    filtrarVehiculos()
                } else {
                    vehiculosList.clear()
                    filtrarVehiculos()
                }
            }
    }

    private fun filtrarVehiculos() {
        val busqueda = etBuscar.text.toString().lowercase()
        val tipoSeleccionado = spTipoVehiculo.selectedItem.toString()
        val estadoSeleccionado = spEstado.selectedItem.toString()

        vehiculosFiltrados.clear()

        for (vehiculo in vehiculosList) {
            // Filtro de búsqueda
            val coincideBusqueda = busqueda.isEmpty() ||
                    vehiculo.placa.lowercase().contains(busqueda) ||
                    vehiculo.tipoVehiculo.lowercase().contains(busqueda) ||
                    vehiculo.observaciones.lowercase().contains(busqueda)

            // Filtro de tipo
            val coincideTipo = tipoSeleccionado == "Todos" || vehiculo.tipoVehiculo == tipoSeleccionado

            // Filtro de estado
            val coincideEstado = estadoSeleccionado == "Todos" || vehiculo.estado == estadoSeleccionado

            if (coincideBusqueda && coincideTipo && coincideEstado) {
                vehiculosFiltrados.add(vehiculo)
            }
        }

        // Actualizar vista
        if (vehiculosFiltrados.isEmpty()) {
            rvVehiculos.visibility = View.GONE
            llSinDatos.visibility = View.VISIBLE
        } else {
            rvVehiculos.visibility = View.VISIBLE
            llSinDatos.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }

//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment GestionarVehiculos.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            GestionarVehiculos().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
}