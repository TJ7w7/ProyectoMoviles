package com.tj.proyecto

import android.app.DatePickerDialog
import android.os.Bundle
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
import com.tj.proyecto.Adapter.AyudantesAdapter
import com.tj.proyecto.Entidad.entAsignacionRuta
import com.tj.proyecto.Entidad.entUsuario
import com.tj.proyecto.Entidad.entVehiculo
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditarAsignacion : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var asignacionId: String
    private var asignacionActual: entAsignacionRuta? = null

    private lateinit var etFecha: EditText
    private lateinit var spVehiculo: Spinner
    private lateinit var spConductor: Spinner
    private lateinit var rvAyudantesSeleccionados: RecyclerView
    private lateinit var btnAgregarAyudante: MaterialButton
    private lateinit var etObservaciones: EditText
    private lateinit var btnGuardar: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    private val vehiculosList = mutableListOf<entVehiculo>()
    private val recolectoresList = mutableListOf<entUsuario>()
    private val ayudantesSeleccionados = mutableListOf<entUsuario>()

    private lateinit var ayudantesAdapter: AyudantesAdapter

    private var vehiculoSeleccionado: entVehiculo? = null
    private var conductorSeleccionado: entUsuario? = null
    private var fechaSeleccionada: Long = System.currentTimeMillis()

    companion object {
        fun newInstance(asignacionId: String): EditarAsignacion {
            val fragment = EditarAsignacion()
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
        val view = inflater.inflate(R.layout.fragment_editar_asignacion, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecyclerView()
        setupDatePicker()
        cargarDatos()

        btnAgregarAyudante.setOnClickListener {
            mostrarDialogoSeleccionAyudantes()
        }

        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        etFecha = view.findViewById(R.id.etFecha)
        spVehiculo = view.findViewById(R.id.spVehiculo)
        spConductor = view.findViewById(R.id.spConductor)
        rvAyudantesSeleccionados = view.findViewById(R.id.rvAyudantesSeleccionados)
        btnAgregarAyudante = view.findViewById(R.id.btnAgregarAyudante)
        etObservaciones = view.findViewById(R.id.etObservaciones)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun setupRecyclerView() {
        ayudantesAdapter = AyudantesAdapter(ayudantesSeleccionados) { position ->
            quitarAyudante(position)
        }
        rvAyudantesSeleccionados.layoutManager = LinearLayoutManager(requireContext())
        rvAyudantesSeleccionados.adapter = ayudantesAdapter
    }

    private fun setupDatePicker() {
        etFecha.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = fechaSeleccionada
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val fecha = Calendar.getInstance()
                    fecha.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                    fechaSeleccionada = fecha.timeInMillis

                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    etFecha.setText(sdf.format(fecha.time))
                },
                year, month, day
            )

            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun cargarDatos() {
        // Cargar asignación actual
        db.collection("asignaciones_rutas")
            .document(asignacionId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    asignacionActual = document.toObject(entAsignacionRuta::class.java)
                    cargarVehiculos()
                    cargarRecolectores()
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

    private fun cargarVehiculos() {
        db.collection("vehiculos")
            .whereIn("estado", listOf("Disponible", "Asignado"))
            .get()
            .addOnSuccessListener { documents ->
                vehiculosList.clear()
                vehiculosList.add(entVehiculo(id = "", placa = "Seleccione vehículo"))

                for (document in documents) {
                    val vehiculo = document.toObject(entVehiculo::class.java)
                    vehiculosList.add(vehiculo)
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    vehiculosList.map {
                        if (it.id.isEmpty()) it.placa
                        else "${it.placa} - ${it.tipoVehiculo}"
                    }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spVehiculo.adapter = adapter

                // Seleccionar vehículo actual
                val indexVehiculo = vehiculosList.indexOfFirst { it.id == asignacionActual?.vehiculoId }
                if (indexVehiculo >= 0) {
                    spVehiculo.setSelection(indexVehiculo)
                }

                spVehiculo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        vehiculoSeleccionado = if (position > 0) vehiculosList[position] else null
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
    }

    private fun cargarRecolectores() {
        db.collection("usuarios")
            .whereEqualTo("tipoUsuario", "Recolector")
            .whereEqualTo("estado", true)
            .get()
            .addOnSuccessListener { documents ->
                recolectoresList.clear()
                recolectoresList.add(
                    entUsuario(
                        id = "",
                        nombres = "Seleccione conductor",
                        apellidos = ""
                    )
                )

                for (document in documents) {
                    val usuario = document.toObject(entUsuario::class.java)
                    recolectoresList.add(usuario)
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    recolectoresList.map {
                        if (it.id.isEmpty()) it.nombres
                        else "${it.nombres} ${it.apellidos}"
                    }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spConductor.adapter = adapter

                // Seleccionar conductor actual
                val indexConductor = recolectoresList.indexOfFirst { it.id == asignacionActual?.conductorId }
                if (indexConductor >= 0) {
                    spConductor.setSelection(indexConductor)
                }

                spConductor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        conductorSeleccionado = if (position > 0) recolectoresList[position] else null
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Cargar ayudantes actuales
                cargarAyudantesActuales()
            }
    }

    private fun cargarAyudantesActuales() {
        asignacionActual?.let { asignacion ->
            fechaSeleccionada = asignacion.fechaAsignacion
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            etFecha.setText(sdf.format(fechaSeleccionada))

            etObservaciones.setText(asignacion.observaciones)

            // Cargar ayudantes
            for (ayudanteId in asignacion.ayudantesIds) {
                db.collection("usuarios")
                    .document(ayudanteId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val ayudante = document.toObject(entUsuario::class.java)
                            if (ayudante != null) {
                                ayudantesSeleccionados.add(ayudante)
                                ayudantesAdapter.notifyDataSetChanged()
                            }
                        }
                    }
            }
        }
    }

    private fun mostrarDialogoSeleccionAyudantes() {
        if (conductorSeleccionado == null) {
            Toast.makeText(requireContext(), "Seleccione primero un conductor", Toast.LENGTH_SHORT).show()
            return
        }

        val ayudantesDisponibles = recolectoresList.filter {
            it.id.isNotEmpty() &&
                    it.id != conductorSeleccionado?.id &&
                    !ayudantesSeleccionados.any { ayudante -> ayudante.id == it.id }
        }

        if (ayudantesDisponibles.isEmpty()) {
            Toast.makeText(requireContext(), "No hay ayudantes disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val nombresAyudantes = ayudantesDisponibles.map { "${it.nombres} ${it.apellidos}" }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Ayudante")
            .setItems(nombresAyudantes) { _, which ->
                agregarAyudante(ayudantesDisponibles[which])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarAyudante(ayudante: entUsuario) {
        if (ayudantesSeleccionados.size >= 3) {
            Toast.makeText(requireContext(), "Máximo 3 ayudantes permitidos", Toast.LENGTH_SHORT).show()
            return
        }

        ayudantesSeleccionados.add(ayudante)
        ayudantesAdapter.notifyDataSetChanged()
    }

    private fun quitarAyudante(position: Int) {
        ayudantesSeleccionados.removeAt(position)
        ayudantesAdapter.notifyDataSetChanged()
    }

    private fun guardarCambios() {
        if (vehiculoSeleccionado == null) {
            Toast.makeText(requireContext(), "Seleccione un vehículo", Toast.LENGTH_SHORT).show()
            return
        }

        if (conductorSeleccionado == null) {
            Toast.makeText(requireContext(), "Seleccione un conductor", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        val vehiculoAnterior = asignacionActual?.vehiculoId

        val updates = hashMapOf<String, Any>(
            "vehiculoId" to vehiculoSeleccionado!!.id,
            "vehiculoPlaca" to vehiculoSeleccionado!!.placa,
            "conductorId" to conductorSeleccionado!!.id,
            "conductorNombre" to "${conductorSeleccionado!!.nombres} ${conductorSeleccionado!!.apellidos}",
            "ayudantesIds" to ayudantesSeleccionados.map { it.id },
            "ayudantesNombres" to ayudantesSeleccionados.map { "${it.nombres} ${it.apellidos}" },
            "fechaAsignacion" to fechaSeleccionada,
            "observaciones" to etObservaciones.text.toString().trim()
        )

        db.collection("asignaciones_rutas")
            .document(asignacionId)
            .update(updates)
            .addOnSuccessListener {
                // Actualizar estados de vehículos
                if (vehiculoAnterior != null && vehiculoAnterior != vehiculoSeleccionado!!.id) {
                    // Liberar vehículo anterior
                    db.collection("vehiculos")
                        .document(vehiculoAnterior)
                        .update("estado", "Disponible")

                    // Asignar nuevo vehículo
                    db.collection("vehiculos")
                        .document(vehiculoSeleccionado!!.id)
                        .update("estado", "Asignado")
                }

                Toast.makeText(
                    requireContext(),
                    "✓ Asignación actualizada exitosamente",
                    Toast.LENGTH_SHORT
                ).show()

                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar Cambios"
            }
    }

}