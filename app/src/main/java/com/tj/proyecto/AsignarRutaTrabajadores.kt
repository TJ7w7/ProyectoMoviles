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
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Adapter.AyudantesAdapter
import com.tj.proyecto.Entidad.entAsignacionRuta
import com.tj.proyecto.Entidad.entRuta
import com.tj.proyecto.Entidad.entUsuario
import com.tj.proyecto.Entidad.entVehiculo
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AsignarRutaTrabajadores : Fragment() {
    private lateinit var db: FirebaseFirestore

    private lateinit var spRuta: Spinner
    private lateinit var cardInfoRuta: MaterialCardView
    private lateinit var txtInfoRuta: TextView
    private lateinit var etFecha: EditText
    private lateinit var spVehiculo: Spinner
    private lateinit var spConductor: Spinner
    private lateinit var rvAyudantesSeleccionados: RecyclerView
    private lateinit var btnAgregarAyudante: MaterialButton
    private lateinit var etObservaciones: EditText
    private lateinit var btnAsignar: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    // Listas de datos
    private val rutasList = mutableListOf<entRuta>()
    private val vehiculosList = mutableListOf<entVehiculo>()
    private val recolectoresList = mutableListOf<entUsuario>()
    private val ayudantesSeleccionados = mutableListOf<entUsuario>()

    // Adapters
    private lateinit var ayudantesAdapter: AyudantesAdapter

    // Variables para selección
    private var rutaSeleccionada: entRuta? = null
    private var vehiculoSeleccionado: entVehiculo? = null
    private var conductorSeleccionado: entUsuario? = null
    private var fechaSeleccionada: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_asignar_ruta_trabajadores, container, false)

        db = FirebaseFirestore.getInstance()
        initViews(view)
        setupRecyclerView()
        setupDatePicker()
        cargarDatos()

        btnAgregarAyudante.setOnClickListener {
            mostrarDialogoSeleccionAyudantes()
        }

        btnAsignar.setOnClickListener {
            guardarAsignacion()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        spRuta = view.findViewById(R.id.spRuta)
        cardInfoRuta = view.findViewById(R.id.cardInfoRuta)
        txtInfoRuta = view.findViewById(R.id.txtInfoRuta)
        etFecha = view.findViewById(R.id.etFecha)
        spVehiculo = view.findViewById(R.id.spVehiculo)
        spConductor = view.findViewById(R.id.spConductor)
        rvAyudantesSeleccionados = view.findViewById(R.id.rvAyudantesSeleccionados)
        btnAgregarAyudante = view.findViewById(R.id.btnAgregarAyudante)
        etObservaciones = view.findViewById(R.id.etObservaciones)
        btnAsignar = view.findViewById(R.id.btnAsignar)
        btnCancelar = view.findViewById(R.id.btnCancelar)

        // Establecer fecha actual por defecto
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etFecha.setText(sdf.format(fechaSeleccionada))

        // Ocultar card de info hasta que se seleccione una ruta
        cardInfoRuta.visibility = View.GONE
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

            // No permitir fechas pasadas
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun cargarDatos() {
        cargarRutas()
        cargarVehiculos()
        cargarRecolectores()
    }

    private fun cargarRutas() {
        db.collection("rutas")
            .whereEqualTo("estado", "Activa")
            .get()
            .addOnSuccessListener { documents ->
                rutasList.clear()
                rutasList.add(entRuta(id = "", nombre = "Seleccione una ruta", codigo = ""))

                for (document in documents) {
                    val ruta = document.toObject(entRuta::class.java)
                    rutasList.add(ruta)
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    rutasList.map { if (it.id.isEmpty()) it.nombre else "${it.codigo} - ${it.nombre}" }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spRuta.adapter = adapter

                spRuta.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position > 0) {
                            rutaSeleccionada = rutasList[position]
                            cargarInfoRuta(rutasList[position])
                        } else {
                            rutaSeleccionada = null
                            cardInfoRuta.visibility = View.GONE
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar rutas: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarInfoRuta(ruta: entRuta) {
        // Cargar cantidad de puntos de la ruta
        db.collection("ruta_puntos")
            .whereEqualTo("rutaId", ruta.id)
            .get()
            .addOnSuccessListener { documents ->
                val cantidadPuntos = documents.size()
                val diasTexto = ruta.diasSemana.joinToString(", ")

                txtInfoRuta.text = """
                    📍 Zona: ${ruta.zona}
                    📦 Puntos: $cantidadPuntos puntos de recolección
                    📅 Días: $diasTexto
                    ${if (ruta.descripcion.isNotEmpty()) "\n📝 ${ruta.descripcion}" else ""}
                """.trimIndent()

                cardInfoRuta.visibility = View.VISIBLE
            }
    }

    private fun cargarVehiculos() {
        db.collection("vehiculos")
            .whereEqualTo("estado", "Disponible")
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

                spVehiculo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        vehiculoSeleccionado = if (position > 0) vehiculosList[position] else null
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar vehículos: ${e.message}", Toast.LENGTH_LONG).show()
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

                spConductor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        conductorSeleccionado = if (position > 0) recolectoresList[position] else null
                        // Limpiar ayudantes si se cambia el conductor
                        if (ayudantesSeleccionados.isNotEmpty()) {
                            ayudantesSeleccionados.clear()
                            ayudantesAdapter.notifyDataSetChanged()
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar recolectores: ${e.message}", Toast.LENGTH_LONG).show()
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
                val ayudanteSeleccionado = ayudantesDisponibles[which]
                agregarAyudante(ayudanteSeleccionado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarAyudante(ayudante: entUsuario) {
        if (ayudantesSeleccionados.size >= 3) { // Máximo 3 ayudantes
            Toast.makeText(requireContext(), "Máximo 3 ayudantes permitidos", Toast.LENGTH_SHORT).show()
            return
        }

        ayudantesSeleccionados.add(ayudante)
        ayudantesAdapter.notifyDataSetChanged()

        Toast.makeText(requireContext(), "✓ Ayudante agregado", Toast.LENGTH_SHORT).show()
    }

    private fun quitarAyudante(position: Int) {
        val ayudante = ayudantesSeleccionados[position]
        ayudantesSeleccionados.removeAt(position)
        ayudantesAdapter.notifyDataSetChanged()

        Toast.makeText(requireContext(), "${ayudante.nombres} removido", Toast.LENGTH_SHORT).show()
    }

    private fun validarCampos(): Boolean {
        return when {
            rutaSeleccionada == null -> {
                Toast.makeText(requireContext(), "Seleccione una ruta", Toast.LENGTH_SHORT).show()
                spRuta.requestFocus()
                false
            }
            vehiculoSeleccionado == null -> {
                Toast.makeText(requireContext(), "Seleccione un vehículo", Toast.LENGTH_SHORT).show()
                spVehiculo.requestFocus()
                false
            }
            conductorSeleccionado == null -> {
                Toast.makeText(requireContext(), "Seleccione un conductor", Toast.LENGTH_SHORT).show()
                spConductor.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun guardarAsignacion() {
        if (!validarCampos()) return

        btnAsignar.isEnabled = false
        btnAsignar.text = "Guardando..."

        val asignacionId = db.collection("asignaciones_rutas").document().id

        val asignacion = entAsignacionRuta(
            id = asignacionId,
            rutaId = rutaSeleccionada!!.id,
            rutaCodigo = rutaSeleccionada!!.codigo,
            rutaNombre = rutaSeleccionada!!.nombre,
            vehiculoId = vehiculoSeleccionado!!.id,
            vehiculoPlaca = vehiculoSeleccionado!!.placa,
            conductorId = conductorSeleccionado!!.id,
            conductorNombre = "${conductorSeleccionado!!.nombres} ${conductorSeleccionado!!.apellidos}",
            ayudantesIds = ayudantesSeleccionados.map { it.id },
            ayudantesNombres = ayudantesSeleccionados.map { "${it.nombres} ${it.apellidos}" },
            fechaAsignacion = fechaSeleccionada,
            fechaEjecucion = null,
            estado = "Programada",
            observaciones = etObservaciones.text.toString().trim(),
            fechaRegistro = System.currentTimeMillis()
        )

        db.collection("asignaciones_rutas")
            .document(asignacionId)
            .set(asignacion)
            .addOnSuccessListener {
                // Actualizar estado del vehículo
                db.collection("vehiculos")
                    .document(vehiculoSeleccionado!!.id)
                    .update("estado", "Asignado")
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "✓ Ruta asignada exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navegar de vuelta
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Advertencia: Error al actualizar vehículo: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al guardar asignación: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnAsignar.isEnabled = true
                btnAsignar.text = "Asignar Ruta"
            }
    }
}