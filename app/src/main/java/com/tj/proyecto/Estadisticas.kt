package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Estadisticas.newInstance] factory method to
 * create an instance of this fragment.
 */
class Estadisticas : Fragment() {
    private lateinit var spPeriodo: Spinner
    private lateinit var txtTotalRecolectado: TextView
    private lateinit var txtRutasCompletadas: TextView
    private lateinit var txtPuntosAtendidos: TextView
    private lateinit var txtTrabajadoresActivos: TextView
    private lateinit var rvTopRecolectores: RecyclerView
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
        val view = inflater.inflate(R.layout.fragment_estadisticas, container, false)

        initViews(view)
        setupSpinner()
        setupRecyclerView()
        cargarDatosEjemplo()

        return view
    }

    private fun initViews(view: View) {
        spPeriodo = view.findViewById(R.id.spPeriodo)
        txtTotalRecolectado = view.findViewById(R.id.txtTotalRecolectado)
        txtRutasCompletadas = view.findViewById(R.id.txtRutasCompletadas)
        txtPuntosAtendidos = view.findViewById(R.id.txtPuntosAtendidos)
        txtTrabajadoresActivos = view.findViewById(R.id.txtTrabajadoresActivos)
        rvTopRecolectores = view.findViewById(R.id.rvTopRecolectores)
    }

    private fun setupSpinner() {
        val periodos = arrayOf("Hoy", "Esta Semana", "Este Mes", "Este Año")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periodos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPeriodo.adapter = adapter
    }

    private fun setupRecyclerView() {
        rvTopRecolectores.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Configurar adapter cuando se conecte con Firebase
    }

    private fun cargarDatosEjemplo() {
        // Datos de ejemplo
        txtTotalRecolectado.text = "2,450 Kg"
        txtRutasCompletadas.text = "24"
        txtPuntosAtendidos.text = "186"
        txtTrabajadoresActivos.text = "12"
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Estadisticas.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Estadisticas().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}