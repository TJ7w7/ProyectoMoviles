package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Adapter.MantenimientoAdapter
import com.tj.proyecto.Entidad.entMantenimiento
import com.tj.proyecto.Entidad.entVehiculo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"
//
///**
// * A simple [Fragment] subclass.
// * Use the [HistorialMantenimiento.newInstance] factory method to
// * create an instance of this fragment.
// */
class HistorialMantenimiento : Fragment() {
    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
//    }

    private lateinit var db: FirebaseFirestore
    private lateinit var vehiculo: entVehiculo

    private lateinit var txtPlaca: TextView
    private lateinit var txtTipoVehiculo: TextView
    private lateinit var rvMantenimientos: RecyclerView
    private lateinit var llSinDatos: View

    private val mantenimientosList = mutableListOf<entMantenimiento>()
    private lateinit var adapter: MantenimientoAdapter
    private var listenerRegistration: ListenerRegistration? = null

    companion object {
        private const val ARG_VEHICULO = "vehiculo"

        fun newInstance(vehiculo: entVehiculo): HistorialMantenimiento {
            val fragment = HistorialMantenimiento()
            val args = Bundle()
            args.putString("vehiculo_id", vehiculo.id)
            args.putString("placa", vehiculo.placa)
            args.putString("tipo_vehiculo", vehiculo.tipoVehiculo)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        arguments?.let {
            vehiculo = entVehiculo(
                id = it.getString("vehiculo_id", ""),
                placa = it.getString("placa", ""),
                tipoVehiculo = it.getString("tipo_vehiculo", "")
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_historial_mantenimiento, container, false)

        initViews(view)
        setupRecyclerView()
        cargarHistorialMantenimientos()

        return view
    }

    private fun initViews(view: View) {
        txtPlaca = view.findViewById(R.id.txtPlaca)
        txtTipoVehiculo = view.findViewById(R.id.txtTipoVehiculo)
        rvMantenimientos = view.findViewById(R.id.rvMantenimientos)
        llSinDatos = view.findViewById(R.id.llSinDatos)

        txtPlaca.text = vehiculo.placa
        txtTipoVehiculo.text = vehiculo.tipoVehiculo
    }

    private fun setupRecyclerView() {
        adapter = MantenimientoAdapter(mantenimientosList)
        rvMantenimientos.layoutManager = LinearLayoutManager(requireContext())
        rvMantenimientos.adapter = adapter
    }

    private fun cargarHistorialMantenimientos() {

        listenerRegistration = db.collection("mantenimientos")
            .whereEqualTo("vehiculoId", vehiculo.id)
            .orderBy("fechaMantenimiento", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar historial: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    mantenimientosList.clear()
                    for (document in snapshot.documents) {
                        val mantenimiento = document.toObject(entMantenimiento::class.java)
                        if (mantenimiento != null) {
                            mantenimientosList.add(mantenimiento)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    actualizarVista()
                } else {
                    mantenimientosList.clear()
                    adapter.notifyDataSetChanged()
                    actualizarVista()
                }
            }
    }

    private fun actualizarVista() {
        if (mantenimientosList.isEmpty()) {
            rvMantenimientos.visibility = View.GONE
            llSinDatos.visibility = View.VISIBLE
        } else {
            rvMantenimientos.visibility = View.VISIBLE
            llSinDatos.visibility = View.GONE
        }
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
//         * @return A new instance of fragment HistorialMantenimiento.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            HistorialMantenimiento().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
}