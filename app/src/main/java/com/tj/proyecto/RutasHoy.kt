package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class RutasHoy : Fragment() {
    private lateinit var txtFecha: TextView
    private lateinit var txtPendientes: TextView
    private lateinit var txtEnProgreso: TextView
    private lateinit var txtCompletadas: TextView
    private lateinit var spEstado: Spinner
    private lateinit var btnVerMapa: MaterialButton
    private lateinit var rvRutasHoy: RecyclerView
    private lateinit var llSinRutas: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_rutas_hoy, container, false)

        initViews(view)

        setupFecha()
        setupSpinner()
        setupRecyclerView()
        cargarDatosEjemplo()

        btnVerMapa.setOnClickListener {
            Toast.makeText(requireContext(), "Abrir mapa con rutas del día", Toast.LENGTH_SHORT).show()
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
        val estados = arrayOf("Todas", "Pendientes", "En Progreso", "Completadas")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapter
    }

    private fun setupRecyclerView() {
        rvRutasHoy.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Configurar adapter cuando se conecte con Firebase
    }

    private fun cargarDatosEjemplo() {
        // Datos de ejemplo
        txtPendientes.text = "3"
        txtEnProgreso.text = "2"
        txtCompletadas.text = "1"
    }
}