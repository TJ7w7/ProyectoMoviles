package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Incidencias.newInstance] factory method to
 * create an instance of this fragment.
 */
class Incidencias : Fragment() {
    private lateinit var spEstado: Spinner
    private lateinit var spTipo: Spinner
    private lateinit var rvIncidencias: RecyclerView
    private lateinit var llSinDatos: View
    private lateinit var fabNuevaIncidencia: FloatingActionButton
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_incidencias, container, false)

        initViews(view)
        setupSpinners()
        setupRecyclerView()

        fabNuevaIncidencia.setOnClickListener {
            Toast.makeText(requireContext(), "Registrar nueva incidencia - Próximamente", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun initViews(view: View) {
        spEstado = view.findViewById(R.id.spEstado)
        spTipo = view.findViewById(R.id.spTipo)
        rvIncidencias = view.findViewById(R.id.rvIncidencias)
        llSinDatos = view.findViewById(R.id.llSinDatos)
        fabNuevaIncidencia = view.findViewById(R.id.fabNuevaIncidencia)
    }

    private fun setupSpinners() {
        // Estados
        val estados = arrayOf("Todas", "Pendiente", "En Proceso", "Resuelta", "Cancelada")
        val adapterEstados = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapterEstados

        // Tipos
        val tipos = arrayOf("Todos", "Punto Inaccesible", "Vehículo Averiado", "Accidente", "Clima", "Otro")
        val adapterTipos =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipo.adapter = adapterTipos
    }

    private fun setupRecyclerView() {
        rvIncidencias.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Configurar adapter cuando se conecte con Firebase

        llSinDatos.visibility = View.VISIBLE
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Incidencias.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Incidencias().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}