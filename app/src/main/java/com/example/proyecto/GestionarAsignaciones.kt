package com.example.proyecto

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
import com.example.proyecto.Adapter.AsignacionesAdapter
import com.example.proyecto.Entidad.entAsignacionRuta
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class GestionarAsignaciones : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etBuscar: EditText
    private lateinit var spEstado: Spinner
    private lateinit var spFecha: Spinner
    private lateinit var rvAsignaciones: RecyclerView
    private lateinit var llSinDatos: View

    private val asignacionesList = mutableListOf<entAsignacionRuta>()
    private val asignacionesFiltradas = mutableListOf<entAsignacionRuta>()
    private lateinit var asignacionesAdapter: AsignacionesAdapter
    private var listenerRegistration: ListenerRegistration? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_gestionar_asignaciones, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupSpinners()
        setupRecyclerView()
        setupBusqueda()
        cargarAsignaciones()

        return view
    }

    private fun initViews(view: View) {
        etBuscar = view.findViewById(R.id.etBuscar)
        spEstado = view.findViewById(R.id.spEstado)
        spFecha = view.findViewById(R.id.spFecha)
        rvAsignaciones = view.findViewById(R.id.rvAsignaciones)
        llSinDatos = view.findViewById(R.id.llSinDatos)
    }

    private fun setupSpinners() {
        // Estados
        val estados = arrayOf("Todas", "Programada", "En Progreso", "Completada", "Cancelada")
        val adapterEstados = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapterEstados

        // Filtro de fecha
        val fechas = arrayOf("Todas las fechas", "Hoy", "Esta semana", "Este mes")
        val adapterFechas = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fechas)
        adapterFechas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFecha.adapter = adapterFechas

        // Listeners para filtros
        spEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarAsignaciones()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spFecha.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarAsignaciones()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        asignacionesAdapter = AsignacionesAdapter(
            asignacionesFiltradas,
            onVerDetalle = { asignacion -> verDetalleAsignacion(asignacion) },
            onEditar = { asignacion -> editarAsignacion(asignacion) },
            onCambiarEstado = { asignacion -> cambiarEstadoAsignacion(asignacion) },
            onEliminar = { asignacion -> confirmarEliminarAsignacion(asignacion) }
        )
        rvAsignaciones.layoutManager = LinearLayoutManager(requireContext())
        rvAsignaciones.adapter = asignacionesAdapter
    }

    private fun setupBusqueda() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarAsignaciones()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun cargarAsignaciones() {
        listenerRegistration = db.collection("asignaciones_rutas")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar asignaciones: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                asignacionesList.clear()
                if (snapshots != null) {
                    for (document in snapshots) {
                        val asignacion = document.toObject(entAsignacionRuta::class.java)
                        asignacionesList.add(asignacion)
                    }
                }

                // Ordenar por fecha de asignación (más recientes primero)
                asignacionesList.sortByDescending { it.fechaAsignacion }
                filtrarAsignaciones()
            }
    }

    private fun filtrarAsignaciones() {
        val busqueda = etBuscar.text.toString().lowercase()
        val estadoSeleccionado = spEstado.selectedItem.toString()
        val fechaSeleccionada = spFecha.selectedItem.toString()

        asignacionesFiltradas.clear()

        for (asignacion in asignacionesList) {
            val coincideBusqueda = busqueda.isEmpty() ||
                    asignacion.rutaNombre.lowercase().contains(busqueda) ||
                    asignacion.rutaCodigo.lowercase().contains(busqueda) ||
                    asignacion.vehiculoPlaca.lowercase().contains(busqueda) ||
                    asignacion.conductorNombre.lowercase().contains(busqueda)

            val coincideEstado = estadoSeleccionado == "Todas" ||
                    asignacion.estado == estadoSeleccionado

            val coincideFecha = when (fechaSeleccionada) {
                "Todas las fechas" -> true
                "Hoy" -> esHoy(asignacion.fechaAsignacion)
                "Esta semana" -> esEstaSemana(asignacion.fechaAsignacion)
                "Este mes" -> esEsteMes(asignacion.fechaAsignacion)
                else -> true
            }

            if (coincideBusqueda && coincideEstado && coincideFecha) {
                asignacionesFiltradas.add(asignacion)
            }
        }

        asignacionesAdapter.notifyDataSetChanged()

        // Mostrar/ocultar mensaje de sin datos
        if (asignacionesFiltradas.isEmpty()) {
            llSinDatos.visibility = View.VISIBLE
            rvAsignaciones.visibility = View.GONE
        } else {
            llSinDatos.visibility = View.GONE
            rvAsignaciones.visibility = View.VISIBLE
        }
    }

    private fun esHoy(timestamp: Long): Boolean {
        val hoy = java.util.Calendar.getInstance()
        val fecha = java.util.Calendar.getInstance()
        fecha.timeInMillis = timestamp

        return hoy.get(java.util.Calendar.YEAR) == fecha.get(java.util.Calendar.YEAR) &&
                hoy.get(java.util.Calendar.DAY_OF_YEAR) == fecha.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun esEstaSemana(timestamp: Long): Boolean {
        val hoy = java.util.Calendar.getInstance()
        val fecha = java.util.Calendar.getInstance()
        fecha.timeInMillis = timestamp

        return hoy.get(java.util.Calendar.YEAR) == fecha.get(java.util.Calendar.YEAR) &&
                hoy.get(java.util.Calendar.WEEK_OF_YEAR) == fecha.get(java.util.Calendar.WEEK_OF_YEAR)
    }

    private fun esEsteMes(timestamp: Long): Boolean {
        val hoy = java.util.Calendar.getInstance()
        val fecha = java.util.Calendar.getInstance()
        fecha.timeInMillis = timestamp

        return hoy.get(java.util.Calendar.YEAR) == fecha.get(java.util.Calendar.YEAR) &&
                hoy.get(java.util.Calendar.MONTH) == fecha.get(java.util.Calendar.MONTH)
    }

    private fun verDetalleAsignacion(asignacion: entAsignacionRuta) {
        val fragment = DetalleAsignacion.newInstance(asignacion.id)
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

    private fun editarAsignacion(asignacion: entAsignacionRuta) {
        if (asignacion.estado != "Programada") {
            Toast.makeText(
                requireContext(),
                "Solo se pueden editar asignaciones programadas",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val fragment = EditarAsignacion.newInstance(asignacion.id)
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

    private fun cambiarEstadoAsignacion(asignacion: entAsignacionRuta) {
        val estados = when (asignacion.estado) {
            "Programada" -> arrayOf("En Progreso", "Cancelada")
            "En Progreso" -> arrayOf("Completada", "Cancelada")
            else -> {
                Toast.makeText(
                    requireContext(),
                    "No se puede cambiar el estado de esta asignación",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cambiar Estado")
            .setItems(estados) { _, which ->
                val nuevoEstado = estados[which]
                actualizarEstado(asignacion, nuevoEstado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarEstado(asignacion: entAsignacionRuta, nuevoEstado: String) {
        val updates = hashMapOf<String, Any>(
            "estado" to nuevoEstado
        )

        // Si se completa, agregar fecha de ejecución
        if (nuevoEstado == "Completada") {
            updates["fechaEjecucion"] = System.currentTimeMillis()
        }

        db.collection("asignaciones_rutas")
            .document(asignacion.id)
            .update(updates)
            .addOnSuccessListener {
                // Si se completa o cancela, liberar el vehículo
                if (nuevoEstado == "Completada" || nuevoEstado == "Cancelada") {
                    db.collection("vehiculos")
                        .document(asignacion.vehiculoId)
                        .update("estado", "Disponible")
                }

                Toast.makeText(
                    requireContext(),
                    "Estado actualizado a: $nuevoEstado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar estado: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun confirmarEliminarAsignacion(asignacion: entAsignacionRuta) {
        if (asignacion.estado == "En Progreso") {
            Toast.makeText(
                requireContext(),
                "No se puede eliminar una asignación en progreso",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Asignación")
            .setMessage("¿Está seguro de eliminar esta asignación? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarAsignacion(asignacion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarAsignacion(asignacion: entAsignacionRuta) {
        db.collection("asignaciones_rutas")
            .document(asignacion.id)
            .delete()
            .addOnSuccessListener {
                // Liberar el vehículo
                db.collection("vehiculos")
                    .document(asignacion.vehiculoId)
                    .update("estado", "Disponible")

                Toast.makeText(
                    requireContext(),
                    "Asignación eliminada exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al eliminar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }
}