package com.tj.proyecto

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.tj.proyecto.Adapter.RutasActivasAdminAdapter
import com.tj.proyecto.Entidad.entAsignacionRuta
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
class RutasHoy : Fragment() {
    private lateinit var db: FirebaseFirestore

    private lateinit var txtFecha: TextView
    private lateinit var txtPendientes: TextView
    private lateinit var txtEnProgreso: TextView
    private lateinit var txtCompletadas: TextView
    private lateinit var spEstado: Spinner
    private lateinit var btnVerMapa: MaterialButton
    private lateinit var rvRutasHoy: RecyclerView
    private lateinit var llSinRutas: View

    private val rutasHoyList = mutableListOf<entAsignacionRuta>()
    private val rutasFiltradasList = mutableListOf<entAsignacionRuta>()
    private lateinit var rutasAdapter: RutasActivasAdminAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rutas_hoy, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupFecha()
        setupSpinner()
        setupRecyclerView()
        cargarRutasHoy()

        btnVerMapa.setOnClickListener {
            // TODO: Implementar vista de mapa con todas las rutas
            Toast.makeText(requireContext(), "Próximamente: Mapa con rutas del día", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun initViews(view: View) {
        txtFecha = view.findViewById(R.id.txtFecha)
        txtPendientes = view.findViewById(R.id.txtPendientes)
        txtEnProgreso = view.findViewById(R.id.txtEnProgreso)
        txtCompletadas = view.findViewById(R.id.txtCompletadas)
        spEstado = view.findViewById(R.id.spEstado)
        btnVerMapa = view.findViewById(R.id.btnVerMapa)
        rvRutasHoy = view.findViewById(R.id.rvRutasHoy)
        llSinRutas = view.findViewById(R.id.llSinRutas)
    }

    private fun setupFecha() {
        val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM yyyy", Locale("es", "ES"))
        txtFecha.text = sdf.format(Date())
    }

    private fun setupSpinner() {
        val estados = arrayOf("Todas", "Programada", "En Progreso", "Completada")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapter

        spEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarRutas(estados[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        rutasAdapter = RutasActivasAdminAdapter(rutasFiltradasList) { asignacion ->
            verDetalleRuta(asignacion)
        }
        rvRutasHoy.layoutManager = LinearLayoutManager(requireContext())
        rvRutasHoy.adapter = rutasAdapter
    }

    private fun cargarRutasHoy() {
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

        db.collection("asignaciones_rutas")
            .whereGreaterThanOrEqualTo("fechaAsignacion", inicioDia)
            .whereLessThanOrEqualTo("fechaAsignacion", finDia)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("RutasHoy", "Error al cargar rutas: ${error.message}")
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar rutas: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                rutasHoyList.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        try {
                            val asignacion = doc.toObject(entAsignacionRuta::class.java)
                            rutasHoyList.add(asignacion)
                        } catch (e: Exception) {
                            Log.e("RutasHoy", "Error al parsear asignación: ${e.message}")
                        }
                    }
                }

                actualizarContadores()
                filtrarRutas(spEstado.selectedItem.toString())
            }
    }

    private fun actualizarContadores() {
        var programadas = 0
        var enProgreso = 0
        var completadas = 0

        rutasHoyList.forEach { asignacion ->
            when (asignacion.estado) {
                "Programada" -> programadas++
                "En Progreso" -> enProgreso++
                "Completada" -> completadas++
            }
        }

        txtPendientes.text = programadas.toString()
        txtEnProgreso.text = enProgreso.toString()
        txtCompletadas.text = completadas.toString()
    }

    private fun filtrarRutas(estadoSeleccionado: String) {
        rutasFiltradasList.clear()

        when (estadoSeleccionado) {
            "Todas" -> rutasFiltradasList.addAll(rutasHoyList)
            "Programada" -> rutasFiltradasList.addAll(rutasHoyList.filter { it.estado == "Programada" })
            "En Progreso" -> rutasFiltradasList.addAll(rutasHoyList.filter { it.estado == "En Progreso" })
            "Completada" -> rutasFiltradasList.addAll(rutasHoyList.filter { it.estado == "Completada" })
        }

        // Ordenar por fecha de asignación (más reciente primero)
        rutasFiltradasList.sortByDescending { it.fechaAsignacion }

        rutasAdapter.notifyDataSetChanged()

        // Mostrar/ocultar mensaje sin rutas
        if (rutasFiltradasList.isEmpty()) {
            llSinRutas.visibility = View.VISIBLE
            rvRutasHoy.visibility = View.GONE
        } else {
            llSinRutas.visibility = View.GONE
            rvRutasHoy.visibility = View.VISIBLE
        }
    }

    private fun verDetalleRuta(asignacion: entAsignacionRuta) {
        val fragment = DetalleAsignacion.newInstance(asignacion.id)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}